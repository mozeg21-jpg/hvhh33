package com.news.kimo.ui.activities;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.news.kimo.R;
import com.news.kimo.databinding.ActivityEmailVerificationBinding;
import com.news.kimo.utils.SessionManager;

import java.util.Locale;

/**
 * Email verification screen.
 * <p>
 * Displays the current user's email, periodically (every 3 seconds)
 * reloads the Firebase user to check {@code isEmailVerified()}, provides
 * a resend-verification-email button with a 60-second cooldown, and
 * allows the user to continue to {@link MainActivity} once verified.
 */
public class EmailVerificationActivity extends BaseActivity {

    private static final String TAG = "EmailVerificationActivity";
    private static final long AUTO_CHECK_INTERVAL_MS = 3000L;
    private static final long RESEND_COOLDOWN_MS = 60000L;

    private ActivityEmailVerificationBinding binding;

    private CountDownTimer autoCheckTimer;
    private CountDownTimer resendCooldownTimer;
    private boolean isResendCooldownActive = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEmailVerificationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initViews();
        initListeners();
        startAutoCheck();
    }

    // ==================================================================
    // Initialisation
    // ==================================================================

    private void initViews() {
        binding.toolbar.setTitle(R.string.verification_title);
        binding.toolbar.setNavigationOnClickListener(v -> {
            stopAutoCheck();
            openActivityWithTransition(LoginActivity.class);
            finish();
        });

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.getEmail() != null) {
            binding.tvEmail.setText(currentUser.getEmail());
        }
    }

    private void initListeners() {
        // Continue button
        binding.btnContinue.setOnClickListener(v -> checkVerificationAndContinue());

        // Resend verification email
        binding.btnResendEmail.setOnClickListener(v -> resendVerificationEmail());

        // Change email → go back to login
        binding.tvChangeEmail.setOnClickListener(v -> {
            stopAutoCheck();
            FirebaseAuth.getInstance().signOut();
            SessionManager.getInstance(this).clearAllSessionData();
            openActivityWithTransition(LoginActivity.class);
            finish();
        });
    }

    // ==================================================================
    // Auto-Check Verification
    // ==================================================================

    /**
     * Starts a repeating timer that reloads the Firebase user every
     * 3 seconds to check whether the email has been verified.
     */
    private void startAutoCheck() {
        autoCheckTimer = new CountDownTimer(Long.MAX_VALUE, AUTO_CHECK_INTERVAL_MS) {
            @Override
            public void onTick(long millisUntilFinished) {
                checkEmailVerified();
            }

            @Override
            public void onFinish() {
                // Never finishes
            }
        }.start();
    }

    /**
     * Cancels the auto-check timer.
     */
    private void stopAutoCheck() {
        if (autoCheckTimer != null) {
            autoCheckTimer.cancel();
            autoCheckTimer = null;
        }
    }

    // ==================================================================
    // Verification Check
    // ==================================================================

    /**
     * Reloads the current Firebase user and checks if the email is verified.
     * If verified, updates the UI and enables the continue button.
     */
    private void checkEmailVerified() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "No current user — stopping auto check");
            stopAutoCheck();
            return;
        }

        currentUser.reload()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        boolean verified = currentUser.isEmailVerified();
                        updateVerificationUI(verified);
                    } else {
                        Log.w(TAG, "Failed to reload user", task.getException());
                    }
                });
    }

    /**
     * Updates the UI based on the verification status.
     *
     * @param verified {@code true} if email is verified
     */
    private void updateVerificationUI(boolean verified) {
        if (verified) {
            binding.ivCheckIcon.setImageResource(R.drawable.ic_check_circle);
            binding.ivCheckIcon.setColorFilter(
                    getResources().getColor(R.color.colorSuccess, null)
            );
            binding.tvVerificationStatus.setText(R.string.email_verified);
            binding.btnContinue.setEnabled(true);
            binding.btnContinue.setAlpha(1f);
            stopAutoCheck();
        } else {
            binding.ivCheckIcon.setImageResource(R.drawable.ic_hourglass);
            binding.ivCheckIcon.setColorFilter(
                    getResources().getColor(R.color.colorWarning, null)
            );
            binding.tvVerificationStatus.setText(R.string.email_not_verified);
            binding.btnContinue.setEnabled(false);
            binding.btnContinue.setAlpha(0.5f);
        }
    }

    /**
     * Checks if the user's email is verified and, if so, navigates
     * to {@link MainActivity}. Otherwise, shows a message.
     */
    private void checkVerificationAndContinue() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            showError(getString(R.string.error_generic));
            return;
        }

        currentUser.reload()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (currentUser.isEmailVerified()) {
                            openActivityWithTransition(MainActivity.class);
                            finish();
                        } else {
                            showMessage(getString(R.string.email_not_verified_yet));
                        }
                    } else {
                        showError(getString(R.string.error_check_verification_failed));
                        Log.e(TAG, "Failed to reload user for verification check",
                                task.getException());
                    }
                });
    }

    // ==================================================================
    // Resend Email
    // ==================================================================

    /**
     * Sends a verification email to the current user and starts a
     * 60-second cooldown timer to prevent spamming.
     */
    private void resendVerificationEmail() {
        if (isResendCooldownActive) {
            showMessage(getString(R.string.resend_cooldown_message));
            return;
        }

        if (!isNetworkAvailable()) {
            showError(getString(R.string.error_no_internet));
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            showError(getString(R.string.error_generic));
            return;
        }

        showLoading();

        currentUser.sendEmailVerification()
                .addOnCompleteListener(task -> {
                    hideLoading();
                    if (task.isSuccessful()) {
                        showMessage(getString(R.string.verification_email_sent));
                        startResendCooldown();
                    } else {
                        showError(getString(R.string.error_send_verification_failed));
                        Log.e(TAG, "Failed to send verification email", task.getException());
                    }
                });
    }

    /**
     * Starts a 60-second cooldown timer during which the resend
     * button is disabled and shows a countdown.
     */
    private void startResendCooldown() {
        isResendCooldownActive = true;
        binding.btnResendEmail.setEnabled(false);
        binding.btnResendEmail.setAlpha(0.5f);

        if (resendCooldownTimer != null) {
            resendCooldownTimer.cancel();
        }

        resendCooldownTimer = new CountDownTimer(RESEND_COOLDOWN_MS, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long seconds = millisUntilFinished / 1000;
                binding.btnResendEmail.setText(
                        getString(R.string.resend_countdown,
                                String.format(Locale.getDefault(), "%d", seconds))
                );
            }

            @Override
            public void onFinish() {
                isResendCooldownActive = false;
                binding.btnResendEmail.setEnabled(true);
                binding.btnResendEmail.setAlpha(1f);
                binding.btnResendEmail.setText(R.string.resend_verification_email);
            }
        }.start();
    }

    // ==================================================================
    // Lifecycle
    // ==================================================================

    @Override
    protected void onDestroy() {
        stopAutoCheck();
        if (resendCooldownTimer != null) {
            resendCooldownTimer.cancel();
            resendCooldownTimer = null;
        }
        super.onDestroy();
    }
}
