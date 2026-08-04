package com.news.kimo.utils;

/**
 * App-wide constants for the social media platform.
 * Contains Firebase DB paths, reaction types, notification types,
 * user roles, post types, report types, privacy levels, theme constants,
 * max values, request codes, and app version.
 */
public final class Constants {

    private Constants() {
        // Prevent instantiation
    }

    // ============================================================
    // Firebase Database Paths
    // ============================================================
    public static final String USERS = "users";
    public static final String POSTS = "posts";
    public static final String COMMENTS = "comments";
    public static final String REPLIES = "replies";
    public static final String LIKES = "likes";
    public static final String REACTIONS = "reactions";
    public static final String FOLLOWERS = "followers";
    public static final String FOLLOWING = "following";
    public static final String NOTIFICATIONS = "notifications";
    public static final String MESSAGES = "messages";
    public static final String CHATS = "chats";
    public static final String GROUPS = "groups";
    public static final String REPORTS = "reports";
    public static final String SAVED_POSTS = "saved_posts";
    public static final String HASHTAGS = "hashtags";
    public static final String TRENDING = "trending";
    public static final String SETTINGS = "settings";
    public static final String MEDIA = "media";
    public static final String ANALYTICS = "analytics";
    public static final String ADMIN_LOGS = "admin_logs";
    public static final String VERIFICATION = "verification";
    public static final String BLOCKS = "blocks";
    public static final String MUTES = "mutes";
    public static final String SESSIONS = "sessions";
    public static final String DEVICES = "devices";
    public static final String APP_CONFIG = "app_config";

    // ============================================================
    // Reaction Types
    // ============================================================
    public static final String REACTION_LIKE = "like";
    public static final String REACTION_LOVE = "love";
    public static final String REACTION_HAHA = "haha";
    public static final String REACTION_WOW = "wow";
    public static final String REACTION_SAD = "sad";
    public static final String REACTION_ANGRY = "angry";

    // ============================================================
    // Notification Types
    // ============================================================
    public static final String NOTIFICATION_LIKE = "like";
    public static final String NOTIFICATION_COMMENT = "comment";
    public static final String NOTIFICATION_REPLY = "reply";
    public static final String NOTIFICATION_FOLLOW = "follow";
    public static final String NOTIFICATION_MENTION = "mention";
    public static final String NOTIFICATION_REPOST = "repost";
    public static final String NOTIFICATION_MESSAGE = "message";
    public static final String NOTIFICATION_GROUP_INVITE = "group_invite";
    public static final String NOTIFICATION_VERIFICATION = "verification";
    public static final String NOTIFICATION_SYSTEM = "system";
    public static final String NOTIFICATION_REPORT = "report";
    public static final String NOTIFICATION_REACTION = "reaction";

    // ============================================================
    // User Roles
    // ============================================================
    public static final String ROLE_USER = "user";
    public static final String ROLE_MODERATOR = "moderator";
    public static final String ROLE_ADMIN = "admin";
    public static final String ROLE_SUPER_ADMIN = "super_admin";

    // ============================================================
    // Post Types
    // ============================================================
    public static final String POST_TYPE_TEXT = "text";
    public static final String POST_TYPE_IMAGE = "image";
    public static final String POST_TYPE_VIDEO = "video";
    public static final String POST_TYPE_POLL = "poll";
    public static final String POST_TYPE_LINK = "link";
    public static final String POST_TYPE_FILE = "file";
    public static final String POST_TYPE_CODE = "code";
    public static final String POST_TYPE_QUOTE = "quote";
    public static final String POST_TYPE_ARTICLE = "article";

    // ============================================================
    // Report Types
    // ============================================================
    public static final String REPORT_SPAM = "spam";
    public static final String REPORT_HARASSMENT = "harassment";
    public static final String REPORT_HATE_SPEECH = "hate_speech";
    public static final String REPORT_VIOLENCE = "violence";
    public static final String REPORT_NUDITY = "nudity";
    public static final String REPORT_FALSE_INFO = "false_info";
    public static final String REPORT_COPYRIGHT = "copyright";
    public static final String REPORT_OTHER = "other";

    // ============================================================
    // Privacy Levels
    // ============================================================
    public static final String PRIVACY_PUBLIC = "public";
    public static final String PRIVACY_PRIVATE = "private";
    public static final String PRIVACY_FRIENDS = "friends";
    public static final String PRIVACY_CUSTOM = "custom";

    // ============================================================
    // Theme Constants
    // ============================================================
    public static final String THEME_LIGHT = "light";
    public static final String THEME_DARK = "dark";
    public static final String THEME_SYSTEM = "system";
    public static final String THEME_AMOLED = "amoled";

    // ============================================================
    // Max Values
    // ============================================================
    public static final int MAX_POST_LENGTH = 5000;
    public static final int MAX_IMAGES = 10;
    public static final long MAX_VIDEO_SIZE = 100 * 1024 * 1024L; // 100MB in bytes
    public static final long MAX_FILE_SIZE = 50 * 1024 * 1024L;   // 50MB in bytes
    public static final int PAGINATION_SIZE = 20;

    // ============================================================
    // Request Codes
    // ============================================================
    public static final int REQUEST_CODE_CAMERA = 1001;
    public static final int REQUEST_CODE_GALLERY = 1002;
    public static final int REQUEST_CODE_VIDEO_CAMERA = 1003;
    public static final int REQUEST_CODE_VIDEO_GALLERY = 1004;
    public static final int REQUEST_CODE_FILE_PICKER = 1005;
    public static final int REQUEST_CODE_LOCATION = 1006;
    public static final int REQUEST_CODE_CONTACTS = 1007;
    public static final int REQUEST_CODE_AUDIO = 1008;
    public static final int REQUEST_CODE_PERMISSIONS = 1010;
    public static final int REQUEST_CODE_EDIT_POST = 1011;
    public static final int REQUEST_CODE_CROP_IMAGE = 1012;

    // ============================================================
    // App Version
    // ============================================================
    public static final String APP_VERSION = "2.0.0";
    public static final int APP_VERSION_CODE = 2;

    // ============================================================
    // Shared Preference Keys
    // ============================================================
    public static final String PREF_NAME = "kimo_social_prefs";
    public static final String KEY_CURRENT_USER = "current_user";
    public static final String KEY_IS_FIRST_LAUNCH = "is_first_launch";
    public static final String KEY_FCM_TOKEN = "fcm_token";
    public static final String KEY_THEME = "theme";
    public static final String KEY_LANGUAGE = "language";
    public static final String KEY_SETTINGS = "app_settings";
    public static final String KEY_POSTS_CACHE = "posts_cache";
    public static final String KEY_USERS_CACHE = "users_cache";
    public static final String KEY_SEARCH_HISTORY = "search_history";

    // ============================================================
    // Notification Channel IDs
    // ============================================================
    public static final String CHANNEL_GENERAL = "channel_general";
    public static final String CHANNEL_MESSAGES = "channel_messages";
    public static final String CHANNEL_GROUPS = "channel_groups";

    // ============================================================
    // Extra Keys
    // ============================================================
    public static final String EXTRA_POST_ID = "extra_post_id";
    public static final String EXTRA_USER_ID = "extra_user_id";
    public static final String EXTRA_COMMENT_ID = "extra_comment_id";
}
