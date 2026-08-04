package com.news.kimo.ui.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.news.kimo.R;
import com.news.kimo.databinding.ActivityForgotPasswordBinding;
import com.news.kimo.utils.ValidationUtils;

/**
 * Forgot-password screen activity.
 * <p>
 * Allows the user to enter their email address and request a
 * password-reset link via {@link FirebaseAuth#sendPasswordResetEmail(String)}.
 * On success a confirmation message is displayed and the user can
 * navigate back to the login screen.
 */
public class ForgotPasswordActivity extends BaseActivity {

    private static final String TAG = "ForgotPasswordActivity";

    private ActivityForgotPasswordBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityForgotPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initViews();
        initListeners();
    }

    // ==================================================================
    // Initialisation
    // ==================================================================

    private void initViews() {
        binding.toolbar.setTitle(R.string.forgot_password_title);
        binding.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Show the email from the intent if passed from LoginActivity
        if (getIntent() != null && getIntent().hasExtra("email")) {
            String email = getIntent().getStringExtra("email");
            if (email != null) {
                binding.etEmail.setText(email);
            }
        }
    }

    private void initListeners() {
        binding.btnSendResetLink.setOnClickListener(v -> attemptSendResetLink());

        binding.tvBackToLogin.setOnClickListener(v -> {
            openActivityWithTransition(LoginActivity.class);
            finish();
        });
    }

    // ==================================================================
    // Validation
    // ==================================================================

    private boolean validateEmail() {
        String email = binding.etEmail.getText() != null
                ? binding.etEmail.getText().toString().trim() : "";

        binding.tilEmail.setError(null);

        if (TextUtils.isEmpty(email)) {
            binding.tilEmail.setError(getString(R.string.error_email_empty));
            binding.tilEmail.requestFocus();
            return false;
        }

        if (!ValidationUtils.isValidEmail(email)) {
            binding.tilEmail.setError(getString(R.string.error_email_invalid));
            binding.tilEmail.requestFocus();
            return false;
        }

        return true;
    }

    // ==================================================================
    // Send Reset Link
    // ==================================================================

    /**
     * Validates the email input and sends a password-reset email.
     */
    private void attemptSendResetLink() {
        if (!isNetworkAvailable()) {
            showError(getString(R.string.error_no_internet));
            return;
        }

        if (!validateEmail()) return;

        String email = binding.etEmail.getText().toString().trim();

        setFormEnabled(false);
        showLoading();

        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    hideLoading();
                    if (task.isSuccessful()) {
                        // Show success state
                        binding.layoutForm.setVisibility(android.view.View.GONE);
                        binding.layoutSuccess.setVisibility(android.view.View.VISIBLE);
                        binding.tvSuccessMessage.setText(
                                getString(R.string.reset_link_sent, email)
                        );
                        showMessage(getString(R.string.reset_link_sent_success));
                    } else {
                        setFormEnabled(true);
                        Exception exception = task.getException();
                        Log.e(TAG, "Failed to send reset link", exception);

                        if (exception != null && exception.getMessage() != null) {
                            String msg = exception.getMessage();
                            if (msg.contains("no user record") || msg.contains("user not found")) {
                                binding.tilEmail.setError(getString(R.string.error_user_not_found));
                            } else if (msg.contains("network")) {
                                showError(getString(R.string.error_no_internet));
                            } else {
                                showError(getString(R.string.error_send_reset_failed));
                            }
                        } else {
                            showError(getString(R.string.error_generic));
                        }
                    }
                });
    }

    // ==================================================================
    // Helpers
    // ==================================================================

    /**
     * Enables or disables form inputs during the request.
     */
    private void setFormEnabled(boolean enabled) {
        binding.etEmail.setEnabled(enabled);
        binding.btnSendResetLink.setEnabled(enabled);
        binding.tvBackToLogin.setEnabled(enabled);

        binding.btnSendResetLink.setAlpha(enabled ? 1f : 0.5f);
    }
}
