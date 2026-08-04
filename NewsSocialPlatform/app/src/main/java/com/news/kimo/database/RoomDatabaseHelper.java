package com.news.kimo.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Dao;
import androidx.room.Database;
import androidx.room.Delete;
import androidx.room.Entity;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.PrimaryKey;
import androidx.room.Query;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import java.util.List;

/**
 * Consolidates all Room entities, DAOs, and the {@link AppDatabase} definition
 * for offline caching in the social media platform.
 * <p>
 * Compatible with {@code androidx.room:room-runtime:2.6.1}.
 */
public final class RoomDatabaseHelper {

    /** Private constructor — this class is a namespace only. */
    private RoomDatabaseHelper() {
        throw new AssertionError("No instances");
    }

    // ==================================================================
    // Entity: CachedPost
    // ==================================================================

    /**
     * A lightweight, cacheable representation of a post stored in the
     * local Room database so that the feed can be shown offline.
     */
    @Entity(tableName = "cached_posts")
    public static class CachedPost {

        @PrimaryKey
        @NonNull
        public String postId;

        @NonNull
        public String uid;

        @NonNull
        public String userName;

        @NonNull
        public String userPhoto;

        @NonNull
        public String text;

        @NonNull
        public String imageUrl;

        public long timestamp;

        public long likesCount;

        public long commentsCount;

        /** Required by Room. */
        public CachedPost() {
            this.userName = "";
            this.userPhoto = "";
            this.text = "";
            this.imageUrl = "";
        }

        public CachedPost(@NonNull String postId, @NonNull String uid,
                          @NonNull String userName, @NonNull String userPhoto,
                          @NonNull String text, @NonNull String imageUrl,
                          long timestamp, long likesCount, long commentsCount) {
            this.postId = postId;
            this.uid = uid;
            this.userName = userName;
            this.userPhoto = userPhoto;
            this.text = text;
            this.imageUrl = imageUrl;
            this.timestamp = timestamp;
            this.likesCount = likesCount;
            this.commentsCount = commentsCount;
        }
    }

    // ==================================================================
    // Entity: CachedUser
    // ==================================================================

    /**
     * A minimal user record cached locally for quick profile previews
     * and offline access.
     */
    @Entity(tableName = "cached_users")
    public static class CachedUser {

        @PrimaryKey
        @NonNull
        public String uid;

        @NonNull
        public String name;

        @NonNull
        public String photoUrl;

        public boolean isVerified;

        /** Required by Room. */
        public CachedUser() {
            this.name = "";
            this.photoUrl = "";
        }

        public CachedUser(@NonNull String uid, @NonNull String name,
                          @NonNull String photoUrl, boolean isVerified) {
            this.uid = uid;
            this.name = name;
            this.photoUrl = photoUrl;
            this.isVerified = isVerified;
        }
    }

    // ==================================================================
    // Entity: SearchHistory
    // ==================================================================

    /**
     * Stores recent search queries for offline history display.
     * The auto-generated {@code id} serves as the primary key.
     */
    @Entity(tableName = "search_history")
    public static class SearchHistory {

        @PrimaryKey(autoGenerate = true)
        public long id;

        @NonNull
        public String query;

        public long timestamp;

        /** Required by Room. */
        public SearchHistory() {
            this.query = "";
        }

        public SearchHistory(@NonNull String query, long timestamp) {
            this.query = query;
            this.timestamp = timestamp;
        }
    }

    // ==================================================================
    // DAO: PostDao
    // ==================================================================

    /**
     * Data access object for {@link CachedPost} operations.
     */
    @Dao
    public interface PostDao {

        /** Insert or replace a list of cached posts. */
        @Insert(onConflict = OnConflictStrategy.REPLACE)
        void insertAll(List<CachedPost> posts);

        /** Insert or replace a single cached post. */
        @Insert(onConflict = OnConflictStrategy.REPLACE)
        void insert(CachedPost post);

        /** Delete all cached posts. */
        @Query("DELETE FROM cached_posts")
        void deleteAll();

        /** Retrieve all cached posts ordered by timestamp descending. */
        @Query("SELECT * FROM cached_posts ORDER BY timestamp DESC")
        List<CachedPost> getAll();

        /** Retrieve a single cached post by its ID. */
        @Query("SELECT * FROM cached_posts WHERE postId = :postId LIMIT 1")
        CachedPost getPostById(String postId);

        /** Delete cached posts older than the given timestamp. */
        @Query("DELETE FROM cached_posts WHERE timestamp < :threshold")
        void deleteOlderThan(long threshold);
    }

    // ==================================================================
    // DAO: UserDao
    // ==================================================================

    /**
     * Data access object for {@link CachedUser} operations.
     */
    @Dao
    public interface UserDao {

        /** Insert or replace a cached user. */
        @Insert(onConflict = OnConflictStrategy.REPLACE)
        void insert(CachedUser user);

        /** Insert or replace a list of cached users. */
        @Insert(onConflict = OnConflictStrategy.REPLACE)
        void insertAll(List<CachedUser> users);

        /** Delete a cached user. */
        @Delete
        void delete(CachedUser user);

        /** Retrieve a cached user by UID. */
        @Query("SELECT * FROM cached_users WHERE uid = :uid LIMIT 1")
        CachedUser getUser(String uid);

        /** Retrieve all cached users. */
        @Query("SELECT * FROM cached_users")
        List<CachedUser> getAllUsers();

        /** Delete all cached users. */
        @Query("DELETE FROM cached_users")
        void deleteAll();
    }

    // ==================================================================
    // DAO: SearchHistoryDao
    // ==================================================================

    /**
     * Data access object for {@link SearchHistory} operations.
     */
    @Dao
    public interface SearchHistoryDao {

        /** Insert a new search history entry. */
        @Insert(onConflict = OnConflictStrategy.REPLACE)
        void insert(SearchHistory history);

        /** Delete all search history entries. */
        @Query("DELETE FROM search_history")
        void deleteAll();

        /** Retrieve all search history entries ordered by timestamp descending. */
        @Query("SELECT * FROM search_history ORDER BY timestamp DESC")
        List<SearchHistory> getAll();

        /** Delete a specific search query by its primary key. */
        @Query("DELETE FROM search_history WHERE id = :id")
        void deleteQuery(long id);

        /** Delete search history entries older than the given timestamp. */
        @Query("DELETE FROM search_history WHERE timestamp < :threshold")
        void deleteOlderThan(long threshold);
    }

    // ==================================================================
    // Database: AppDatabase
    // ==================================================================

    /**
     * The Room database definition.  Registers all entities and exposes
     * their DAOs for use throughout the application.
     * <p>
     * Version {@code 1} — initial schema.
     */
    @Database(
            entities = {
                    RoomDatabaseHelper.CachedPost.class,
                    RoomDatabaseHelper.CachedUser.class,
                    RoomDatabaseHelper.SearchHistory.class
            },
            version = 1,
            exportSchema = false
    )
    public static abstract class AppDatabase extends RoomDatabase {

        private static volatile AppDatabase INSTANCE;

        /** Provides access to post-related cache operations. */
        public abstract PostDao postDao();

        /** Provides access to user-related cache operations. */
        public abstract UserDao userDao();

        /** Provides access to search history operations. */
        public abstract SearchHistoryDao searchHistoryDao();

        /**
         * Returns the singleton {@link AppDatabase} instance.
         * <p>
         * Uses double-checked locking for thread safety.  The database
         * is built with {@link Room#databaseBuilder} and
         * {@link RoomDatabase.Builder#allowMainThreadQueries()} is
         * deliberately <b>not</b> called — all queries must run on a
         * background thread via {@code AsyncTask}, {@code Executor},
         * or Kotlin coroutines.
         *
         * @param context Application context (will be wrapped via
         *                {@link Context#getApplicationContext()})
         * @return the singleton AppDatabase instance
         */
        @NonNull
        public static AppDatabase getInstance(@NonNull Context context) {
            if (INSTANCE == null) {
                synchronized (AppDatabase.class) {
                    if (INSTANCE == null) {
                        INSTANCE = Room.databaseBuilder(
                                        context.getApplicationContext(),
                                        AppDatabase.class,
                                        "kimo_social_db"
                                )
                                .fallbackToDestructiveMigration()
                                .build();
                    }
                }
            }
            return INSTANCE;
        }

        /**
     * Destroys the current singleton instance.
     * <p>
     * Intended for use in tests or when the database needs to be
     * fully reset at runtime.
     */
        public static void destroyInstance() {
            synchronized (AppDatabase.class) {
                if (INSTANCE != null) {
                    INSTANCE.close();
                    INSTANCE = null;
                }
            }
        }
    }
}
