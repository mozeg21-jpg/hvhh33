package com.news.kimo.utils;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for handling Android runtime permissions.
 * Provides convenient methods for requesting camera, storage, and location permissions
 * with rationale checking and result handling via callback.
 */
public class PermissionHelper {

    /**
     * Callback interface for permission request results.
     */
    public interface OnPermissionResultListener {
        /**
         * Called when all requested permissions have been granted.
         */
        void onPermissionsGranted();

        /**
         * Called when one or more permissions were denied.
         *
         * @param deniedPermissions List of denied permission strings
         */
        void onPermissionsDenied(List<String> deniedPermissions);
    }

    private final Activity activity;
    private OnPermissionResultListener resultListener;
    private String[] pendingPermissions;
    private int requestCode;

    // Predefined permission groups
    public static final String[] CAMERA_PERMISSIONS = {
            android.Manifest.permission.CAMERA
    };

    public static final String[] STORAGE_PERMISSIONS;

    static {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            STORAGE_PERMISSIONS = new String[]{
                    android.Manifest.permission.READ_MEDIA_IMAGES,
                    android.Manifest.permission.READ_MEDIA_VIDEO,
                    android.Manifest.permission.READ_MEDIA_AUDIO
            };
        } else {
            STORAGE_PERMISSIONS = new String[]{
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
        }
    }

    public static final String[] LOCATION_PERMISSIONS = {
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
    };

    public static final String[] CAMERA_AND_STORAGE_PERMISSIONS;

    static {
        String[] camera = CAMERA_PERMISSIONS;
        String[] storage = STORAGE_PERMISSIONS;
        CAMERA_AND_STORAGE_PERMISSIONS = new String[camera.length + storage.length];
        System.arraycopy(camera, 0, CAMERA_AND_STORAGE_PERMISSIONS, 0, camera.length);
        System.arraycopy(storage, 0, CAMERA_AND_STORAGE_PERMISSIONS, camera.length, storage.length);
    }

    /**
     * Creates a new PermissionHelper instance.
     *
     * @param activity The activity that will request the permissions
     */
    public PermissionHelper(@NonNull Activity activity) {
        this.activity = activity;
    }

    /**
     * Check and request runtime permissions.
     * If all permissions are already granted, the listener's onPermissionsGranted
     * is called immediately. Otherwise, the permission dialog is shown.
     *
     * @param permissions The array of permissions to request
     * @param requestCode The request code for onActivityResult / onRequestPermissionsResult
     * @param listener    The callback listener for results
     */
    public void checkAndRequestPermissions(@NonNull String[] permissions, int requestCode,
                                           @NonNull OnPermissionResultListener listener) {
        this.resultListener = listener;
        this.requestCode = requestCode;
        this.pendingPermissions = permissions;

        List<String> neededPermissions = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(activity, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(permission);
            }
        }

        if (neededPermissions.isEmpty()) {
            listener.onPermissionsGranted();
        } else {
            ActivityCompat.requestPermissions(activity,
                    neededPermissions.toArray(new String[0]), requestCode);
        }
    }

    /**
     * Check if rationale should be shown for any of the given permissions.
     * Should be called before requesting permissions to decide whether to
     * show an educational UI.
     *
     * @param permissions The array of permissions to check
     * @return true if rationale should be shown for at least one permission
     */
    public boolean shouldShowRationale(@NonNull String[] permissions) {
        for (String permission : permissions) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if all the specified permissions are granted.
     *
     * @param permissions The array of permissions to check
     * @return true if all permissions are granted
     */
    public static boolean allPermissionsGranted(@NonNull Context context,
                                                 @NonNull String[] permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * Handle the result of a permission request.
     * Call this from the Activity's onRequestPermissionsResult.
     * This method must be called so the internal callback is invoked.
     *
     * @param requestCode  The request code received in onRequestPermissionsResult
     * @param permissions  The permissions array received
     * @param grantResults The grant results array received
     */
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (this.resultListener == null || this.pendingPermissions == null) {
            return;
        }

        if (requestCode != this.requestCode) {
            return;
        }

        List<String> deniedPermissions = new ArrayList<>();
        for (int i = 0; i < permissions.length; i++) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                deniedPermissions.add(permissions[i]);
            }
        }

        if (deniedPermissions.isEmpty()) {
            resultListener.onPermissionsGranted();
        } else {
            resultListener.onPermissionsDenied(deniedPermissions);
        }

        // Clear references to prevent leaks
        this.resultListener = null;
        this.pendingPermissions = null;
    }

    /**
     * Check if camera permission is granted.
     *
     * @param context Context
     * @return true if camera permission is granted
     */
    public static boolean isCameraPermissionGranted(@NonNull Context context) {
        return ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Check if storage permission is granted.
     *
     * @param context Context
     * @return true if storage permission is granted
     */
    public static boolean isStoragePermissionGranted(@NonNull Context context) {
        return allPermissionsGranted(context, STORAGE_PERMISSIONS);
    }

    /**
     * Check if location permission is granted.
     *
     * @param context Context
     * @return true if location permission is granted
     */
    public static boolean isLocationPermissionGranted(@NonNull Context context) {
        return allPermissionsGranted(context, LOCATION_PERMISSIONS);
    }
}
