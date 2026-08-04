package com.news.kimo.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

/**
 * Utility class for creating and showing notifications.
 * Supports notification channels (API 26+), large icons,
 * big picture style, and grouped notifications.
 */
public class NotificationHelper {

    private static final String TAG = "NotificationHelper";

    // Default channel IDs
    public static final String CHANNEL_ID_GENERAL = Constants.CHANNEL_GENERAL;
    public static final String CHANNEL_ID_MESSAGES = Constants.CHANNEL_MESSAGES;
    public static final String CHANNEL_ID_GROUPS = Constants.CHANNEL_GROUPS;

    private final Context context;
    private final NotificationManager notificationManager;

    /**
     * Creates a new NotificationHelper instance.
     *
     * @param context Application or Activity context
     */
    public NotificationHelper(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    /**
     * Create a notification channel (required for API 26+).
     * This is a no-op on older API versions.
     *
     * @param channelId   The channel ID
     * @param channelName The user-visible channel name
     * @param importance  One of NotificationManager.IMPORTANCE_* constants
     */
    public void createNotificationChannel(String channelId, String channelName, int importance) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    channelName,
                    importance
            );
            channel.setDescription(channelName);
            channel.enableVibration(true);
            notificationManager.createNotificationChannel(channel);
        }
    }

    /**
     * Show a notification with a large icon.
     *
     * @param channelId   The notification channel ID
     * @param id          The notification ID
     * @param title       The notification title
     * @param message     The notification message
     * @param largeIcon   The large icon bitmap (e.g. sender's avatar)
     * @param intent      The intent to open when notification is tapped
     */
    public void showNotification(@NonNull String channelId,
                                 int id,
                                 @NonNull String title,
                                 @NonNull String message,
                                 Bitmap largeIcon,
                                 Intent intent) {
        NotificationCompat.Builder builder = createBaseBuilder(channelId)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setAutoCancel(true);

        if (largeIcon != null) {
            builder.setLargeIcon(largeIcon);
        }

        if (intent != null) {
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context, id, intent, flags);
            builder.setContentIntent(pendingIntent);
        }

        notificationManager.notify(id, builder.build());
    }

    /**
     * Show a notification with a big picture style.
     *
     * @param channelId   The notification channel ID
     * @param id          The notification ID
     * @param title       The notification title
     * @param message     The notification message
     * @param largeIcon   The large icon bitmap
     * @param bigPicture  The big picture bitmap to display in the expanded view
     * @param intent      The intent to open when notification is tapped
     */
    public void showNotificationWithImage(@NonNull String channelId,
                                          int id,
                                          @NonNull String title,
                                          @NonNull String message,
                                          Bitmap largeIcon,
                                          Bitmap bigPicture,
                                          Intent intent) {
        NotificationCompat.Builder builder = createBaseBuilder(channelId)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true);

        if (largeIcon != null) {
            builder.setLargeIcon(largeIcon);
        }

        if (bigPicture != null) {
            builder.setStyle(new NotificationCompat.BigPictureStyle()
                    .bigPicture(bigPicture)
                    .bigLargeIcon(null)); // Hide large icon when expanded
        }

        if (intent != null) {
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context, id, intent, flags);
            builder.setContentIntent(pendingIntent);
        }

        notificationManager.notify(id, builder.build());
    }

    /**
     * Show a group summary notification (used for grouping multiple notifications).
     *
     * @param groupId   The group ID for notifications to be grouped under
     * @param id        The summary notification ID
     * @param title     The summary title (e.g. "3 new messages")
     * @param message   The summary message
     */
    public void showGroupSummaryNotification(@NonNull String groupId,
                                             int id,
                                             @NonNull String title,
                                             @NonNull String message) {
        NotificationCompat.Builder builder = createBaseBuilder(CHANNEL_ID_GENERAL)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.InboxStyle()
                        .setBigContentTitle(title)
                        .setSummaryText(message))
                .setGroup(groupId)
                .setGroupSummary(true)
                .setAutoCancel(true);

        notificationManager.notify(id, builder.build());
    }

    /**
     * Clear a specific notification by ID.
     *
     * @param id The notification ID to cancel
     */
    public void clearNotification(int id) {
        notificationManager.cancel(id);
    }

    /**
     * Clear all notifications for this app.
     */
    public void clearAllNotifications() {
        notificationManager.cancelAll();
    }

    /**
     * Create a base NotificationCompat.Builder with common settings.
     *
     * @param channelId The channel ID
     * @return A configured NotificationCompat.Builder
     */
    private NotificationCompat.Builder createBaseBuilder(String channelId) {
        return new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info) // Use app icon in production
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setDefaults(NotificationCompat.DEFAULT_ALL);
    }
}