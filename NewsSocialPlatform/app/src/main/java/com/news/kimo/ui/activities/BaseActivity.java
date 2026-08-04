package com.news.kimo.ui.activities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.news.kimo.R;
import com.news.kimo.utils.DateUtils;
import com.news.kimo.utils.NetworkHelper;

/**
 * Base activity for all activities in the application.
 * Provides common functionality: transparent status bar, loading dialog,
 * snackbar messages, network check, keyboard hiding, image loading,
 * relative time formatting, dp conversion, and activity navigation.
 */
public abstract class BaseActivity extends AppCompatActivity {

    private Dialog loadingDialog;
    private NetworkHelper networkHelper;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupStatusBar();
        networkHelper = new NetworkHelper(this);
    }

    // ==================================================================
    // Status Bar
    // ==================================================================

    /**
     * Makes the status bar transparent on SDK 21+.
     * On SDK 23+ the status bar icons remain visible (light mode).
     */
    protected void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            );
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            );
        }
    }

    // ==================================================================
    // Loading Dialog (Lottie)
    // ==================================================================

    /**
     * Shows a non-cancelable loading dialog with a Lottie animation.
     * The dialog uses the layout {@code R.layout.dialog_loading}.
     */
    protected void showLoading() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            return;
        }
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View view = getLayoutInflater().inflate(R.layout.dialog_loading, null);
            builder.setView(view);
            builder.setCancelable(false);
            loadingDialog = builder.create();
            if (loadingDialog.getWindow() != null) {
                loadingDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                loadingDialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
            }
            loadingDialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Dismisses the loading dialog if it is currently showing.
     */
    protected void hideLoading() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            try {
                loadingDialog.dismiss();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        loadingDialog = null;
    }

    // ==================================================================
    // Messages
    // ==================================================================

    /**
     * Shows an error message as a Snackbar anchored at the bottom of the screen.
     *
     * @param message the Arabic error message to display
     */
    protected void showError(String message) {
        if (message == null || message.isEmpty()) {
            message = getString(R.string.error_generic);
        }
        View rootView = getWindow().getDecorView().findViewById(android.R.id.content);
        if (rootView != null) {
            com.google.android.material.snackbar.Snackbar.make(
                    rootView,
                    message,
                    com.google.android.material.snackbar.Snackbar.LENGTH_LONG
            ).setBackgroundTint(ContextCompat.getColor(this, R.color.colorError))
             .setTextColor(Color.WHITE)
             .setAnchorView(null)
             .show();
        } else {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Shows an informational message as a Snackbar anchored at the bottom of the screen.
     *
     * @param message the Arabic message to display
     */
    protected void showMessage(String message) {
        if (message == null || message.isEmpty()) return;
        View rootView = getWindow().getDecorView().findViewById(android.R.id.content);
        if (rootView != null) {
            com.google.android.material.snackbar.Snackbar.make(
                    rootView,
                    message,
                    com.google.android.material.snackbar.Snackbar.LENGTH_LONG
            ).show();
        } else {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
    }

    // ==================================================================
    // Keyboard
    // ==================================================================

    /**
     * Hides the soft keyboard if it is currently visible.
     */
    protected void hideKeyboard() {
        View view = getCurrentFocus();
        if (view == null) {
            view = new View(this);
        }
        try {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ==================================================================
    // Network
    // ==================================================================

    /**
     * Checks whether the device has an active network connection.
     *
     * @return {@code true} if network is available, {@code false} otherwise
     */
    protected boolean isNetworkAvailable() {
        if (networkHelper != null) {
            return networkHelper.isNetworkAvailable();
        }
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }

    // ==================================================================
    // Navigation
    // ==================================================================

    /**
     * Opens the target activity with no extras.
     *
     * @param cls the activity class to start
     */
    protected void openActivity(Class cls) {
        Intent intent = new Intent(this, cls);
        startActivity(intent);
    }

    /**
     * Opens the target activity with a {@link Bundle} of extras.
     *
     * @param cls    the activity class to start
     * @param extras the bundle to pass
     */
    protected void openActivity(Class cls, Bundle extras) {
        Intent intent = new Intent(this, cls);
        if (extras != null) {
            intent.putExtras(extras);
        }
        startActivity(intent);
    }

    /**
     * Opens the target activity with a shared-element / fade transition.
     *
     * @param cls the activity class to start
     */
    protected void openActivityWithTransition(Class cls) {
        Intent intent = new Intent(this, cls);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    // ==================================================================
    // Utilities
    // ==================================================================

    /**
     * Converts a density-independent pixel (dp) value to actual pixels.
     *
     * @param value the value in dp
     * @return the value in pixels
     */
    protected int dp(float value) {
        float density = getResources().getDisplayMetrics().density;
        return (int) (value * density + 0.5f);
    }

    /**
     * Returns a human-readable Arabic relative time string for the given timestamp.
     *
     * @param timestamp the epoch timestamp in milliseconds
     * @return Arabic relative time string (e.g. "منذ 3 دقائق")
     */
    protected String getRelativeTime(long timestamp) {
        return DateUtils.formatRelativeTimeArabic(timestamp);
    }

    // ==================================================================
    // Image Loading (Glide)
    // ==================================================================

    /**
     * Loads a circular image into the target ImageView using Glide.
     *
     * @param url       the image URL (may be null or empty)
     * @param imageView the target ImageView
     */
    protected void loadCircularImage(String url, ImageView imageView) {
        if (imageView == null) return;
        RequestOptions options = new RequestOptions()
                .centerCrop()
                .circleCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.ic_placeholder_avatar)
                .error(R.drawable.ic_placeholder_avatar);

        Glide.with(this)
                .load(url)
                .apply(options)
                .into(imageView);
    }

    /**
     * Loads an image into the target ImageView using Glide with center-crop.
     *
     * @param url       the image URL (may be null or empty)
     * @param imageView the target ImageView
     */
    protected void loadImage(String url, ImageView imageView) {
        if (imageView == null) return;
        RequestOptions options = new RequestOptions()
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.ic_placeholder_image)
                .error(R.drawable.ic_placeholder_image);

        Glide.with(this)
                .load(url)
                .apply(options)
                .into(imageView);
    }

    /**
     * Loads a profile image with circle crop. If the URL is null/empty,
     * generates a fallback with the user's initial letter on a colored background.
     *
     * @param url       the photo URL (may be null or empty)
     * @param imageView the target ImageView
     * @param name      the user's display name (used for fallback initial)
     */
    protected void loadProfileImage(String url, ImageView imageView, String name) {
        if (imageView == null) return;
        if (url != null && !url.trim().isEmpty()) {
            RequestOptions options = new RequestOptions()
                    .centerCrop()
                    .circleCrop()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.ic_placeholder_avatar)
                    .error(R.drawable.ic_placeholder_avatar);

            Glide.with(this)
                    .load(url)
                    .apply(options)
                    .into(imageView);
        } else {
            // Fallback: show initial letter using a generated drawable
            imageView.setImageDrawable(
                    createLetterPlaceholder(name)
            );
        }
    }

    /**
     * Creates a circular {@link android.graphics.drawable.Drawable} containing
     * the first letter of the given name on a coloured background.
     */
    private android.graphics.drawable.Drawable createLetterPlaceholder(String name) {
        String letter = "?";
        if (name != null && !name.trim().isEmpty()) {
            letter = name.trim().substring(0, 1);
        }
        android.graphics.drawable.GradientDrawable drawable = new android.graphics.drawable.GradientDrawable();
        drawable.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        drawable.setColor(ContextCompat.getColor(this, R.color.colorPrimary));
        drawable.setSize(dp(48), dp(48));

        // Build a text drawable via a Bitmap
        int size = dp(48);
        android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
        drawable.setBounds(0, 0, size, size);
        drawable.draw(canvas);

        android.graphics.Paint paint = new android.graphics.Paint();
        paint.setColor(Color.WHITE);
        paint.setAntiAlias(true);
        paint.setTextAlign(android.graphics.Paint.Align.CENTER);
        paint.setTextSize(size * 0.45f);
        android.graphics.Rect bounds = new android.graphics.Rect();
        paint.getTextBounds(letter, 0, letter.length(), bounds);
        float y = (size / 2f) + (bounds.height() / 2f);
        canvas.drawText(letter, size / 2f, y, paint);

        return new android.graphics.drawable.BitmapDrawable(getResources(), bitmap);
    }

    // ==================================================================
    // Lifecycle
    // ==================================================================

    @Override
    protected void onDestroy() {
        hideLoading();
        super.onDestroy();
    }
}
