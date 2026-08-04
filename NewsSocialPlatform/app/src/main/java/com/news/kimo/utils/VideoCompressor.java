package com.news.kimo.utils;

import android.media.MediaMetadataRetriever;
import android.graphics.Bitmap;

import java.io.File;
import java.util.HashMap;

/**
 * Utility class for video-related operations such as extracting thumbnails,
 * getting duration, file size info, and validating video files.
 */
public class VideoCompressor {

    private static final long MIN_VALID_VIDEO_SIZE = 1024L; // 1KB minimum for a valid video
    private static final long MAX_VALID_VIDEO_DURATION_MS = 3 * 60 * 60 * 1000L; // 3 hours max

    private VideoCompressor() {
        // Prevent instantiation
    }

    /**
     * Get a thumbnail bitmap from a video file path.
     *
     * @param filePath Absolute path to the video file
     * @param timeUs   Time in microseconds at which to retrieve the frame (or -1 for default)
     * @return Thumbnail bitmap, or null if extraction fails
     */
    public static Bitmap getVideoThumbnail(String filePath, long timeUs) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(filePath);
            if (timeUs > 0) {
                return retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
            } else {
                return retriever.getFrameAtTime();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                retriever.release();
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Get a thumbnail from a video file at the default frame.
     *
     * @param filePath Absolute path to the video file
     * @return Thumbnail bitmap, or null if extraction fails
     */
    public static Bitmap getVideoThumbnail(String filePath) {
        return getVideoThumbnail(filePath, -1);
    }

    /**
     * Get the duration of a video file in milliseconds.
     *
     * @param filePath Absolute path to the video file
     * @return Duration in milliseconds, or -1 if unable to retrieve
     */
    public static long getVideoDuration(String filePath) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(filePath);
            String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (durationStr != null) {
                return Long.parseLong(durationStr);
            }
            return -1;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        } finally {
            try {
                retriever.release();
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Get a formatted string with video file size info.
     *
     * @param filePath Absolute path to the video file
     * @return A human-readable string with file size and duration, e.g. "12.5 MB | 2:30"
     */
    public static String getVideoSizeInfo(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            return "Invalid file";
        }

        long fileSize = file.length();
        String sizeStr = formatFileSize(fileSize);

        long durationMs = getVideoDuration(filePath);
        String durationStr = "";
        if (durationMs > 0) {
            long totalSeconds = durationMs / 1000;
            long minutes = totalSeconds / 60;
            long seconds = totalSeconds % 60;
            if (minutes > 60) {
                long hours = minutes / 60;
                minutes = minutes % 60;
                durationStr = String.format("%d:%02d:%02d", hours, minutes, seconds);
            } else {
                durationStr = String.format("%d:%02d", minutes, seconds);
            }
        }

        if (durationMs > 0) {
            return sizeStr + " | " + durationStr;
        }
        return sizeStr;
    }

    /**
     * Check if a video file is valid (exists, has content, and is within duration limits).
     *
     * @param filePath Absolute path to the video file
     * @return true if the video is valid, false otherwise
     */
    public static boolean isVideoValid(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            return false;
        }

        File file = new File(filePath);
        if (!file.exists() || !file.canRead()) {
            return false;
        }

        if (file.length() < MIN_VALID_VIDEO_SIZE) {
            return false;
        }

        long duration = getVideoDuration(filePath);
        if (duration <= 0) {
            return false;
        }

        if (duration > MAX_VALID_VIDEO_DURATION_MS) {
            return false;
        }

        return true;
    }

    /**
     * Get the width of a video.
     *
     * @param filePath Absolute path to the video file
     * @return Width in pixels, or -1 if unable to retrieve
     */
    public static int getVideoWidth(String filePath) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(filePath);
            String widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
            if (widthStr != null) {
                return Integer.parseInt(widthStr);
            }
            return -1;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        } finally {
            try {
                retriever.release();
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Get the height of a video.
     *
     * @param filePath Absolute path to the video file
     * @return Height in pixels, or -1 if unable to retrieve
     */
    public static int getVideoHeight(String filePath) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(filePath);
            String heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
            if (heightStr != null) {
                return Integer.parseInt(heightStr);
            }
            return -1;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        } finally {
            try {
                retriever.release();
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Format a file size in bytes to a human-readable string.
     *
     * @param bytes File size in bytes
     * @return Formatted string like "12.5 MB" or "1.2 KB"
     */
    private static String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
}