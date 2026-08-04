package com.news.kimo.services;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.news.kimo.database.FirestoreHelper;
import com.news.kimo.models.User;
import com.news.kimo.utils.Constants;
import com.news.kimo.utils.NotificationHelper;
import com.news.kimo.utils.SessionManager;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Handles Firebase Cloud Messaging token refresh and incoming
 * data messages for the Kimo social media platform.
 * <p>
 * On receiving a data payload the service parses the {@code type}
 * field and shows the appropriate notification channel.
 */
public class FCMService extends FirebaseMessagingService {

    private static final String TAG = "FCMService";

    /** Monotonically increasing notification ID generator. */
    private static final AtomicInteger notificationIdCounter = new AtomicInteger(0);

    // ------------------------------------------------------------------
    // Keys expected in the data payload
    // ------------------------------------------------------------------

    private static final String KEY_TYPE = "type";
    private static final String KEY_TITLE = "title";
    private static final String KEY_BODY = "body";
    private static final String KEY_POST_ID = "postId";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_CHAT_ID = "chatId";
    private static final String KEY_SENDER_NAME = "senderName";
    private static final String KEY_SENDER_PHOTO = "senderPhoto";

    // ------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannels();
    }

    /**
     * Called when a new FCM registration token is issued.
     * <p>
     * The token is persisted locally via {@link SessionManager} and
     * pushed to the user's Realtime Database node so that the
     * cloud-function / server can target this device.
     */
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "New FCM token received");

        // Persist locally
        SessionManager sessionManager = SessionManager.getInstance(this);
        sessionManager.saveFcmToken(token);

        // Push to Realtime Database
        User currentUser = sessionManager.loadCurrentUser();
        if (currentUser != null && currentUser.getUid() != null) {
            DatabaseReference userRef = FirestoreHelper.getInstance().getUserRef(currentUser.getUid());
            userRef.child("fcmToken").setValue(token)
                    .addOnSuccessListener(unused ->
                            Log.d(TAG, "FCM token updated in DB for uid=" + currentUser.getUid()))
                    .addOnFailureListener(e ->
                            Log.e(TAG, "Failed to update FCM token", e));
        } else {
            Log.w(TAG, "No current user — skipping remote FCM token update");
        }
    }

    /**
     * Called when a data-only message is received from FCM.
     * <p>
     * The payload must contain at minimum {@code type}, {@code title},
     * and {@code body}.  The {@code type} determines which notification
     * channel is used and how the tap intent is routed.
     */
    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);

        if (message.getData().isEmpty()) {
            Log.d(TAG, "Received empty data payload — ignoring");
            return;
        }

        Map<String, String> data = message.getData();
        String type = data.getOrDefault(KEY_TYPE, Constants.NOTIFICATION_SYSTEM);
        String title = data.getOrDefault(KEY_TITLE, "Kimo Social");
        String body = data.getOrDefault(KEY_BODY, "");

        Log.d(TAG, "Notification received — type: " + type + ", title: " + title);

        // Route to the correct channel and intent
        switch (type) {
            case Constants.NOTIFICATION_LIKE:
            case Constants.NOTIFICATION_REACTION:
                showNotification(Constants.CHANNEL_GENERAL, type, title, body, data);
                break;

            case Constants.NOTIFICATION_COMMENT:
            case Constants.NOTIFICATION_REPLY:
                showNotification(Constants.CHANNEL_GENERAL, type, title, body, data);
                break;

            case Constants.NOTIFICATION_FOLLOW:
                showNotification(Constants.CHANNEL_GENERAL, type, title, body, data);
                break;

            case Constants.NOTIFICATION_MESSAGE:
                showNotification(Constants.CHANNEL_MESSAGES, type, title, body, data);
                break;

            case Constants.NOTIFICATION_MENTION:
                showNotification(Constants.CHANNEL_GENERAL, type, title, body, data);
                break;

            case Constants.NOTIFICATION_GROUP_INVITE:
                showNotification(Constants.CHANNEL_GROUPS, type, title, body, data);
                break;

            case Constants.NOTIFICATION_SYSTEM:
            case Constants.NOTIFICATION_VERIFICATION:
            case Constants.NOTIFICATION_REPORT:
            default:
                showNotification(Constants.CHANNEL_GENERAL, type, title, body, data);
                break;
        }
    }

    // ------------------------------------------------------------------
    // Notification helpers
    // ------------------------------------------------------------------

    /**
     * Creates all required notification channels on API 26+.
     * Called once in {@link #onCreate()}.
     */
    private void createNotificationChannels() {
        NotificationHelper notificationHelper = new NotificationHelper(this);

        // General notifications (likes, comments, follows, mentions, system)
        notificationHelper.createNotificationChannel(
                Constants.CHANNEL_GENERAL,
                "General Notifications",
                android.app.NotificationManager.IMPORTANCE_DEFAULT
        );

        // Direct / group messages
        notificationHelper.createNotificationChannel(
                Constants.CHANNEL_MESSAGES,
                "Messages",
                android.app.NotificationManager.IMPORTANCE_HIGH
        );

        // Group chat activity
        notificationHelper.createNotificationChannel(
                Constants.CHANNEL_GROUPS,
                "Groups",
                android.app.NotificationManager.IMPORTANCE_DEFAULT
        );
    }

    /**
     * Builds a tap {@link Intent} based on the notification type and
     * the identifiers present in the data payload.
     *
     * @param type the notification type constant
     * @param data the full data payload map
     * @return an Intent suitable for a PendingIntent, or {@code null}
     */
    private Intent buildTapIntent(String type, Map<String, String> data) {
        /*
         * In a full implementation these would point to real Activity
         * classes.  Here we use a generic deep-link approach via
         * package-level action constants so the manifest can route
         * them correctly.
         */
        Intent intent = new Intent(this, getApplication().getClass());
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        String postId = data.get(KEY_POST_ID);
        String userId = data.get(KEY_USER_ID);
        String chatId = data.get(KEY_CHAT_ID);

        switch (type) {
            case Constants.NOTIFICATION_LIKE:
            case Constants.NOTIFICATION_REACTION:
            case Constants.NOTIFICATION_COMMENT:
            case Constants.NOTIFICATION_REPLY:
            case Constants.NOTIFICATION_MENTION:
                if (postId != null) {
                    intent.putExtra(Constants.EXTRA_POST_ID, postId);
                }
                break;

            case Constants.NOTIFICATION_FOLLOW:
                if (userId != null) {
                    intent.putExtra(Constants.EXTRA_USER_ID, userId);
                }
                break;

            case Constants.NOTIFICATION_MESSAGE:
                if (chatId != null) {
                    intent.putExtra("chatId", chatId);
                }
                if (userId != null) {
                    intent.putExtra(Constants.EXTRA_USER_ID, userId);
                }
                break;

            case Constants.NOTIFICATION_GROUP_INVITE:
                if (chatId != null) {
                    intent.putExtra("chatId", chatId);
                }
                break;

            default:
                // System / verification / report — no specific target
                break;
        }

        return intent;
    }

    /**
     * Displays a notification via {@link NotificationHelper}.
     *
     * @param channelId the notification channel ID
     * @param type      the notification type (used for intent routing)
     * @param title     the notification title
     * @param body      the notification body text
     * @param data      the full data payload
     */
    private void showNotification(String channelId,
                                  String type,
                                  String title,
                                  String body,
                                  Map<String, String> data) {
        int id = notificationIdCounter.incrementAndGet();

        Intent tapIntent = buildTapIntent(type, data);

        NotificationHelper notificationHelper = new NotificationHelper(this);
        notificationHelper.showNotification(
                channelId,
                id,
                title,
                body,
                null,  // largeIcon — can be fetched from senderPhoto if needed
                tapIntent
        );
    }

    /**
     * Generates a unique notification ID using an atomic counter,
     * avoiding collisions across rapid successive notifications.
     *
     * @return a unique integer suitable for {@link NotificationCompat.Builder}
     */
    private static int getNextNotificationId() {
        return notificationIdCounter.incrementAndGet();
    }
}
