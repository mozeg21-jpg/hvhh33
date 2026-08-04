package com.news.kimo.ui.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.news.kimo.R;
import com.news.kimo.database.FirestoreHelper;
import com.news.kimo.databinding.ActivityRegisterBinding;
import com.news.kimo.utils.Constants;
import com.news.kimo.utils.ValidationUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Registration screen activity.
 * <p>
 * Collects name, email, password, and confirm-password, validates all
 * inputs (including a terms-and-conditions checkbox), creates the
 * Firebase Auth account, updates the display name, persists a full
 * user record to the Realtime Database, and navigates to
 * {@link EmailVerificationActivity}.
 */
public class RegisterActivity extends BaseActivity {

    private static final String TAG = "RegisterActivity";

    private ActivityRegisterBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initViews();
        initListeners();
    }

    // ==================================================================
    // Initialisation
    // ==================================================================

    private void initViews() {
        binding.toolbar.setTitle(R.string.register_title);
        binding.toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void initListeners() {
        binding.btnRegister.setOnClickListener(v -> attemptRegister());

        binding.tvLogin.setOnClickListener(v -> {
            openActivityWithTransition(LoginActivity.class);
            finish();
        });

        // Google sign-up — placeholder
        binding.btnGoogleSignup.setOnClickListener(v -> {
            // TODO: Implement Google Sign-Up using GoogleSignInClient
            showMessage(getString(R.string.google_sign_in_coming_soon));
        });

        // Phone sign-up navigation — placeholder
        binding.tvPhoneSignup.setOnClickListener(v -> {
            // TODO: Implement phone number authentication
            showMessage(getString(R.string.phone_sign_in_coming_soon));
        });

        // Submit on ime action on confirm-password field
        binding.etConfirmPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                attemptRegister();
                hideKeyboard();
                return true;
            }
            return false;
        });
    }

    // ==================================================================
    // Input Validation
    // ==================================================================

    private boolean validateInputs() {
        String name = binding.etName.getText() != null
                ? binding.etName.getText().toString().trim() : "";
        String email = binding.etEmail.getText() != null
                ? binding.etEmail.getText().toString().trim() : "";
        String password = binding.etPassword.getText() != null
                ? binding.etPassword.getText().toString().trim() : "";
        String confirmPassword = binding.etConfirmPassword.getText() != null
                ? binding.etConfirmPassword.getText().toString().trim() : "";

        // Reset all errors
        binding.tilName.setError(null);
        binding.tilEmail.setError(null);
        binding.tilPassword.setError(null);
        binding.tilConfirmPassword.setError(null);

        // Name validation
        if (TextUtils.isEmpty(name)) {
            binding.tilName.setError(getString(R.string.error_name_empty));
            binding.tilName.requestFocus();
            return false;
        }
        if (!ValidationUtils.isValidName(name)) {
            binding.tilName.setError(getString(R.string.error_name_invalid));
            binding.tilName.requestFocus();
            return false;
        }

        // Email validation
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

        // Password validation
        if (TextUtils.isEmpty(password)) {
            binding.tilPassword.setError(getString(R.string.error_password_empty));
            binding.tilPassword.requestFocus();
            return false;
        }
        if (!ValidationUtils.isValidPassword(password)) {
            binding.tilPassword.setError(getString(R.string.error_password_short));
            binding.tilPassword.requestFocus();
            return false;
        }

        // Confirm password
        if (TextUtils.isEmpty(confirmPassword)) {
            binding.tilConfirmPassword.setError(getString(R.string.error_confirm_password_empty));
            binding.tilConfirmPassword.requestFocus();
            return false;
        }
        if (!password.equals(confirmPassword)) {
            binding.tilConfirmPassword.setError(getString(R.string.error_password_mismatch));
            binding.tilConfirmPassword.requestFocus();
            return false;
        }

        // Terms checkbox
        if (!binding.cbTerms.isChecked()) {
            showMessage(getString(R.string.error_terms_required));
            return false;
        }

        return true;
    }

    // ==================================================================
    // Registration
    // ==================================================================

    /**
     * Validates inputs and creates a new Firebase Auth account.
     */
    private void attemptRegister() {
        if (!isNetworkAvailable()) {
            showError(getString(R.string.error_no_internet));
            return;
        }

        if (!validateInputs()) return;

        String name = binding.etName.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        setFormEnabled(false);
        showLoading();

        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = task.getResult().getUser();
                        if (firebaseUser != null) {
                            // Update display name
                            UserProfileChangeRequest profileUpdate = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(name)
                                    .build();

                            firebaseUser.updateProfile(profileUpdate)
                                    .addOnCompleteListener(profileTask -> {
                                        if (profileTask.isSuccessful()) {
                                            Log.d(TAG, "Display name updated");
                                        } else {
                                            Log.w(TAG, "Failed to update display name",
                                                    profileTask.getException());
                                        }
                                        // Proceed regardless — save user to database
                                        saveUserToDatabase(firebaseUser, name, email);
                                    });
                        } else {
                            hideLoading();
                            setFormEnabled(true);
                            showError(getString(R.string.error_generic));
                        }
                    } else {
                        hideLoading();
                        setFormEnabled(true);
                        handleRegisterError(task.getException());
                    }
                });
    }

    /**
     * Saves a comprehensive user record to the Realtime Database under
     * {@code users/{uid}}.
     *
     * @param firebaseUser the newly created Firebase user
     * @param name         the display name
     * @param email        the email address
     */
    private void saveUserToDatabase(FirebaseUser firebaseUser, String name, String email) {
        String uid = firebaseUser.getUid();
        long now = System.currentTimeMillis();

        Map<String, Object> userData = new HashMap<>();
        userData.put("uid", uid);
        userData.put("name", name);
        userData.put("email", email);
        userData.put("photoUrl", "");
        userData.put("coverUrl", "");
        userData.put("bio", "");
        userData.put("country", "");
        userData.put("city", "");
        userData.put("birthDate", "");
        userData.put("gender", "");
        userData.put("location", "");
        userData.put("website", "");
        userData.put("postCount", 0L);
        userData.put("followersCount", 0L);
        userData.put("followingCount", 0L);
        userData.put("likesCount", 0L);
        userData.put("viewsCount", 0L);
        userData.put("isVerified", false);
        userData.put("isPrivate", false);
        userData.put("isDisabled", false);
        userData.put("isOnline", true);
        userData.put("role", Constants.ROLE_USER);
        userData.put("createdAt", now);
        userData.put("updatedAt", now);
        userData.put("lastSeen", now);

        FirestoreHelper.getInstance().getUserRef(uid).setValue(userData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User record saved for uid=" + uid);
                    hideLoading();

                    // Send verification email
                    firebaseUser.sendEmailVerification()
                            .addOnCompleteListener(emailTask -> {
                                if (!emailTask.isSuccessful()) {
                                    Log.w(TAG, "Failed to send verification email",
                                            emailTask.getException());
                                }
                            });

                    // Navigate to email verification screen
                    openActivityWithTransition(EmailVerificationActivity.class);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save user record", e);
                    hideLoading();
                    setFormEnabled(true);
                    showError(getString(R.string.error_register_failed));
                });
    }

    // ==================================================================
    // Error Handling
    // ==================================================================

    /**
     * Translates FirebaseAuth registration exceptions into Arabic
     * error messages.
     *
     * @param exception the exception from the auth task
     */
    private void handleRegisterError(Exception exception) {
        if (exception == null) {
            showError(getString(R.string.error_generic));
            return;
        }

        Log.e(TAG, "Register error", exception);

        if (exception instanceof FirebaseAuthUserCollisionException) {
            showError(getString(R.string.error_email_already_exists));
        } else if (exception instanceof FirebaseAuthWeakPasswordException) {
            showError(getString(R.string.error_password_weak));
        } else {
            String msg = exception.getMessage();
            if (msg != null) {
                if (msg.contains("network")) {
                    showError(getString(R.string.error_no_internet));
                } else if (msg.contains("too many")) {
                    showError(getString(R.string.error_too_many_requests));
                } else {
                    showError(getString(R.string.error_register_failed));
                }
            } else {
                showError(getString(R.string.error_generic));
            }
        }
    }

    // ==================================================================
    // Helpers
    // ==================================================================

    /**
     * Enables or disables all form inputs during registration.
     */
    private void setFormEnabled(boolean enabled) {
        binding.etName.setEnabled(enabled);
        binding.etEmail.setEnabled(enabled);
        binding.etPassword.setEnabled(enabled);
        binding.etConfirmPassword.setEnabled(enabled);
        binding.cbTerms.setEnabled(enabled);
        binding.btnRegister.setEnabled(enabled);
        binding.btnGoogleSignup.setEnabled(enabled);
        binding.tvLogin.setEnabled(enabled);
        binding.tvPhoneSignup.setEnabled(enabled);

        if (enabled) {
            binding.btnRegister.setAlpha(1f);
            binding.btnGoogleSignup.setAlpha(1f);
        } else {
            binding.btnRegister.setAlpha(0.5f);
            binding.btnGoogleSignup.setAlpha(0.5f);
        }
    }
}
