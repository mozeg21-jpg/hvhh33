package com.news.kimo.ui.activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.news.kimo.R;
import com.news.kimo.adapters.SocialLinkAdapter;
import com.news.kimo.database.FirestoreHelper;
import com.news.kimo.databinding.ActivityEditProfileBinding;
import com.news.kimo.models.User;
import com.news.kimo.utils.Constants;
import com.news.kimo.utils.SessionManager;
import com.news.kimo.utils.ValidationUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EditProfileActivity extends BaseActivity {

    private ActivityEditProfileBinding binding;
    private FirestoreHelper db;
    private FirebaseAuth mAuth;
    private SessionManager sessionManager;

    private String currentUid;
    private User currentUser;

    private Uri avatarUri = null;
    private Uri coverUri = null;
    private boolean avatarChanged = false;
    private boolean coverChanged = false;

    private final List<Map<String, String>> socialLinks = new ArrayList<>();
    private SocialLinkAdapter socialLinkAdapter;

    private static final int MAX_BIO_LENGTH = 160;
    private static final String[] GENDERS = {"ذكر", "أنثى", "آخر"};

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupStatusBar();

        db = FirestoreHelper.getInstance();
        mAuth = FirebaseAuth.getInstance();
        sessionManager = SessionManager.getInstance(this);

        if (mAuth.getCurrentUser() == null) {
            finish();
            return;
        }
        currentUid = mAuth.getCurrentUser().getUid();

        setupToolbar();
        loadCurrentUser();
        setupAvatarPicker();
        setupCoverPicker();
        setupBioCounter();
        setupGenderDropdown();
        setupBirthDate();
        setupSocialLinks();
        setupSaveButton();
    }

    // ==================================================================
    // Toolbar
    // ==================================================================

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.edit_profile);
        }
        binding.toolbar.setNavigationOnClickListener(v -> {
            hideKeyboard();
            finish();
        });
    }

    // ==================================================================
    // Load Current User
    // ==================================================================

    private void loadCurrentUser() {
        showLoading();
        currentUser = sessionManager.loadCurrentUser();

        db.getUserRef(currentUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                hideLoading();
                User user = snapshot.getValue(User.class);
                if (user == null) {
                    showError("لم يتم العثور على بيانات المستخدم");
                    return;
                }
                currentUser = user;
                populateForm(user);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                hideLoading();
                showError(error.getMessage());
            }
        });
    }

    private void populateForm(User user) {
        // Avatar
        loadProfileImage(user.getPhotoUrl(), binding.ivAvatar, user.getName());

        // Cover
        if (user.getCoverUrl() != null && !user.getCoverUrl().isEmpty()) {
            loadImage(user.getCoverUrl(), binding.ivCoverPhoto);
        }

        // Name
        binding.etName.setText(user.getName() != null ? user.getName() : "");

        // Bio
        binding.etBio.setText(user.getBio() != null ? user.getBio() : "");
        int bioLen = user.getBio() != null ? user.getBio().length() : 0;
        binding.tvBioCounter.setText(bioLen + "/" + MAX_BIO_LENGTH);

        // Country
        binding.etCountry.setText(user.getCountry() != null ? user.getCountry() : "");

        // City
        binding.etCity.setText(user.getCity() != null ? user.getCity() : "");

        // Birth date
        if (user.getBirthDate() != null && !user.getBirthDate().isEmpty()) {
            binding.tvBirthDate.setText(user.getBirthDate());
        }

        // Gender
        if (user.getGender() != null && !user.getGender().isEmpty()) {
            for (int i = 0; i < GENDERS.length; i++) {
                if (GENDERS[i].equals(user.getGender())) {
                    binding.actvGender.setText(GENDERS[i], false);
                    break;
                }
            }
        }

        // Website
        binding.etWebsite.setText(user.getWebsite() != null ? user.getWebsite() : "");

        // Social links
        if (user.getSocialLinks() != null && !user.getSocialLinks().isEmpty()) {
            socialLinks.clear();
            for (Map.Entry<String, String> entry : user.getSocialLinks().entrySet()) {
                Map<String, String> link = new HashMap<>();
                link.put("platform", entry.getKey());
                link.put("url", entry.getValue());
                socialLinks.add(link);
            }
            socialLinkAdapter.notifyDataSetChanged();
        }

        // Switches
        binding.switchPrivate.setChecked(user.isPrivate());
        binding.switchOnlineVisible.setChecked(user.isOnline());
    }

    // ==================================================================
    // Avatar Picker
    // ==================================================================

    private void setupAvatarPicker() {
        binding.ivAvatar.setLayoutParams(new LinearLayout.LayoutParams(dp(100), dp(100)));

        binding.fabCameraAvatar.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(Intent.createChooser(intent, "اختر صورة الملف الشخصي"),
                    Constants.REQUEST_CODE_GALLERY);
        });
    }

    // ==================================================================
    // Cover Picker
    // ==================================================================

    private void setupCoverPicker() {
        binding.ivCoverPhoto.getLayoutParams().height = dp(160);

        binding.fabCameraCover.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(Intent.createChooser(intent, "اختر صورة الغلاف"),
                    Constants.REQUEST_CODE_CROP_IMAGE);
        });
    }

    // ==================================================================
    // Bio Counter
    // ==================================================================

    private void setupBioCounter() {
        binding.etBio.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int len = s.length();
                binding.tvBioCounter.setText(len + "/" + MAX_BIO_LENGTH);
                if (len > MAX_BIO_LENGTH) {
                    binding.tvBioCounter.setTextColor(getColor(R.color.colorError));
                } else {
                    binding.tvBioCounter.setTextColor(getColor(R.color.colorTextSecondary));
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    // ==================================================================
    // Gender Dropdown
    // ==================================================================

    private void setupGenderDropdown() {
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, GENDERS);
        binding.actvGender.setAdapter(genderAdapter);
    }

    // ==================================================================
    // Birth Date
    // ==================================================================

    private void setupBirthDate() {
        binding.tvBirthDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance(Locale.getDefault());
            int year = cal.get(Calendar.YEAR);
            int month = cal.get(Calendar.MONTH);
            int day = cal.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePicker = new DatePickerDialog(this, (view, selectedYear, selectedMonth, selectedDay) -> {
                String date = String.format(Locale.getDefault(), "%04d-%02d-%02d",
                        selectedYear, selectedMonth + 1, selectedDay);
                binding.tvBirthDate.setText(date);
            }, year - 18, month, day);
            datePicker.getDatePicker().setMaxDate(System.currentTimeMillis());
            datePicker.show();
        });
    }

    // ==================================================================
    // Social Links
    // ==================================================================

    private void setupSocialLinks() {
        binding.rvSocialLinks.setLayoutManager(new LinearLayoutManager(this));
        socialLinkAdapter = new SocialLinkAdapter(this, socialLinks, new SocialLinkAdapter.OnSocialLinkActionListener() {
            @Override
            public void onRemove(int position) {
                socialLinks.remove(position);
                socialLinkAdapter.notifyItemRemoved(position);
            }

            @Override
            public void onUpdate(int position, String platform, String url) {
                Map<String, String> link = socialLinks.get(position);
                link.put("platform", platform);
                link.put("url", url);
            }
        });
        binding.rvSocialLinks.setAdapter(socialLinkAdapter);

        binding.btnAddSocialLink.setOnClickListener(v -> {
            Map<String, String> newLink = new HashMap<>();
            newLink.put("platform", "");
            newLink.put("url", "");
            socialLinks.add(newLink);
            socialLinkAdapter.notifyItemInserted(socialLinks.size() - 1);
            binding.rvSocialLinks.scrollToPosition(socialLinks.size() - 1);
        });
    }

    // ==================================================================
    // Save
    // ==================================================================

    private void setupSaveButton() {
        binding.btnSave.setOnClickListener(v -> saveProfile());
    }

    private void saveProfile() {
        hideKeyboard();

        // Validate
        String name = binding.etName.getText().toString().trim();
        if (name.isEmpty()) {
            showError("الاسم مطلوب");
            binding.etName.setError("أدخل اسمك");
            return;
        }

        String bio = binding.etBio.getText().toString().trim();
        if (bio.length() > MAX_BIO_LENGTH) {
            showError("تجاوزت الحد الأقصى للنبذة");
            return;
        }

        if (!isNetworkAvailable()) {
            showError("لا يوجد اتصال بالإنترنت");
            return;
        }

        showLoading();

        if (avatarChanged || coverChanged) {
            uploadImagesAndSave(name, bio);
        } else {
            saveToDatabase(name, bio, null, null);
        }
    }

    private void uploadImagesAndSave(String name, String bio) {
        // Upload avatar if changed
        if (avatarChanged && avatarUri != null) {
            StorageReference avatarRef = FirebaseStorage.getInstance().getReference()
                    .child(Constants.MEDIA).child(currentUid).child("avatar_" + System.currentTimeMillis() + ".jpg");

            avatarRef.putFile(avatarUri)
                    .continueWithTask(task -> {
                        if (!task.isSuccessful()) throw task.getException();
                        return avatarRef.getDownloadUrl();
                    })
                    .addOnSuccessListener(avatarUrl -> {
                        if (coverChanged && coverUri != null) {
                            uploadCover(name, bio, avatarUrl.toString());
                        } else {
                            saveToDatabase(name, bio, avatarUrl.toString(), null);
                        }
                    })
                    .addOnFailureListener(e -> {
                        hideLoading();
                        showError("فشل رفع صورة الملف الشخصي");
                    });
        } else if (coverChanged && coverUri != null) {
            uploadCover(name, bio, null);
        }
    }

    private void uploadCover(String name, String bio, String avatarUrl) {
        StorageReference coverRef = FirebaseStorage.getInstance().getReference()
                .child(Constants.MEDIA).child(currentUid).child("cover_" + System.currentTimeMillis() + ".jpg");

        coverRef.putFile(coverUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    return coverRef.getDownloadUrl();
                })
                .addOnSuccessListener(coverUrl -> {
                    saveToDatabase(name, bio, avatarUrl, coverUrl.toString());
                })
                .addOnFailureListener(e -> {
                    hideLoading();
                    showError("فشل رفع صورة الغلاف");
                });
    }

    private void saveToDatabase(String name, String bio, String newAvatarUrl, String newCoverUrl) {
        String photoUrl = newAvatarUrl != null ? newAvatarUrl :
                (currentUser != null ? currentUser.getPhotoUrl() : "");
        String coverUrl = newCoverUrl != null ? newCoverUrl :
                (currentUser != null ? currentUser.getCoverUrl() : "");

        String country = binding.etCountry.getText().toString().trim();
        String city = binding.etCity.getText().toString().trim();
        String birthDate = binding.tvBirthDate.getText().toString().trim();
        String gender = binding.actvGender.getText().toString().trim();
        String website = binding.etWebsite.getText().toString().trim();
        boolean isPrivate = binding.switchPrivate.isChecked();
        boolean isOnlineVisible = binding.switchOnlineVisible.isChecked();

        // Build social links map
        Map<String, String> socialLinksMap = new HashMap<>();
        for (Map<String, String> link : socialLinks) {
            String platform = link.get("platform");
            String url = link.get("url");
            if (platform != null && !platform.trim().isEmpty() &&
                    url != null && !url.trim().isEmpty()) {
                socialLinksMap.put(platform.trim(), url.trim());
            }
        }

        // Build user update map
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("photoUrl", photoUrl);
        updates.put("coverUrl", coverUrl);
        updates.put("bio", bio);
        updates.put("country", country);
        updates.put("city", city);
        updates.put("birthDate", birthDate);
        updates.put("gender", gender);
        updates.put("website", website);
        updates.put("socialLinks", socialLinksMap);
        updates.put("isPrivate", isPrivate);
        updates.put("updatedAt", System.currentTimeMillis());

        // Update Firebase Auth display name
        if (mAuth.getCurrentUser() != null && !name.equals(mAuth.getCurrentUser().getDisplayName())) {
            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build();
            mAuth.getCurrentUser().updateProfile(profileUpdates);
        }

        // Save to users/{uid}
        db.getUserRef(currentUid).updateChildren(updates)
                .addOnSuccessListener(v -> {
                    // Update all existing posts with new name and photo
                    updateExistingPosts(name, photoUrl);

                    // Update session
                    if (currentUser != null) {
                        currentUser.setName(name);
                        currentUser.setPhotoUrl(photoUrl);
                        currentUser.setCoverUrl(coverUrl);
                        currentUser.setBio(bio);
                        currentUser.setCountry(country);
                        currentUser.setCity(city);
                        currentUser.setBirthDate(birthDate);
                        currentUser.setGender(gender);
                        currentUser.setWebsite(website);
                        currentUser.setSocialLinks(socialLinksMap);
                        currentUser.setPrivate(isPrivate);
                        currentUser.setUpdatedAt(System.currentTimeMillis());
                        sessionManager.saveCurrentUser(currentUser);
                    }

                    hideLoading();
                    showMessage("تم تحديث الملف الشخصي بنجاح");
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e -> {
                    hideLoading();
                    showError("فشل تحديث الملف الشخصي");
                });
    }

    private void updateExistingPosts(String newName, String newPhotoUrl) {
        Query postsQuery = db.getReference(Constants.POSTS).orderByChild("uid").equalTo(currentUid);
        postsQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, Object> postUpdates = new HashMap<>();
                postUpdates.put("userName", newName);
                postUpdates.put("userPhoto", newPhotoUrl);

                for (DataSnapshot postSnap : snapshot.getChildren()) {
                    postSnap.getRef().updateChildren(postUpdates);
                }

                // Also update comments
                updateExistingComments(newName, newPhotoUrl);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateExistingComments(String newName, String newPhotoUrl) {
        db.getReference(Constants.COMMENTS).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, Object> commentUpdates = new HashMap<>();
                commentUpdates.put("userName", newName);
                commentUpdates.put("userPhoto", newPhotoUrl);

                for (DataSnapshot postComments : snapshot.getChildren()) {
                    for (DataSnapshot commentSnap : postComments.getChildren()) {
                        String uid = commentSnap.child("uid").getValue(String.class);
                        if (currentUid.equals(uid)) {
                            commentSnap.getRef().updateChildren(commentUpdates);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // ==================================================================
    // Activity Result
    // ==================================================================

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) return;

        if (requestCode == Constants.REQUEST_CODE_GALLERY) {
            Uri uri = data.getData();
            if (uri != null) {
                avatarUri = uri;
                avatarChanged = true;
                binding.ivAvatar.setImageURI(uri);
            }
        } else if (requestCode == Constants.REQUEST_CODE_CROP_IMAGE) {
            Uri uri = data.getData();
            if (uri != null) {
                coverUri = uri;
                coverChanged = true;
                binding.ivCoverPhoto.setImageURI(uri);
            }
        }
    }

    // ==================================================================
    // Lifecycle
    // ==================================================================

    @Override
    public void onBackPressed() {
        hideKeyboard();
        super.onBackPressed();
    }
}
