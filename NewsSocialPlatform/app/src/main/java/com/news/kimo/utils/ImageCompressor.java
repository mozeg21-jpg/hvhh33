package com.news.kimo.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.net.Uri;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Utility class for compressing and resizing images.
 * Provides methods for bitmap quality compression, dimension resizing,
 * target file size compression, and file I/O operations for images.
 */
public class ImageCompressor {

    private static final int DEFAULT_QUALITY = 80;
    private static final int DEFAULT_MAX_DIMENSION = 1920;

    private ImageCompressor() {
        // Prevent instantiation
    }

    /**
     * Compress a bitmap by quality (0-100).
     *
     * @param bitmap  The source bitmap to compress
     * @param quality Compression quality (0 = max compression, 100 = no compression)
     * @return Compressed byte array in JPEG format
     */
    public static byte[] compressByQuality(Bitmap bitmap, int quality) {
        quality = Math.max(0, Math.min(100, quality));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
        return outputStream.toByteArray();
    }

    /**
     * Resize a bitmap to fit within the specified maximum dimension while maintaining aspect ratio.
     *
     * @param bitmap        The source bitmap
     * @param maxDimension  The maximum width or height in pixels
     * @return A new resized bitmap, or the original if it already fits
     */
    public static Bitmap resizeBitmap(Bitmap bitmap, int maxDimension) {
        if (bitmap == null) {
            return null;
        }
        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();

        if (originalWidth <= maxDimension && originalHeight <= maxDimension) {
            return bitmap;
        }

        float ratio;
        if (originalWidth > originalHeight) {
            ratio = (float) maxDimension / originalWidth;
        } else {
            ratio = (float) maxDimension / originalHeight;
        }

        int targetWidth = (int) (originalWidth * ratio);
        int targetHeight = (int) (originalHeight * ratio);

        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true);
    }

    /**
     * Compress an image file to a target size in KB by iteratively reducing quality.
     *
     * @param imageFile The source image file
     * @param targetKB  Target file size in kilobytes
     * @return A compressed File, or the original file if it is already under the target size
     * @throws IOException if an I/O error occurs
     */
    public static File compressImageToTargetSize(File imageFile, int targetKB) throws IOException {
        if (imageFile == null || !imageFile.exists()) {
            throw new IOException("Image file does not exist");
        }

        long targetBytes = targetKB * 1024L;
        if (imageFile.length() <= targetBytes) {
            return imageFile;
        }

        // First, decode and resize the bitmap
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);

        options.inSampleSize = calculateInSampleSize(options, DEFAULT_MAX_DIMENSION, DEFAULT_MAX_DIMENSION);
        options.inJustDecodeBounds = false;

        Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);
        if (bitmap == null) {
            throw new IOException("Failed to decode image file");
        }

        // Iteratively compress to reach target size
        int quality = 90;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);

        while (outputStream.size() > targetBytes && quality > 5) {
            outputStream.reset();
            quality -= 5;
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
        }

        // Write the compressed bytes to a new file
        File outputFile = File.createTempFile("compressed_", ".jpg", imageFile.getParentFile());
        FileOutputStream fos = new FileOutputStream(outputFile);
        fos.write(outputStream.toByteArray());
        fos.flush();
        fos.close();
        outputStream.close();

        if (!bitmap.isRecycled()) {
            bitmap.recycle();
        }

        return outputFile;
    }

    /**
     * Get a bitmap from a URI with in-sampling to avoid OOM on large images.
     *
     * @param context  Context for content resolution
     * @param uri      The image URI
     * @param reqWidth  The requested width for subsampling
     * @param reqHeight The requested height for subsampling
     * @return The decoded bitmap, or null if decoding fails
     */
    public static Bitmap getBitmapFromUri(Context context, Uri uri, int reqWidth, int reqHeight) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                return null;
            }

            // First decode with inJustDecodeBounds=true to check dimensions
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();

            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
            options.inJustDecodeBounds = false;

            // Decode the bitmap with inSampleSize set
            inputStream = context.getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();

            return bitmap;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Save a bitmap to a file in JPEG format.
     *
     * @param bitmap  The bitmap to save
     * @param file    The destination file
     * @param quality Compression quality (0-100)
     * @return true if saved successfully, false otherwise
     */
    public static boolean saveBitmapToFile(Bitmap bitmap, File file, int quality) {
        if (bitmap == null || file == null) {
            return false;
        }
        quality = Math.max(0, Math.min(100, quality));

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, fos);
            fos.flush();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    /**
     * Calculate an appropriate inSampleSize for BitmapFactory.Options based on
     * the requested width and height.
     *
     * @param options   BitmapFactory options with outWidth and outHeight already set
     * @param reqWidth  The requested width
     * @param reqHeight The requested height
     * @return The calculated inSampleSize (power of two)
     */
    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    /**
     * Read bytes from a file.
     *
     * @param file The file to read
     * @return Byte array of the file contents, or null on error
     */
    public static byte[] readFileBytes(File file) {
        if (file == null || !file.exists()) {
            return null;
        }
        try {
            FileInputStream fis = new FileInputStream(file);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                bos.write(buffer, 0, bytesRead);
            }
            fis.close();
            bos.close();
            return bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
