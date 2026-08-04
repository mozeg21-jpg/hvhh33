package com.news.kimo.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.news.kimo.models.User;

/**
 * Singleton session manager for persisting user data, settings,
 * and FCM tokens using SharedPreferences.
 */
public class SessionManager {

    private static final String PREF_NAME = "kimo_session";
    private static final String KEY_CURRENT_USER = "current_user";
    private static final String KEY_IS_FIRST_LAUNCH = "is_first_launch";
    private static final String KEY_FCM_TOKEN = "fcm_token";
    private static final String KEY_SETTINGS = "app_settings";

    private static volatile SessionManager instance;
    private final SharedPreferences preferences;
    private final Gson gson;

    private SessionManager(Context context) {
        this.preferences = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
    }

    /**
     * Get the singleton instance of SessionManager.
     *
     * @param context Application or Activity context
     * @return The singleton instance
     */
    public static SessionManager getInstance(Context context) {
        if (instance == null) {
            synchronized (SessionManager.class) {
                if (instance == null) {
                    instance = new SessionManager(context);
                }
            }
        }
        return instance;
    }

    /**
     * Save the current user data to session.
     *
     * @param user The User object to save
     */
    public void saveCurrentUser(User user) {
        if (user != null) {
            String json = gson.toJson(user);
            preferences.edit().putString(KEY_CURRENT_USER, json).apply();
        } else {
            preferences.edit().remove(KEY_CURRENT_USER).apply();
        }
    }

    /**
     * Load the current user data from session.
     *
     * @return The User object, or null if not found
     */
    public User loadCurrentUser() {
        String json = preferences.getString(KEY_CURRENT_USER, null);
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return gson.fromJson(json, User.class);
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Save a settings string value.
     *
     * @param key   The settings key
     * @param value The settings value
     */
    public void saveSetting(String key, String value) {
        if (key != null) {
            preferences.edit().putString(key, value).apply();
        }
    }

    /**
     * Save a settings boolean value.
     *
     * @param key   The settings key
     * @param value The settings value
     */
    public void saveSetting(String key, boolean value) {
        if (key != null) {
            preferences.edit().putBoolean(key, value).apply();
        }
    }

    /**
     * Save a settings integer value.
     *
     * @param key   The settings key
     * @param value The settings value
     */
    public void saveSetting(String key, int value) {
        if (key != null) {
            preferences.edit().putInt(key, value).apply();
        }
    }

    /**
     * Load a settings string value.
     *
     * @param key          The settings key
     * @param defaultValue Default value if not found
     * @return The stored value or default
     */
    public String loadSetting(String key, String defaultValue) {
        return preferences.getString(key, defaultValue);
    }

    /**
     * Load a settings boolean value.
     *
     * @param key          The settings key
     * @param defaultValue Default value if not found
     * @return The stored value or default
     */
    public boolean loadSetting(String key, boolean defaultValue) {
        return preferences.getBoolean(key, defaultValue);
    }

    /**
     * Load a settings integer value.
     *
     * @param key          The settings key
     * @param defaultValue Default value if not found
     * @return The stored value or default
     */
    public int loadSetting(String key, int defaultValue) {
        return preferences.getInt(key, defaultValue);
    }

    /**
     * Save all settings as a JSON string.
     *
     * @param settingsJson The settings JSON string
     */
    public void saveSettings(String settingsJson) {
        if (settingsJson != null) {
            preferences.edit().putString(KEY_SETTINGS, settingsJson).apply();
        }
    }

    /**
     * Load all settings as a JSON string.
     *
     * @return The settings JSON string, or null
     */
    public String loadSettings() {
        return preferences.getString(KEY_SETTINGS, null);
    }

    /**
     * Check if this is the first time the app is launched.
     *
     * @return true if first launch
     */
    public boolean isFirstLaunch() {
        return preferences.getBoolean(KEY_IS_FIRST_LAUNCH, true);
    }

    /**
     * Mark the first launch as complete.
     */
    public void setFirstLaunchCompleted() {
        preferences.edit().putBoolean(KEY_IS_FIRST_LAUNCH, false).apply();
    }

    /**
     * Save the FCM registration token.
     *
     * @param token The FCM token string
     */
    public void saveFcmToken(String token) {
        if (token != null) {
            preferences.edit().putString(KEY_FCM_TOKEN, token).apply();
        }
    }

    /**
     * Load the saved FCM token.
     *
     * @return The FCM token, or null if not saved
     */
    public String loadFcmToken() {
        return preferences.getString(KEY_FCM_TOKEN, null);
    }

    /**
     * Clear all session data including user, settings, and FCM token.
     */
    public void clearAllSessionData() {
        preferences.edit().clear().apply();
    }
}