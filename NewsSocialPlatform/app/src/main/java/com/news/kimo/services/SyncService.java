package com.news.kimo.services;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.database.DatabaseReference;
import com.news.kimo.database.FirestoreHelper;
import com.news.kimo.database.RoomDatabaseHelper;
import com.news.kimo.models.AdminLog;
import com.news.kimo.models.Post;
import com.news.kimo.models.User;
import com.news.kimo.utils.Constants;
import com.news.kimo.utils.SessionManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A {@link Worker} that periodically synchronises local cached data
 * with the Firebase Realtime Database.
 * <p>
 * Responsibilities:
 * <ol>
 *   <li>Publish scheduled posts whose scheduled time has arrived.</li>
 *   <li>Remove stale cache entries older than 7 days.</li>
 *   <li>Update the current user's online / last-seen status.</li>
 *   <li>Log every sync run to the {@code admin_logs} node.</li>
 * </ol>
 * <p>
 * Schedule this worker with {@code PeriodicWorkRequest} (minimum
 * interval 15 minutes) or {@code OneTimeWorkRequest} from your
 * Application class or a dedicated scheduler.
 */
public class SyncService extends Worker {

    private static final String TAG = "SyncService";

    /** Cache entries older than this many milliseconds are purged (7 days). */
    private static final long CACHE_TTL_MS = 7L * 24 * 60 * 60 * 1000;

    public SyncService(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    // ------------------------------------------------------------------
    // Core doWork
    // ------------------------------------------------------------------

    @NonNull
    @Override
    public Result doWork() {
        Log.i(TAG, "Sync started");

        try {
            // 1. Publish due scheduled posts
            syncScheduledPosts();

            // 2. Clean stale cache
            cleanupOldCache();

            // 3. Update online status
            updateOnlineStatus();

            // 4. Log the sync
            logSyncOperation("sync_completed", "Scheduled posts published, cache cleaned, status updated");

            Log.i(TAG, "Sync completed successfully");
            return Result.success();

        } catch (Exception e) {
            Log.e(TAG, "Sync failed", e);
            logSyncOperation("sync_failed", e.getMessage() != null ? e.getMessage() : "Unknown error");
            return Result.retry();
        }
    }

    // ------------------------------------------------------------------
    // 1. Scheduled post publishing
    // ------------------------------------------------------------------

    /**
     * Checks the Realtime Database for posts where {@code isScheduled}
     * is {@code true} and {@code scheduledAt} <= now, then flips them
     * to published status.
     * <p>
     * In a production app this would typically query a local cache of
     * the current user's own scheduled posts rather than scanning the
     * entire posts node.
     */
    private void syncScheduledPosts() {
        User currentUser = SessionManager.getInstance(getApplicationContext()).loadCurrentUser();
        if (currentUser == null || currentUser.getUid() == null) {
            Log.w(TAG, "No authenticated user — skipping scheduled post sync");
            return;
        }

        String uid = currentUser.getUid();
        long now = System.currentTimeMillis();
        DatabaseReference postsRef = FirestoreHelper.getInstance().getReference(Constants.POSTS);

        // Query for scheduled posts belonging to this user whose time has come.
        // Realtime DB queries are limited — we order by scheduledAt and
        // limit the range to avoid scanning the entire collection.
        postsRef.orderByChild("scheduledAt")
                .endAt(now)
                .get()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful() || task.getResult() == null) {
                        Log.w(TAG, "Failed to query scheduled posts");
                        return;
                    }

                    int publishedCount = 0;

                    for (var snapshot : task.getResult().getChildren()) {
                        Boolean isScheduled = snapshot.child("isScheduled").getValue(Boolean.class);
                        String postUid = snapshot.child("uid").getValue(String.class);

                        if (Boolean.TRUE.equals(isScheduled) && uid.equals(postUid)) {
                            String postId = snapshot.getKey();

                            Map<String, Object> updates = new HashMap<>();
                            updates.put("isScheduled", false);
                            updates.put("timestamp", now);
                            // Clear the scheduledAt marker to avoid re-processing
                            updates.put("scheduledAt", 0L);

                            postsRef.child(postId).updateChildren(updates)
                                    .addOnSuccessListener(unused -> {
                                        Log.d(TAG, "Scheduled post published: " + postId);
                                        removeFromLocalCache(postId);
                                    })
                                    .addOnFailureListener(e ->
                                            Log.e(TAG, "Failed to publish scheduled post: " + postId, e));

                            publishedCount++;
                        }
                    }

                    Log.d(TAG, "Scheduled post sync: " + publishedCount + " posts published");
                });
    }

    /**
     * Removes a locally cached post after it has been published.
     *
     * @param postId the post ID to remove from cache
     */
    private void removeFromLocalCache(String postId) {
        try {
            RoomDatabaseHelper.AppDatabase db =
                    RoomDatabaseHelper.AppDatabase.getInstance(getApplicationContext());
            RoomDatabaseHelper.CachedPost cached = db.postDao().getPostById(postId);
            if (cached != null) {
                db.postDao().deleteAll(); // Remove and will be refreshed on next fetch
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to remove post from local cache", e);
        }
    }

    // ------------------------------------------------------------------
    // 2. Cache cleanup
    // ------------------------------------------------------------------

    /**
     * Deletes cached posts and search history entries that are older
     * than {@link #CACHE_TTL_MS} (7 days).
     */
    private void cleanupOldCache() {
        try {
            RoomDatabaseHelper.AppDatabase db =
                    RoomDatabaseHelper.AppDatabase.getInstance(getApplicationContext());

            long threshold = System.currentTimeMillis() - CACHE_TTL_MS;

            // Purge old posts
            db.postDao().deleteOlderThan(threshold);
            Log.d(TAG, "Old cached posts cleaned up (threshold: " + threshold + ")");

            // Purge old search history
            db.searchHistoryDao().deleteOlderThan(threshold);
            Log.d(TAG, "Old search history cleaned up");

        } catch (Exception e) {
            Log.e(TAG, "Cache cleanup failed", e);
        }
    }

    // ------------------------------------------------------------------
    // 3. Online status
    // ------------------------------------------------------------------

    /**
     * Updates the current user's {@code isOnline} flag and
     * {@code lastSeen} timestamp in the Realtime Database.
     */
    private void updateOnlineStatus() {
        User currentUser = SessionManager.getInstance(getApplicationContext()).loadCurrentUser();
        if (currentUser == null || currentUser.getUid() == null) {
            Log.w(TAG, "No authenticated user — skipping online status update");
            return;
        }

        long now = System.currentTimeMillis();
        Map<String, Object> statusUpdate = new HashMap<>();
        statusUpdate.put("isOnline", true);
        statusUpdate.put("lastSeen", now);

        DatabaseReference userRef = FirestoreHelper.getInstance().getUserRef(currentUser.getUid());
        userRef.updateChildren(statusUpdate)
                .addOnSuccessListener(unused ->
                        Log.d(TAG, "Online status updated for uid=" + currentUser.getUid()))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Failed to update online status", e));
    }

    // ------------------------------------------------------------------
    // 4. Admin log
    // ------------------------------------------------------------------

    /**
     * Writes a log entry to the {@code admin_logs} node so that
     * administrators can audit background sync operations.
     *
     * @param action  short action identifier (e.g. {@code sync_completed})
     * @param details human-readable description of the result
     */
    private void logSyncOperation(String action, String details) {
        try {
            User currentUser = SessionManager.getInstance(getApplicationContext()).loadCurrentUser();

            String adminUid = (currentUser != null) ? currentUser.getUid() : "system";
            String adminName = (currentUser != null) ? currentUser.getName() : "System";

            AdminLog log = new AdminLog(
                    UUID.randomUUID().toString(),
                    adminUid,
                    adminName,
                    action,
                    "sync_worker",
                    "periodic_sync",
                    details,
                    System.currentTimeMillis()
            );

            DatabaseReference logRef = FirestoreHelper.getInstance().getAdminLogsRef();
            logRef.child(log.getLogId()).setValue(log)
                    .addOnFailureListener(e ->
                            Log.e(TAG, "Failed to write admin log", e));

        } catch (Exception e) {
            Log.e(TAG, "Failed to log sync operation", e);
        }
    }
}