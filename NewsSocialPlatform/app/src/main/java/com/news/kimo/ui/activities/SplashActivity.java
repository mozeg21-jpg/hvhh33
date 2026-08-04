package com.news.kimo.ui.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.news.kimo.R;
import com.news.kimo.database.FirestoreHelper;
import com.news.kimo.models.AppConfig;

/**
 * Splash / launch screen activity.
 * <p>
 * Displays a Lottie animation for 3 seconds, checks the app's
 * maintenance-mode configuration from Firebase, checks the user's
 * authentication state, and then routes to the appropriate screen.
 */
public class SplashActivity extends BaseActivity {

    private static final String TAG = "SplashActivity";
    private static final int SPLASH_DELAY_MS = 3000;

    private LinearLayout dotsContainer;
    private ImageView dot1, dot2, dot3;
    private com.airbnb.lottie.LottieAnimationView lottieAnimation;
    private TextView tvAppName;
    private TextView tvAppSlogan;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean hasNavigated = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Full-screen immersive mode for splash
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
        }

        setContentView(R.layout.activity_splash);

        initViews();
        animateDots();
        startLottieAnimation();
        checkAppConfig();
    }

    /**
     * Binds view references from the splash layout.
     */
    private void initViews() {
        try {
            lottieAnimation = findViewById(R.id.lottie_splash);
            tvAppName = findViewById(R.id.tv_splash_app_name);
            tvAppSlogan = findViewById(R.id.tv_splash_slogan);
            dotsContainer = findViewById(R.id.dots_container);
            dot1 = findViewById(R.id.dot_1);
            dot2 = findViewById(R.id.dot_2);
            dot3 = findViewById(R.id.dot_3);
        } catch (Exception e) {
            Log.e(TAG, "Failed to init splash views", e);
        }
    }

    /**
     * Starts the Lottie animation and loops it.
     */
    private void startLottieAnimation() {
        if (lottieAnimation != null) {
            try {
                lottieAnimation.playAnimation();
                lottieAnimation.loop(true);
            } catch (Exception e) {
                Log.w(TAG, "Lottie animation not available", e);
            }
        }
    }

    /**
     * Animates three white dots in a pulsing sequence using
     * scale and alpha animations.
     */
    private void animateDots() {
        if (dotsContainer == null || dot1 == null) return;

        // Fade in the dots container
        dotsContainer.setVisibility(View.VISIBLE);
        AlphaAnimation fadeIn = new AlphaAnimation(0f, 1f);
        fadeIn.setDuration(500);
        dotsContainer.startAnimation(fadeIn);

        // Pulse each dot with a staggered delay
        animateDot(dot1, 0);
        animateDot(dot2, 300);
        animateDot(dot3, 600);
    }

    /**
     * Applies a repeating pulse (scale + alpha) animation to a dot ImageView.
     *
     * @param dot          the dot ImageView
     * @param delayMs      initial delay before the animation starts
     */
    private void animateDot(ImageView dot, long delayMs) {
        if (dot == null) return;

        ScaleAnimation pulse = new ScaleAnimation(
                1f, 1.5f, 1f, 1.5f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        pulse.setDuration(600);
        pulse.setRepeatCount(Animation.INFINITE);
        pulse.setRepeatMode(Animation.REVERSE);
        pulse.setInterpolator(new AccelerateDecelerateInterpolator());
        pulse.setStartTime(Animation.START_ON_FIRST_FRAME);

        AlphaAnimation alphaPulse = new AlphaAnimation(1f, 0.4f);
        alphaPulse.setDuration(600);
        alphaPulse.setRepeatCount(Animation.INFINITE);
        alphaPulse.setRepeatMode(Animation.REVERSE);

        handler.postDelayed(() -> {
            if (dot != null && !isFinishing()) {
                dot.startAnimation(pulse);
                dot.startAnimation(alphaPulse);
            }
        }, delayMs);
    }

    // ==================================================================
    // App Config & Navigation
    // ==================================================================

    /**
     * Reads the {@code app_config} node from Firebase to check for
     * maintenance mode. After a 3-second delay, navigates the user.
     */
    private void checkAppConfig() {
        handler.postDelayed(this::navigateUser, SPLASH_DELAY_MS);

        // Check maintenance mode in parallel
        try {
            FirestoreHelper.getInstance().getAppConfigRef()
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (!snapshot.exists()) {
                                return; // No config → proceed normally
                            }
                            AppConfig config = snapshot.getValue(AppConfig.class);
                            if (config != null && config.isMaintenanceMode()) {
                                handler.removeCallbacksAndMessages(null);
                                showMaintenanceDialog(config);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.w(TAG, "Failed to read app config: " + error.getMessage());
                            // Continue with normal navigation on error
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error checking app config", e);
        }
    }

    /**
     * Shows an AlertDialog when the app is in maintenance mode.
     *
     * @param config the app configuration containing the maintenance message
     */
    private void showMaintenanceDialog(AppConfig config) {
        String message = config.getMaintenanceMessage();
        if (message == null || message.trim().isEmpty()) {
            message = getString(R.string.maintenance_message);
        }

        new android.app.AlertDialog.Builder(this)
                .setTitle(R.string.maintenance_title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> finishAffinity())
                .show();
    }

    /**
     * Navigates to the appropriate activity based on auth state.
     * <ul>
     *   <li>Logged in → {@link MainActivity}</li>
     *   <li>Not logged in → {@link LoginActivity}</li>
     * </ul>
     */
    private void navigateUser() {
        if (hasNavigated) return;
        hasNavigated = true;

        // Stop lottie before transition
        if (lottieAnimation != null) {
            lottieAnimation.cancelAnimation();
        }

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            openActivityWithTransition(MainActivity.class);
        } else {
            openActivityWithTransition(LoginActivity.class);
        }
        finish();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
