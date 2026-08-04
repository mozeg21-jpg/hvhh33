package com.news.kimo.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Utility class for file operations.
 * Provides methods for creating temp files, getting file metadata,
 * copying files, reading bytes, and MIME type detection.
 */
public class FileUtil {

    private static final String TEMP_IMAGE_PREFIX = "IMG_";
    private static final String TEMP_VIDEO_PREFIX = "VID_";
    private static final String TEMP_FILE_PREFIX = "FILE_";
    private static final String JPEG_EXTENSION = ".jpg";
    private static final String MP4_EXTENSION = ".mp4";
    private static final String BUFFER_SIZE = "8192";

    private FileUtil() {
        // Prevent instantiation
    }

    /**
     * Create a temporary file in the app's cache directory.
     *
     * @param context Context
     * @return The created temp File, or null on error
     */
    public static File createTempFile(Context context) {
        return createTempFile(context, TEMP_FILE_PREFIX, ".tmp");
    }

    /**
     * Create a temporary file with a given prefix and suffix.
     *
     * @param context Context
     * @param prefix  File name prefix
     * @param suffix  File name suffix (e.g. ".jpg")
     * @return The created temp File, or null on error
     */
    public static File createTempFile(Context context, String prefix, String suffix) {
        File cacheDir = context.getCacheDir();
        if (cacheDir == null) {
            return null;
        }
        try {
            String fileName = prefix + UUID.randomUUID().toString() + suffix;
            File tempFile = new File(cacheDir, fileName);
            if (tempFile.createNewFile()) {
                return tempFile;
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Get the file extension from a URI.
     *
     * @param context Context
     * @param uri     The URI to extract extension from
     * @return The file extension (lowercase, without dot), or empty string
     */
    public static String getFileExtensionFromUri(Context context, Uri uri) {
        String extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
        if (!extension.isEmpty()) {
            return extension.toLowerCase();
        }

        // Fallback: get from content resolver
        ContentResolver contentResolver = context.getContentResolver();
        String mimeType = contentResolver.getType(uri);
        if (mimeType != null) {
            extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
            if (extension != null) {
                return extension.toLowerCase();
            }
        }

        // Fallback: get from URI path
        String path = uri.getPath();
        if (path != null) {
            int dotIndex = path.lastIndexOf('.');
            if (dotIndex >= 0 && dotIndex < path.length() - 1) {
                return path.substring(dotIndex + 1).toLowerCase();
            }
        }

        return "";
    }

    /**
     * Get the file name from a URI using the ContentResolver.
     *
     * @param context Context
     * @param uri     The URI
     * @return The display name, or a generated name if not available
     */
    public static String getFileNameFromUri(Context context, Uri uri) {
        String fileName = "file_" + System.currentTimeMillis();
        ContentResolver contentResolver = context.getContentResolver();

        Cursor cursor = null;
        try {
            cursor = contentResolver.query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIndex >= 0) {
                    String name = cursor.getString(nameIndex);
                    if (name != null && !name.isEmpty()) {
                        fileName = name;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return fileName;
    }

    /**
     * Get the file size from a URI using the ContentResolver.
     *
     * @param context Context
     * @param uri     The URI
     * @return File size in bytes, or -1 if unavailable
     */
    public static long getFileSize(Context context, Uri uri) {
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = null;
        try {
            cursor = contentResolver.query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (sizeIndex >= 0) {
                    return cursor.getLong(sizeIndex);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return -1;
    }

    /**
     * Copy a file from a URI to the app's internal storage.
     *
     * @param context  Context
     * @param uri      The source URI
     * @param destFile The destination File
     * @return true if copy was successful, false otherwise
     */
    public static boolean copyFileToInternalStorage(Context context, Uri uri, File destFile) {
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                return false;
            }
            outputStream = new FileOutputStream(destFile);
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (inputStream != null) inputStream.close();
            } catch (Exception ignored) {
            }
            try {
                if (outputStream != null) outputStream.close();
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Delete a file. Does nothing if the file is null or doesn't exist.
     *
     * @param file The file to delete
     * @return true if the file was deleted, false otherwise
     */
    public static boolean deleteFile(File file) {
        if (file == null || !file.exists()) {
            return false;
        }
        return file.delete();
    }

    /**
     * Get the MIME type of a file from its extension.
     *
     * @param fileName The file name
     * @return The MIME type, or "application/octet-stream" if unknown
     */
    public static String getMimeType(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "application/octet-stream";
        }
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0) {
            return "application/octet-stream";
        }
        String extension = fileName.substring(dotIndex + 1).toLowerCase();
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        return mimeType != null ? mimeType : "application/octet-stream";
    }

    /**
     * Get the MIME type of a URI.
     *
     * @param context Context
     * @param uri     The URI
     * @return The MIME type, or "application/octet-stream" if unknown
     */
    public static String getMimeType(Context context, Uri uri) {
        String mimeType = context.getContentResolver().getType(uri);
        if (mimeType != null) {
            return mimeType;
        }
        return getMimeType(getFileNameFromUri(context, uri));
    }

    /**
     * Create a new image file in the app's cache directory.
     *
     * @param context Context
     * @return The created image File, or null on error
     */
    public static File createImageFile(Context context) {
        return createTempFile(context, TEMP_IMAGE_PREFIX, JPEG_EXTENSION);
    }

    /**
     * Create a new video file in the app's cache directory.
     *
     * @param context Context
     * @return The created video File, or null on error
     */
    public static File createVideoFile(Context context) {
        return createTempFile(context, TEMP_VIDEO_PREFIX, MP4_EXTENSION);
    }

    /**
     * Read all bytes from a URI using the ContentResolver.
     *
     * @param context Context
     * @param uri     The URI to read from
     * @return Byte array of the file contents, or null on error
     */
    public static byte[] readBytesFromUri(Context context, Uri uri) {
        InputStream inputStream = null;
        try {
            inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                return null;
            }
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                bos.write(buffer, 0, bytesRead);
            }
            return bos.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception ignored) {
                }
            }
        }
    }
}