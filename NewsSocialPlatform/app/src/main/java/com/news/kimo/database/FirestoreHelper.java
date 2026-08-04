package com.news.kimo.database;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Transaction;
import com.news.kimo.utils.Constants;

/**
 * Singleton helper class for accessing Firebase Realtime Database references.
 * <p>
 * Despite the class name (kept for backward compatibility), this class
 * exclusively uses Firebase <b>Realtime Database</b> — not Firestore.
 * <p>
 * Every method returns a {@link DatabaseReference} so callers can
 * attach listeners, perform writes, or run transactions directly.
 */
public final class FirestoreHelper {

    private static final String TAG = "FirestoreHelper";

    /** Volatile singleton reference for thread-safe double-checked locking. */
    private static volatile FirestoreHelper instance;

    /** The single FirebaseDatabase instance used throughout the app. */
    private final FirebaseDatabase firebaseDatabase;

    // ------------------------------------------------------------------
    // Singleton
    // ------------------------------------------------------------------

    /**
     * Private constructor — obtains the default FirebaseDatabase instance
     * and enables disk persistence for offline support.
     */
    private FirestoreHelper() {
        this.firebaseDatabase = FirebaseDatabase.getInstance();
        this.firebaseDatabase.setPersistenceEnabled(true);
    }

    /**
     * Returns the singleton instance of {@code FirestoreHelper}.
     * Uses double-checked locking for thread safety.
     *
     * @return the singleton instance
     */
    public static FirestoreHelper getInstance() {
        if (instance == null) {
            synchronized (FirestoreHelper.class) {
                if (instance == null) {
                    instance = new FirestoreHelper();
                }
            }
        }
        return instance;
    }

    // ------------------------------------------------------------------
    // Core accessors
    // ------------------------------------------------------------------

    /**
     * Returns the underlying {@link FirebaseDatabase} instance.
     *
     * @return the FirebaseDatabase instance
     */
    @NonNull
    public FirebaseDatabase getDatabase() {
        return firebaseDatabase;
    }

    /**
     * Returns a DatabaseReference at the given arbitrary path.
     * <p>
     * Example: {@code getReference("users/abc123/posts")}
     *
     * @param path the relative database path
     * @return a DatabaseReference pointing to the requested path
     */
    @NonNull
    public DatabaseReference getReference(@NonNull String path) {
        return firebaseDatabase.getReference(path);
    }

    // ------------------------------------------------------------------
    // Node-level convenience methods
    // ------------------------------------------------------------------

    /** Users root. */
    @NonNull
    public DatabaseReference getUserRef(@NonNull String uid) {
        return firebaseDatabase.getReference()
                .child(Constants.USERS)
                .child(uid);
    }

    /** Single post. */
    @NonNull
    public DatabaseReference getPostRef(@NonNull String postId) {
        return firebaseDatabase.getReference()
                .child(Constants.POSTS)
                .child(postId);
    }

    /** All comments for a post. */
    @NonNull
    public DatabaseReference getCommentsRef(@NonNull String postId) {
        return firebaseDatabase.getReference()
                .child(Constants.COMMENTS)
                .child(postId);
    }

    /** All likes for a post. */
    @NonNull
    public DatabaseReference getLikesRef(@NonNull String postId) {
        return firebaseDatabase.getReference()
                .child(Constants.LIKES)
                .child(postId);
    }

    /** Followers collection for a user. */
    @NonNull
    public DatabaseReference getFollowersRef(@NonNull String uid) {
        return firebaseDatabase.getReference()
                .child(Constants.FOLLOWERS)
                .child(uid);
    }

    /** Following collection for a user. */
    @NonNull
    public DatabaseReference getFollowingRef(@NonNull String uid) {
        return firebaseDatabase.getReference()
                .child(Constants.FOLLOWING)
                .child(uid);
    }

    /** Notifications for a user. */
    @NonNull
    public DatabaseReference getNotificationsRef(@NonNull String uid) {
        return firebaseDatabase.getReference()
                .child(Constants.NOTIFICATIONS)
                .child(uid);
    }

    /** Messages inside a chat. */
    @NonNull
    public DatabaseReference getMessagesRef(@NonNull String chatId) {
        return firebaseDatabase.getReference()
                .child(Constants.MESSAGES)
                .child(chatId);
    }

    /** Chats list for a user. */
    @NonNull
    public DatabaseReference getChatsRef(@NonNull String uid) {
        return firebaseDatabase.getReference()
                .child(Constants.CHATS)
                .child(uid);
    }

    /** Global reports root. */
    @NonNull
    public DatabaseReference getReportsRef() {
        return firebaseDatabase.getReference()
                .child(Constants.REPORTS);
    }

    /** Saved / bookmarked posts for a user. */
    @NonNull
    public DatabaseReference getSavedPostsRef(@NonNull String uid) {
        return firebaseDatabase.getReference()
                .child(Constants.SAVED_POSTS)
                .child(uid);
    }

    /** Global hashtags index. */
    @NonNull
    public DatabaseReference getHashtagsRef() {
        return firebaseDatabase.getReference()
                .child(Constants.HASHTAGS);
    }

    /** Trending topics. */
    @NonNull
    public DatabaseReference getTrendingRef() {
        return firebaseDatabase.getReference()
                .child(Constants.TRENDING);
    }

    /** User-specific settings. */
    @NonNull
    public DatabaseReference getSettingsRef(@NonNull String uid) {
        return firebaseDatabase.getReference()
                .child(Constants.SETTINGS)
                .child(uid);
    }

    /** Global media storage metadata root. */
    @NonNull
    public DatabaseReference getMediaRef() {
        return firebaseDatabase.getReference()
                .child(Constants.MEDIA);
    }

    /** Analytics data root. */
    @NonNull
    public DatabaseReference getAnalyticsRef() {
        return firebaseDatabase.getReference()
                .child(Constants.ANALYTICS);
    }

    /** Admin activity logs. */
    @NonNull
    public DatabaseReference getAdminLogsRef() {
        return firebaseDatabase.getReference()
                .child(Constants.ADMIN_LOGS);
    }

    /** Account verification requests. */
    @NonNull
    public DatabaseReference getVerificationRef() {
        return firebaseDatabase.getReference()
                .child(Constants.VERIFICATION);
    }

    /** Blocked users list for a user. */
    @NonNull
    public DatabaseReference getBlocksRef(@NonNull String uid) {
        return firebaseDatabase.getReference()
                .child(Constants.BLOCKS)
                .child(uid);
    }

    /** Muted users list for a user. */
    @NonNull
    public DatabaseReference getMutesRef(@NonNull String uid) {
        return firebaseDatabase.getReference()
                .child(Constants.MUTES)
                .child(uid);
    }

    /** Active sessions for a user. */
    @NonNull
    public DatabaseReference getSessionsRef(@NonNull String uid) {
        return firebaseDatabase.getReference()
                .child(Constants.SESSIONS)
                .child(uid);
    }

    /** Registered devices for a user. */
    @NonNull
    public DatabaseReference getDevicesRef(@NonNull String uid) {
        return firebaseDatabase.getReference()
                .child(Constants.DEVICES)
                .child(uid);
    }

    /** Global app configuration. */
    @NonNull
    public DatabaseReference getAppConfigRef() {
        return firebaseDatabase.getReference()
                .child(Constants.APP_CONFIG);
    }

    // ------------------------------------------------------------------
    // Transactions
    // ------------------------------------------------------------------

    /**
     * Run a Firebase Realtime Database transaction on the root reference.
     * <p>
     * Use this for atomic multi-step operations such as incrementing
     * counters, transferring data, or performing conditional writes.
     *
     * @param handler the transaction handler implementing the logic
     */
    public void runTransaction(@NonNull Transaction.Handler handler) {
        firebaseDatabase.getReference().runTransaction(handler, false, true);
    }
}
