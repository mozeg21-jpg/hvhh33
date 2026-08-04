package com.news.kimo;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.news.kimo.database.FirestoreHelper;
import com.news.kimo.models.User;
import com.news.kimo.services.SyncService;
import com.news.kimo.utils.Constants;
import com.news.kimo.utils.SessionManager;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Application class for the Kimo social media platform.
 * Handles:
 * <ul>
 *   <li>Firebase initialisation (auto-init, plus WorkManager setup)</li>
 *   <li>Custom font (Noto Kufi Arabic) loading for the entire app</li>
 *   <li>Night-mode default from {@link SessionManager}</li>
 *   <li>Online-status tracking via {@link Application.ActivityLifecycleCallbacks}</li>
 * </ul>
 */
public class SketchApplication extends Application {

    private static final String TAG = "SketchApplication";
    private static final String SYNC_WORK_NAME = "kimo_periodic_sync";

    private static SketchApplication instance;
    private int activityCount = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        // Firebase is auto-initialised via content provider in the manifest.
        // Ensure it is ready (idempotent call).
        try {
            FirebaseApp.initializeApp(this);
        } catch (Exception e) {
            Log.w(TAG, "Firebase already initialised", e);
        }

        // Apply default night mode from session preferences.
        applyNightMode();

        // Schedule periodic WorkManager sync.
        initialiseWorkManager();

        // Track online status across all activities.
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
                // No-op
            }

            @Override
            public void onActivityStarted(@NonNull Activity activity) {
                activityCount++;
                if (activityCount == 1) {
                    // App came to foreground → set user online
                    setUserOnline(true);
                }
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {
                // No-op
            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {
                // No-op
            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {
                activityCount--;
                if (activityCount == 0) {
                    // All activities stopped → set user offline
                    setUserOnline(false);
                }
            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
                // No-op
            }

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {
                // No-op
            }
        });
    }

    /**
     * Returns the singleton {@link SketchApplication} instance.
     *
     * @return the application instance
     */
    public static SketchApplication getApplication() {
        return instance;
    }

    /**
     * Returns a global application {@link Context}.
     *
     * @return application context
     */
    public static Context getContext() {
        return instance.getApplicationContext();
    }

    // ==================================================================
    // Night Mode
    // ==================================================================

    /**
     * Reads the theme preference from {@link SessionManager} and applies
     * the corresponding night-mode configuration globally.
     */
    private void applyNightMode() {
        try {
            SessionManager sessionManager = SessionManager.getInstance(this);
            String theme = sessionManager.loadSetting(Constants.KEY_THEME, Constants.THEME_SYSTEM);

            int nightMode;
            switch (theme) {
                case Constants.THEME_LIGHT:
                    nightMode = android.app.AppCompatDelegate.MODE_NIGHT_NO;
                    break;
                case Constants.THEME_DARK:
                case Constants.THEME_AMOLED:
                    nightMode = android.app.AppCompatDelegate.MODE_NIGHT_YES;
                    break;
                case Constants.THEME_SYSTEM:
                default:
                    nightMode = android.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                    break;
            }
            android.app.AppCompatDelegate.setDefaultNightMode(nightMode);
        } catch (Exception e) {
            Log.e(TAG, "Failed to apply night mode", e);
        }
    }

    // ==================================================================
    // WorkManager
    // ==================================================================

    /**
     * Schedules the {@link SyncService} as a periodic work request
     * that runs approximately every 15 minutes when the device has
     * network connectivity.
     */
    private void initialiseWorkManager() {
        try {
            Constraints constraints = new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build();

            PeriodicWorkRequest syncRequest = new PeriodicWorkRequest.Builder(
                    SyncService.class,
                    15,
                    TimeUnit.MINUTES
            )
                    .setConstraints(constraints)
                    .setBackoffCriteria(
                            androidx.work.BackoffPolicy.EXPONENTIAL,
                            10,
                            TimeUnit.MINUTES
                    )
                    .build();

            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                    SYNC_WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    syncRequest
            );

            Log.d(TAG, "WorkManager sync scheduled");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialise WorkManager", e);
        }
    }

    // ==================================================================
    // Online Status
    // ==================================================================

    /**
     * Updates the current user's online / offline status and lastSeen
     * timestamp in Firebase Realtime Database.
     *
     * @param online {@code true} to mark online, {@code false} to mark offline
     */
    private void setUserOnline(boolean online) {
        try {
            if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                return;
            }
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            long now = System.currentTimeMillis();

            Map<String, Object> updates = new HashMap<>();
            updates.put("isOnline", online);
            updates.put("lastSeen", now);

            DatabaseReference userRef = FirestoreHelper.getInstance().getUserRef(uid);
            userRef.updateChildren(updates)
                    .addOnSuccessListener(aVoid ->
                            Log.d(TAG, "Online status updated: " + (online ? "online" : "offline")))
                    .addOnFailureListener(e ->
                            Log.e(TAG, "Failed to update online status", e));
        } catch (Exception e) {
            Log.e(TAG, "setUserOnline failed", e);
        }
    }
}
