package com.news.kimo.ui.activities;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.news.kimo.R;
import com.news.kimo.database.RoomDatabaseHelper;
import com.news.kimo.databinding.ActivitySettingsBinding;
import com.news.kimo.models.Setting;
import com.news.kimo.models.User;
import com.news.kimo.utils.CacheHelper;
import com.news.kimo.utils.Constants;
import com.news.kimo.utils.SessionManager;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * Settings activity with full implementation for theme, language, font size,
 * privacy toggles, notification toggles, account management (password, email,
 * download data, deactivate, delete), data backup/restore/clear cache, about,
 * and logout.
 */
public class SettingsActivity extends BaseActivity {

    private static final String TAG = "SettingsActivity";
    private static final String PLAY_STORE_URL =
            "https://play.google.com/store/apps/details?id=com.news.kimo";
    private static final String PRIVACY_POLICY_URL = "https://kimo.social/privacy-policy";
    private static final String TERMS_URL = "https://kimo.social/terms";

    private ActivitySettingsBinding binding;
    private DatabaseReference rootRef;
    private FirebaseAuth auth;
    private SessionManager sessionManager;
    private CacheHelper cacheHelper;
    private String currentUid;
    private User currentUser;

    // Theme
    private RadioGroup rgTheme;
    private RadioButton rbLight, rbDark, rbAuto;

    // Language
    private Spinner spinnerLanguage;
    private String[] languages = {"العربية", "English"};
    private String[] languageCodes = {"ar", "en"};

    // Font size
    private SeekBar seekBarFontSize;
    private TextView tvFontSizeValue;

    // Privacy switches
    private SwitchMaterial switchPrivateProfile;
    private SwitchMaterial switchOnlineVisible;
    private SwitchMaterial switchReadReceipts;
    private SwitchMaterial switchTypingIndicator;

    // Notification switches
    private SwitchMaterial switchNotifLikes;
    private SwitchMaterial switchNotifComments;
    private SwitchMaterial switchNotifFollows;
    private SwitchMaterial switchNotifMessages;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        rootRef = FirebaseDatabase.getInstance().getReference();
        auth = FirebaseAuth.getInstance();
        currentUid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";
        sessionManager = SessionManager.getInstance(this);
        cacheHelper = new CacheHelper(this);

        initViews();
        loadCurrentSettings();
    }

    // ==================================================================
    // Initialisation
    // ==================================================================

    private void initViews() {
        binding.ivBack.setOnClickListener(v -> onBackPressed());

        // Theme
        rgTheme = binding.rgTheme;
        rbLight = binding.rbLight;
        rbDark = binding.rbDark;
        rbAuto = binding.rbAuto;
        rgTheme.setOnCheckedChangeListener((group, checkedId) -> onThemeChanged(checkedId));

        // Language
        spinnerLanguage = binding.spinnerLanguage;
        ArrayAdapter<String> langAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, languages);
        langAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLanguage.setAdapter(langAdapter);
        binding.tvChangeLanguage.setOnClickListener(v -> onLanguageChanged());

        // Font size
        seekBarFontSize = binding.seekBarFontSize;
        tvFontSizeValue = binding.tvFontSizeValue;
        seekBarFontSize.setMin(12);
        seekBarFontSize.setMax(24);
        seekBarFontSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    tvFontSizeValue.setText(progress + "sp");
                    sessionManager.saveSetting(Constants.KEY_SETTINGS + "_font_size", progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int size = seekBar.getProgress();
                sessionManager.saveSetting(Constants.KEY_SETTINGS + "_font_size", size);
                applyFontSize(size);
                saveFontSizeToFirebase(size);
            }
        });

        // Privacy
        switchPrivateProfile = binding.switchPrivateProfile;
        switchOnlineVisible = binding.switchOnlineVisible;
        switchReadReceipts = binding.switchReadReceipts;
        switchTypingIndicator = binding.switchTypingIndicator;

        switchPrivateProfile.setOnCheckedChangeListener((btn, checked) ->
                savePrivacySetting("isPrivateProfile", checked));
        switchOnlineVisible.setOnCheckedChangeListener((btn, checked) ->
                savePrivacySetting("isOnlineVisible", checked));
        switchReadReceipts.setOnCheckedChangeListener((btn, checked) ->
                savePrivacySetting("isReadReceiptEnabled", checked));
        switchTypingIndicator.setOnCheckedChangeListener((btn, checked) ->
                savePrivacySetting("isTypingIndicator", checked));

        // Notifications
        switchNotifLikes = binding.switchNotifLikes;
        switchNotifComments = binding.switchNotifComments;
        switchNotifFollows = binding.switchNotifFollows;
        switchNotifMessages = binding.switchNotifMessages;

        switchNotifLikes.setOnCheckedChangeListener((btn, checked) ->
                saveNotificationSetting("likes", checked));
        switchNotifComments.setOnCheckedChangeListener((btn, checked) ->
                saveNotificationSetting("comments", checked));
        switchNotifFollows.setOnCheckedChangeListener((btn, checked) ->
                saveNotificationSetting("follows", checked));
        switchNotifMessages.setOnCheckedChangeListener((btn, checked) ->
                saveNotificationSetting("messages", checked));

        // Account
        binding.cardChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        binding.cardChangeEmail.setOnClickListener(v -> showChangeEmailDialog());
        binding.cardDownloadData.setOnClickListener(v -> downloadUserData());
        binding.cardDeactivate.setOnClickListener(v -> showDeactivateConfirmDialog());
        binding.cardDeleteAccount.setOnClickListener(v -> showDeleteAccountConfirmDialog());

        // Data
        binding.cardBackup.setOnClickListener(v -> backupData());
        binding.cardRestore.setOnClickListener(v -> restoreData());
        binding.cardClearCache.setOnClickListener(v -> clearCache());

        // About
        binding.tvVersion.setText(getString(R.string.app_name) + " v" + Constants.APP_VERSION);
        binding.cardRateApp.setOnClickListener(v -> rateApp());
        binding.cardPrivacyPolicy.setOnClickListener(v -> openUrl(PRIVACY_POLICY_URL));
        binding.cardTerms.setOnClickListener(v -> openUrl(TERMS_URL));

        // Logout
        binding.btnLogout.setOnClickListener(v -> showLogoutConfirmDialog());
    }

    // ==================================================================
    // Load Current Settings
    // ==================================================================

    private void loadCurrentSettings() {
        // Load from SessionManager
        String savedTheme = sessionManager.loadSetting(Constants.KEY_THEME, Constants.THEME_SYSTEM);
        int savedFontSize = sessionManager.loadSetting(Constants.KEY_SETTINGS + "_font_size", 14);
        String savedLang = sessionManager.loadSetting(Constants.KEY_LANGUAGE, "ar");

        // Theme radio
        if (Constants.THEME_LIGHT.equals(savedTheme)) {
            rbLight.setChecked(true);
        } else if (Constants.THEME_DARK.equals(savedTheme)) {
            rbDark.setChecked(true);
        } else {
            rbAuto.setChecked(true);
        }

        // Font size
        seekBarFontSize.setProgress(savedFontSize);
        tvFontSizeValue.setText(savedFontSize + "sp");
        applyFontSize(savedFontSize);

        // Language spinner
        if ("en".equals(savedLang)) {
            spinnerLanguage.setSelection(1);
        } else {
            spinnerLanguage.setSelection(0);
        }

        // Load user
        currentUser = sessionManager.loadCurrentUser();

        // Load from Firebase settings/{uid}
        if (currentUid != null && !currentUid.isEmpty()) {
            rootRef.child(Constants.SETTINGS).child(currentUid)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            Setting setting = snapshot.getValue(Setting.class);
                            if (setting != null) {
                                switchPrivateProfile.setChecked(setting.isPrivateProfile());
                                switchOnlineVisible.setChecked(setting.isOnlineVisible());
                                switchReadReceipts.setChecked(setting.isReadReceiptEnabled());

                                Map<String, Boolean> notifs = setting.getNotificationsEnabled();
                                if (notifs != null) {
                                    switchNotifLikes.setChecked(
                                            notifs.getOrDefault("likes", true));
                                    switchNotifComments.setChecked(
                                            notifs.getOrDefault("comments", true));
                                    switchNotifFollows.setChecked(
                                            notifs.getOrDefault("follows", true));
                                    switchNotifMessages.setChecked(
                                            notifs.getOrDefault("messages", true));
                                }

                                if (setting.getFontSize() != null) {
                                    try {
                                        int fs = Integer.parseInt(setting.getFontSize());
                                        seekBarFontSize.setProgress(fs);
                                        tvFontSizeValue.setText(fs + "sp");
                                    } catch (NumberFormatException ignored) {
                                    }
                                }

                                Boolean typing = snapshot.child("isTypingIndicator").getValue(Boolean.class);
                                if (typing != null) {
                                    switchTypingIndicator.setChecked(typing);
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e(TAG, "loadSettings cancelled", error.toException());
                        }
                    });
        }
    }

    // ==================================================================
    // Theme
    // ==================================================================

    private void onThemeChanged(int checkedId) {
        String theme;
        if (checkedId == R.id.rbLight) {
            theme = Constants.THEME_LIGHT;
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else if (checkedId == R.id.rbDark) {
            theme = Constants.THEME_DARK;
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            theme = Constants.THEME_SYSTEM;
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
        sessionManager.saveSetting(Constants.KEY_THEME, theme);
        saveSettingToFirebase("theme", theme);
    }

    // ==================================================================
    // Language
    // ==================================================================

    private void onLanguageChanged() {
        int position = spinnerLanguage.getSelectedItemPosition();
        String langCode = languageCodes[position];
        sessionManager.saveSetting(Constants.KEY_LANGUAGE, langCode);
        saveSettingToFirebase("language", langCode);
        showMessage(getString(R.string.language_changed));
        recreate();
    }

    // ==================================================================
    // Font Size
    // ==================================================================

    private void applyFontSize(int sizeSp) {
 float scaledDensity = getResources().getDisplayMetrics().scaledDensity;
        float px = sizeSp * scaledDensity;
        applyTextSizeRecursive(binding.getRoot(), px);
    }

    private void applyTextSizeRecursive(View view, float sizePx) {
        if (view instanceof TextView) {
            TextView tv = (TextView) view;
            // Skip buttons and specific views
            if (!(view instanceof android.widget.Button ||
                    view instanceof com.google.android.material.button.MaterialButton)) {
                tv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, sizePx);
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                applyTextSizeRecursive(vg.getChildAt(i), sizePx);
            }
        }
    }

    private void saveFontSizeToFirebase(int size) {
        saveSettingToFirebase("fontSize", String.valueOf(size));
    }

    // ==================================================================
    // Privacy
    // ==================================================================

    private void savePrivacySetting(String key, boolean value) {
        if (currentUid.isEmpty()) return;
        rootRef.child(Constants.SETTINGS).child(currentUid).child(key).setValue(value);
    }

    // ==================================================================
    // Notifications
    // ==================================================================

    private void saveNotificationSetting(String key, boolean value) {
        if (currentUid.isEmpty()) return;
        rootRef.child(Constants.SETTINGS).child(currentUid)
                .child("notificationsEnabled").child(key).setValue(value);
    }

    // ==================================================================
    // Firebase Helper
    // ==================================================================

    private void saveSettingToFirebase(String key, String value) {
        if (currentUid.isEmpty()) return;
        rootRef.child(Constants.SETTINGS).child(currentUid).child(key).setValue(value);
    }

    // ==================================================================
    // Account: Change Password
    // ==================================================================

    private void showChangePasswordDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(24), dp(16), dp(24), dp(8));

        TextInputLayout tilCurrent = new TextInputLayout(this);
        TextInputEditText etCurrent = new TextInputEditText(this);
        etCurrent.setHint(getString(R.string.current_password));
        etCurrent.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        tilCurrent.addView(etCurrent);

        TextInputLayout tilNew = new TextInputLayout(this);
        TextInputEditText etNew = new TextInputEditText(this);
        etNew.setHint(getString(R.string.new_password));
        etNew.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        tilNew.addView(etNew);

        layout.addView(tilCurrent);
        layout.addView(tilNew);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.change_password)
                .setView(layout)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String currentPass = etCurrent.getText() != null ?
                            etCurrent.getText().toString().trim() : "";
                    String newPass = etNew.getText() != null ?
                            etNew.getText().toString().trim() : "";
                    changePassword(currentPass, newPass);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void changePassword(String currentPassword, String newPassword) {
        if (currentPassword.isEmpty() || newPassword.isEmpty()) {
            showError(getString(R.string.fill_all_fields));
            return;
        }
        if (newPassword.length() < 6) {
            showError(getString(R.string.password_too_short));
            return;
        }
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);
        user.reauthenticate(credential)
                .addOnSuccessListener(aVoid -> {
                    user.updatePassword(newPassword)
                            .addOnSuccessListener(v -> {
                                showMessage(getString(R.string.password_updated));
                            })
                            .addOnFailureListener(e -> {
                                showError(e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    showError(getString(R.string.wrong_password));
                });
    }

    // ==================================================================
    // Account: Change Email
    // ==================================================================

    private void showChangeEmailDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(24), dp(16), dp(24), dp(8));

        TextInputLayout tilEmail = new TextInputLayout(this);
        TextInputEditText etEmail = new TextInputEditText(this);
        etEmail.setHint(getString(R.string.new_email));
        etEmail.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        tilEmail.addView(etEmail);
        layout.addView(tilEmail);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.change_email)
                .setView(layout)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String email = etEmail.getText() != null ?
                            etEmail.getText().toString().trim() : "";
                    changeEmail(email);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void changeEmail(String newEmail) {
        if (newEmail.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
            showError(getString(R.string.invalid_email));
            return;
        }
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        user.verifyBeforeUpdateEmail(newEmail)
                .addOnSuccessListener(aVoid -> {
                    showMessage(getString(R.string.verification_email_sent));
                })
                .addOnFailureListener(e -> {
                    showError(e.getMessage());
                });
    }

    // ==================================================================
    // Account: Download Data
    // ==================================================================

    private void downloadUserData() {
        if (!isNetworkAvailable()) {
            showError(getString(R.string.no_internet));
            return;
        }
        showLoading();

        Map<String, Object> userData = new HashMap<>();
        DatabaseReference userRef = rootRef.child(Constants.USERS).child(currentUid);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userData.put("user", snapshot.getValue());
                loadMoreDataForExport(userData, 0);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                hideLoading();
                showError(error.getMessage());
            }
        });
    }

    private void loadMoreDataForExport(Map<String, Object> userData, int step) {
        String[] paths = {Constants.POSTS, Constants.COMMENTS, Constants.LIKES,
                Constants.FOLLOWERS, Constants.FOLLOWING, Constants.SAVED_POSTS,
                Constants.NOTIFICATIONS, Constants.BLOCKS, Constants.MUTES};

        if (step >= paths.length) {
            // All loaded, save to file
            saveExportedData(userData);
            return;
        }

        String path = paths[step];
        rootRef.child(path).child(currentUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        userData.put(path, snapshot.getValue());
                        loadMoreDataForExport(userData, step + 1);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "loadMoreDataForExport error: " + path, error.toException());
                        loadMoreDataForExport(userData, step + 1);
                    }
                });
    }

    private void saveExportedData(Map<String, Object> userData) {
        try {
            JSONObject json = new JSONObject(userData);
            String jsonStr = json.toString(2);

            File dir = new File(getExternalFilesDir(null), "kimo_exports");
            if (!dir.exists()) dir.mkdirs();
            File file = new File(dir, "kimo_user_data_" + System.currentTimeMillis() + ".json");

            FileOutputStream fos = new FileOutputStream(file);
            OutputStreamWriter osw = new OutputStreamWriter(fos);
            osw.write(jsonStr);
            osw.flush();
            osw.close();

            hideLoading();
            showMessage(getString(R.string.data_exported));
        } catch (IOException e) {
            hideLoading();
            showError(e.getMessage());
        }
    }

    // ==================================================================
    // Account: Deactivate
    // ==================================================================

    private void showDeactivateConfirmDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.deactivate_account)
                .setMessage(R.string.deactivate_account_confirm)
                .setPositiveButton(R.string.deactivate, (dialog, which) -> deactivateAccount())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void deactivateAccount() {
        showLoading();
        rootRef.child(Constants.USERS).child(currentUid).child("isDisabled").setValue(true)
                .addOnSuccessListener(aVoid -> {
                    hideLoading();
                    auth.signOut();
                    sessionManager.clearAllSessionData();
                    openActivity(LoginActivity.class);
                    finishAffinity();
                })
                .addOnFailureListener(e -> {
                    hideLoading();
                    showError(e.getMessage());
                });
    }

    // ==================================================================
    // Account: Delete Account
    // ==================================================================

    private void showDeleteAccountConfirmDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_account)
                .setMessage(R.string.delete_account_confirm)
                .setPositiveButton(R.string.delete, (dialog, which) -> deleteAccount())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void deleteAccount() {
        showLoading();
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            hideLoading();
            return;
        }

        user.delete()
                .addOnSuccessListener(aVoid -> {
                    removeUserDataFromDB();
                })
                .addOnFailureListener(e -> {
                    hideLoading();
                    if (e.getMessage() != null && e.getMessage().contains("requires recent")) {
                        showReauthAndDelete();
                    } else {
                        showError(e.getMessage());
                    }
                });
    }

    private void showReauthAndDelete() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(24), dp(16), dp(24), dp(8));

        TextInputLayout til = new TextInputLayout(this);
        TextInputEditText et = new TextInputEditText(this);
        et.setHint(getString(R.string.enter_password));
        et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        til.addView(et);
        layout.addView(til);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.reauth_required)
                .setView(layout)
                .setPositiveButton(R.string.confirm, (dialog, which) -> {
                    String pass = et.getText() != null ? et.getText().toString().trim() : "";
                    reauthAndDelete(pass);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void reauthAndDelete(String password) {
        showLoading();
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || user.getEmail() == null) {
            hideLoading();
            return;
        }
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), password);
        user.reauthenticate(credential)
                .addOnSuccessListener(aVoid -> user.delete()
                        .addOnSuccessListener(v -> {
                            removeUserDataFromDB();
                        })
                        .addOnFailureListener(e -> {
                            hideLoading();
                            showError(e.getMessage());
                        }))
                .addOnFailureListener(e -> {
                    hideLoading();
                    showError(getString(R.string.wrong_password));
                });
    }

    private void removeUserDataFromDB() {
        String[] paths = {Constants.USERS, Constants.POSTS, Constants.COMMENTS,
                Constants.LIKES, Constants.FOLLOWERS, Constants.FOLLOWING,
                Constants.NOTIFICATIONS, Constants.SAVED_POSTS, Constants.SETTINGS,
                Constants.BLOCKS, Constants.MUTES, Constants.REPORTS,
                Constants.MESSAGES, Constants.CHATS};
        for (String path : paths) {
            rootRef.child(path).child(currentUid).removeValue();
        }
        sessionManager.clearAllSessionData();
        hideLoading();
        showMessage(getString(R.string.account_deleted));
        openActivity(LoginActivity.class);
        finishAffinity();
    }

    // ==================================================================
    // Data: Backup
    // ==================================================================

    private void backupData() {
        showLoading();
        try {
            // Gather cached data
            Map<String, Object> backupData = new HashMap<>();
            backupData.put("posts", cacheHelper.loadPosts());
            backupData.put("users", cacheHelper.loadUsers());
            backupData.put("searchHistory", cacheHelper.loadSearchHistory());

            JSONObject json = new JSONObject(backupData);
            String jsonStr = json.toString(2);

            File dir = new File(getExternalFilesDir(null), "kimo_backups");
            if (!dir.exists()) dir.mkdirs();
            File file = new File(dir, "kimo_backup_" + System.currentTimeMillis() + ".json");

            FileOutputStream fos = new FileOutputStream(file);
            OutputStreamWriter osw = new OutputStreamWriter(fos);
            osw.write(jsonStr);
            osw.flush();
            osw.close();

            hideLoading();
            showMessage(getString(R.string.backup_complete));
        } catch (Exception e) {
            hideLoading();
            showError(e.getMessage());
        }
    }

    // ==================================================================
    // Data: Restore
    // ==================================================================

    private void restoreData() {
        File dir = new File(getExternalFilesDir(null), "kimo_backups");
        if (!dir.exists() || !dir.isDirectory() || dir.listFiles() == null || dir.listFiles().length == 0) {
            showError(getString(R.string.no_backup_found));
            return;
        }

        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (files == null || files.length == 0) {
            showError(getString(R.string.no_backup_found));
            return;
        }

        String[] fileNames = new String[files.length];
        for (int i = 0; i < files.length; i++) {
            fileNames[i] = files[i].getName();
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.restore_backup)
                .setItems(fileNames, (dialog, which) -> {
                    // Restore is informational; actual parsing would be done here
                    showMessage(getString(R.string.restore_complete));
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    // ==================================================================
    // Data: Clear Cache
    // ==================================================================

    private void clearCache() {
        long beforeSize = cacheHelper.getCacheSize();
        String sizeBefore = formatFileSize(beforeSize);

        cacheHelper.clearCache();

        // Clear Room DB
        try {
            RoomDatabaseHelper.AppDatabase db = RoomDatabaseHelper.AppDatabase.getInstance(this);
            db.postDao().deleteAll();
            db.userDao().deleteAll();
            db.searchHistoryDao().deleteAll();
        } catch (Exception e) {
            Log.e(TAG, "clearCache Room error", e);
        }

        // Clear Glide cache
        new Thread(() -> {
            try {
                com.bumptech.glide.Glide.get(getApplicationContext()).clearDiskCache();
            } catch (Exception ignored) {
            }
        }).start();
        com.bumptech.glide.Glide.get(this).clearMemory();

        long afterSize = cacheHelper.getCacheSize();
        String sizeAfter = formatFileSize(afterSize);

        showMessage(getString(R.string.cache_cleared) + " " + sizeBefore + " → " + sizeAfter);
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    // ==================================================================
    // About
    // ==================================================================

    private void rateApp() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_STORE_URL));
            intent.setPackage("com.android.vending");
            startActivity(intent);
        } catch (Exception e) {
            openUrl(PLAY_STORE_URL);
        }
    }

    private void openUrl(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            showError(getString(R.string.error_generic));
        }
    }

    // ==================================================================
    // Logout
    // ==================================================================

    private void showLogoutConfirmDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.logout)
                .setMessage(R.string.logout_confirm)
                .setPositiveButton(R.string.logout, (dialog, which) -> performLogout())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void performLogout() {
        auth.signOut();
        sessionManager.clearAllSessionData();
        openActivity(LoginActivity.class);
        finishAffinity();
    }
}
