package com.news.kimo.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.news.kimo.models.Post;
import com.news.kimo.models.User;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Utility class for caching data using SharedPreferences and Gson.
 * Supports caching posts, users, and search history.
 */
public class CacheHelper {

    private static final String PREF_NAME = "kimo_cache";
    private static final int MAX_SEARCH_HISTORY = 20;
    private static final String KEY_POSTS_CACHE = "posts_cache";
    private static final String KEY_USERS_CACHE = "users_cache";
    private static final String KEY_SEARCH_HISTORY = "search_history";

    private final SharedPreferences preferences;
    private final Gson gson;

    /**
     * Creates a new CacheHelper instance.
     *
     * @param context Application or Activity context
     */
    public CacheHelper(Context context) {
        this.preferences = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
    }

    /**
     * Save a list of posts to cache as JSON.
     *
     * @param posts The list of posts to cache
     */
    public void savePosts(List<Post> posts) {
        if (posts == null) {
            return;
        }
        String json = gson.toJson(posts);
        preferences.edit().putString(KEY_POSTS_CACHE, json).apply();
    }

    /**
     * Load a list of cached posts.
     *
     * @return List of cached posts, or an empty list if none exist
     */
    public List<Post> loadPosts() {
        String json = preferences.getString(KEY_POSTS_CACHE, null);
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            Type type = new TypeToken<ArrayList<Post>>() {}.getType();
            List<Post> posts = gson.fromJson(json, type);
            return posts != null ? posts : new ArrayList<Post>();
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Save a list of users to cache as JSON.
     *
     * @param users The list of users to cache
     */
    public void saveUsers(List<User> users) {
        if (users == null) {
            return;
        }
        String json = gson.toJson(users);
        preferences.edit().putString(KEY_USERS_CACHE, json).apply();
    }

    /**
     * Load a list of cached users.
     *
     * @return List of cached users, or an empty list if none exist
     */
    public List<User> loadUsers() {
        String json = preferences.getString(KEY_USERS_CACHE, null);
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            Type type = new TypeToken<ArrayList<User>>() {}.getType();
            List<User> users = gson.fromJson(json, type);
            return users != null ? users : new ArrayList<User>();
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Clear all cached data.
     */
    public void clearCache() {
        preferences.edit().clear().apply();
    }

    /**
     * Get the approximate cache size in bytes.
     *
     * @return Cache size in bytes
     */
    public long getCacheSize() {
        long totalSize = 0;
        for (String key : preferences.getAll().keySet()) {
            String value = preferences.getString(key, null);
            if (value != null) {
                totalSize += value.length() * 2L; // Each char is 2 bytes in Java
            }
        }
        return totalSize;
    }

    /**
     * Check if network is available. Duplicate of NetworkHelper.isNetworkAvailable()
     * for backward compatibility.
     *
     * @param context Context
     * @return true if network is available
     */
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager)
                context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            return false;
        }
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }

    /**
     * Save a search query to search history.
     * Duplicates are moved to the top. History is capped at MAX_SEARCH_HISTORY.
     *
     * @param query The search query to save
     */
    public void saveSearchHistory(String query) {
        if (query == null || query.trim().isEmpty()) {
            return;
        }
        query = query.trim();

        List<String> history = loadSearchHistory();
        // Remove existing entry to move it to top
        history.remove(query);
        // Add to beginning
        history.add(0, query);
        // Trim to max size
        while (history.size() > MAX_SEARCH_HISTORY) {
            history.remove(history.size() - 1);
        }

        String json = gson.toJson(history);
        preferences.edit().putString(KEY_SEARCH_HISTORY, json).apply();
    }

    /**
     * Load search history as a list of strings.
     *
     * @return List of search history entries, newest first
     */
    public List<String> loadSearchHistory() {
        String json = preferences.getString(KEY_SEARCH_HISTORY, null);
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            Type type = new TypeToken<ArrayList<String>>() {}.getType();
            List<String> history = gson.fromJson(json, type);
            return history != null ? history : new ArrayList<String>();
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Clear all search history.
     */
    public void clearSearchHistory() {
        preferences.edit().remove(KEY_SEARCH_HISTORY).apply();
    }
}