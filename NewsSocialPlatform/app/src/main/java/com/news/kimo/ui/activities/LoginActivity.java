package com.news.kimo.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.news.kimo.R;
import com.news.kimo.database.FirestoreHelper;
import com.news.kimo.databinding.ActivityLoginBinding;
import com.news.kimo.models.User;
import com.news.kimo.utils.Constants;
import com.news.kimo.utils.SessionManager;
import com.news.kimo.utils.ValidationUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Login screen activity.
 * <p>
 * Supports email / password authentication, Google Sign-In (placeholder),
 * and anonymous (guest) authentication. On successful login the user
 * record is fetched from the database, saved to {@link SessionManager},
 * online status is updated, and the user is navigated to
 * {@link MainActivity}.
 */
public class LoginActivity extends BaseActivity {

    private static final String TAG = "LoginActivity";

    private ActivityLoginBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initViews();
        initListeners();
    }

    // ==================================================================
    // Initialisation
    // ==================================================================

    private void initViews() {
        binding.toolbar.setTitle(R.string.login_title);
        binding.toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void initListeners() {
        binding.btnLogin.setOnClickListener(v -> attemptLogin());

        binding.tvRegister.setOnClickListener(v ->
                openActivityWithTransition(RegisterActivity.class)
        );

        binding.tvForgotPassword.setOnClickListener(v ->
                openActivityWithTransition(ForgotPasswordActivity.class)
        );

        // Google Sign-In button — placeholder, to be implemented with GoogleSignInClient
        binding.btnGoogleLogin.setOnClickListener(v -> {
            // TODO: Implement Google Sign-In using GoogleSignInClient
            // GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            //         .requestIdToken(getString(R.string.default_web_client_id))
            //         .requestEmail()
            //         .build();
            // GoogleSignInClient client = GoogleSignIn.getClient(this, gso);
            // startActivityForResult(client.getSignInIntent(), RC_GOOGLE_SIGN_IN);
            showMessage(getString(R.string.google_sign_in_coming_soon));
        });

        // Guest / anonymous login
        binding.btnGuestLogin.setOnClickListener(v -> attemptGuestLogin());

        // Password field: submit on ime action
        binding.etPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                attemptLogin();
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
        String email = binding.etEmail.getText() != null
                ? binding.etEmail.getText().toString().trim() : "";
        String password = binding.etPassword.getText() != null
                ? binding.etPassword.getText().toString().trim() : "";

        // Reset errors
        binding.tilEmail.setError(null);
        binding.tilPassword.setError(null);

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

        return true;
    }

    // ==================================================================
    // Login
    // ==================================================================

    /**
     * Attempts to sign in with the provided email and password.
     * Shows a loading dialog and handles FirebaseAuth errors with
     * Arabic messages.
     */
    private void attemptLogin() {
        if (!isNetworkAvailable()) {
            showError(getString(R.string.error_no_internet));
            return;
        }

        if (!validateInputs()) return;

        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        setFormEnabled(false);
        showLoading();

        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    hideLoading();
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = task.getResult().getUser();
                        if (firebaseUser != null) {
                            onLoginSuccess(firebaseUser);
                        } else {
                            setFormEnabled(true);
                            showError(getString(R.string.error_generic));
                        }
                    } else {
                        setFormEnabled(true);
                        handleLoginError(task.getException());
                    }
                });
    }

    /**
     * Attempts anonymous (guest) authentication.
     */
    private void attemptGuestLogin() {
        if (!isNetworkAvailable()) {
            showError(getString(R.string.error_no_internet));
            return;
        }

        showLoading();

        FirebaseAuth.getInstance().signInAnonymously()
                .addOnCompleteListener(task -> {
                    hideLoading();
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = task.getResult().getUser();
                        if (firebaseUser != null) {
                            onGuestLoginSuccess(firebaseUser);
                        } else {
                            showError(getString(R.string.error_generic));
                        }
                    } else {
                        handleLoginError(task.getException());
                    }
                });
    }

    /**
     * Called after a successful email/password login.
     * Fetches the user record from the database, saves the session,
     * updates online status, and navigates to the main screen.
     *
     * @param firebaseUser the authenticated Firebase user
     */
    private void onLoginSuccess(FirebaseUser firebaseUser) {
        String uid = firebaseUser.getUid();

        // Check if the user record exists in the database
        FirestoreHelper.getInstance().getUserRef(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        User user;
                        if (snapshot.exists()) {
                            user = snapshot.getValue(User.class);
                            if (user != null) {
                                user.setUid(uid);
                            }
                        } else {
                            // User exists in Auth but not in DB — create a minimal record
                            user = new User();
                            user.setUid(uid);
                            user.setName(firebaseUser.getDisplayName() != null
                                    ? firebaseUser.getDisplayName() : "مستخدم");
                            user.setEmail(firebaseUser.getEmail() != null
                                    ? firebaseUser.getEmail() : "");
                            user.setPhotoUrl(firebaseUser.getPhotoUrl() != null
                                    ? firebaseUser.getPhotoUrl().toString() : "");
                            user.setRole(Constants.ROLE_USER);
                            user.setCreatedAt(System.currentTimeMillis());
                            user.setVerified(false);
                            user.setPrivate(false);
                            user.setDisabled(false);
                            saveUserToDatabase(uid, user);
                        }

                        if (user != null) {
                            saveSessionAndNavigate(user);
                        } else {
                            setFormEnabled(true);
                            showError(getString(R.string.error_generic));
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        setFormEnabled(true);
                        Log.e(TAG, "Failed to fetch user data", error.toException());
                        showError(getString(R.string.error_fetch_user_failed));
                    }
                });
    }

    /**
     * Called after a successful anonymous (guest) login.
     * Creates a minimal guest user record and navigates to main.
     *
     * @param firebaseUser the anonymous Firebase user
     */
    private void onGuestLoginSuccess(FirebaseUser firebaseUser) {
        String uid = firebaseUser.getUid();

        User guestUser = new User();
        guestUser.setUid(uid);
        guestUser.setName(getString(R.string.guest_user_name));
        guestUser.setEmail("");
        guestUser.setPhotoUrl("");
        guestUser.setRole(Constants.ROLE_USER);
        guestUser.setCreatedAt(System.currentTimeMillis());
        guestUser.setVerified(false);
        guestUser.setPrivate(false);
        guestUser.setDisabled(false);

        // Save guest record to database
        saveUserToDatabase(uid, guestUser);
        saveSessionAndNavigate(guestUser);
    }

    /**
     * Saves a new user record to the Realtime Database.
     */
    private void saveUserToDatabase(String uid, User user) {
        try {
            Map<String, Object> userData = new HashMap<>();
            userData.put("uid", user.getUid());
            userData.put("name", user.getName());
            userData.put("email", user.getEmail());
            userData.put("photoUrl", user.getPhotoUrl() != null ? user.getPhotoUrl() : "");
            userData.put("role", user.getRole() != null ? user.getRole() : Constants.ROLE_USER);
            userData.put("createdAt", user.getCreatedAt());
            userData.put("isVerified", user.isVerified());
            userData.put("isPrivate", user.isPrivate());
            userData.put("isDisabled", user.isDisabled());
            userData.put("postCount", 0L);
            userData.put("followersCount", 0L);
            userData.put("followingCount", 0L);
            userData.put("isOnline", true);
            userData.put("lastSeen", System.currentTimeMillis());

            FirestoreHelper.getInstance().getUserRef(uid).setValue(userData)
                    .addOnSuccessListener(aVoid ->
                            Log.d(TAG, "User record saved for uid=" + uid))
                    .addOnFailureListener(e ->
                            Log.e(TAG, "Failed to save user record", e));
        } catch (Exception e) {
            Log.e(TAG, "saveUserToDatabase failed", e);
        }
    }

    /**
     * Saves the user to session, updates online status, and navigates to
     * {@link MainActivity}.
     *
     * @param user the user to persist in the session
     */
    private void saveSessionAndNavigate(User user) {
        // Save user session
        SessionManager.getInstance(this).saveCurrentUser(user);

        // Update online status in database
        updateUserOnlineStatus(user.getUid(), true);

        // Navigate to main
        openActivityWithTransition(MainActivity.class);
        finish();
    }

    /**
     * Updates the user's online status in Firebase Realtime Database.
     */
    private void updateUserOnlineStatus(String uid, boolean online) {
        try {
            Map<String, Object> updates = new HashMap<>();
            updates.put("isOnline", online);
            updates.put("lastSeen", System.currentTimeMillis());
            FirestoreHelper.getInstance().getUserRef(uid).updateChildren(updates)
                    .addOnFailureListener(e ->
                            Log.w(TAG, "Failed to update online status", e));
        } catch (Exception e) {
            Log.w(TAG, "updateUserOnlineStatus failed", e);
        }
    }

    // ==================================================================
    // Error Handling
    // ==================================================================

    /**
     * Translates FirebaseAuth exceptions into user-friendly Arabic
     * error messages.
     *
     * @param exception the exception from the auth task
     */
    private void handleLoginError(Exception exception) {
        if (exception == null) {
            showError(getString(R.string.error_generic));
            return;
        }

        Log.e(TAG, "Login error", exception);

        if (exception instanceof FirebaseAuthInvalidUserException) {
            showError(getString(R.string.error_user_not_found));
        } else if (exception instanceof FirebaseAuthInvalidCredentialsException) {
            showError(getString(R.string.error_wrong_password));
        } else {
            String msg = exception.getMessage();
            if (msg != null) {
                if (msg.contains("network")) {
                    showError(getString(R.string.error_no_internet));
                } else if (msg.contains("too many")) {
                    showError(getString(R.string.error_too_many_requests));
                } else {
                    showError(getString(R.string.error_login_failed));
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
     * Enables or disables all form inputs during login processing.
     *
     * @param enabled {@code true} to enable, {@code false} to disable
     */
    private void setFormEnabled(boolean enabled) {
        binding.etEmail.setEnabled(enabled);
        binding.etPassword.setEnabled(enabled);
        binding.btnLogin.setEnabled(enabled);
        binding.btnGoogleLogin.setEnabled(enabled);
        binding.btnGuestLogin.setEnabled(enabled);
        binding.tvForgotPassword.setEnabled(enabled);
        binding.tvRegister.setEnabled(enabled);

        if (enabled) {
            binding.btnLogin.setAlpha(1f);
            binding.btnGoogleLogin.setAlpha(1f);
            binding.btnGuestLogin.setAlpha(1f);
        } else {
            binding.btnLogin.setAlpha(0.5f);
            binding.btnGoogleLogin.setAlpha(0.5f);
            binding.btnGuestLogin.setAlpha(0.5f);
        }
    }

    @Override
    public void onBackPressed() {
        // Prevent going back from login — exit the app
        if (isTaskRoot()) {
            finishAffinity();
        } else {
            super.onBackPressed();
        }
    }
}
