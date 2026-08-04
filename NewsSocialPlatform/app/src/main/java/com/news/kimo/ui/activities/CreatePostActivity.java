package com.news.kimo.ui.activities;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
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
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.news.kimo.R;
import com.news.kimo.database.FirestoreHelper;
import com.news.kimo.databinding.ActivityCreatePostBinding;
import com.news.kimo.models.Post;
import com.news.kimo.models.User;
import com.news.kimo.utils.AIHelper;
import com.news.kimo.utils.Constants;
import com.news.kimo.utils.ImageCompressor;
import com.news.kimo.utils.SessionManager;
import com.news.kimo.utils.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreatePostActivity extends BaseActivity {

    private ActivityCreatePostBinding binding;
    private FirestoreHelper db;
    private FirebaseAuth mAuth;
    private SessionManager sessionManager;
    private AIHelper aiHelper;

    private String currentUid;
    private String currentUserName = "";
    private String currentUserPhoto = "";
    private String editPostId = null;
    private boolean isEditMode = false;

    private final List<Uri> selectedImageUris = new ArrayList<>();
    private final List<String> uploadedImageUrls = new ArrayList<>();
    private Uri selectedVideoUri = null;
    private String uploadedVideoUrl = null;

    private final List<EditText> pollOptionEdits = new ArrayList<>();
    private LinearLayout pollContainer;

    private boolean isScheduled = false;
    private long scheduledAt = 0;
    private int calendarYear, calendarMonth, calendarDay;
    private int calendarHour, calendarMinute;

    private static final String EXTRA_EDIT_POST_ID = "editPostId";
    private static final String EXTRA_EDIT_TEXT = "editText";
    private static final String EXTRA_EDIT_IMAGE_URL = "editImageUrl";
    private static final int MAX_IMAGES = 10;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCreatePostBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupStatusBar();

        db = FirestoreHelper.getInstance();
        mAuth = FirebaseAuth.getInstance();
        sessionManager = SessionManager.getInstance(this);
        aiHelper = AIHelper.getInstance();

        if (mAuth.getCurrentUser() == null) {
            finish();
            return;
        }
        currentUid = mAuth.getCurrentUser().getUid();

        initCalendarFields();
        checkEditMode();
        loadCurrentUserInfo();
        setupToolbar();
        setupPostText();
        setupImagePicker();
        setupVideoPicker();
        setupPollSection();
        setupCodeBlockSection();
        setupQuoteSection();
        setupLinkPreview();
        setupAIFeatures();
        setupScheduleButton();
        setupPublishButton();
        setupDraftButton();
    }

    // ==================================================================
    // Initialization
    // ==================================================================

    private void initCalendarFields() {
        Calendar cal = Calendar.getInstance();
        calendarYear = cal.get(Calendar.YEAR);
        calendarMonth = cal.get(Calendar.MONTH);
        calendarDay = cal.get(Calendar.DAY_OF_MONTH);
        calendarHour = cal.get(Calendar.HOUR_OF_DAY);
        calendarMinute = cal.get(Calendar.MINUTE);
    }

    private void checkEditMode() {
        if (getIntent() != null) {
            editPostId = getIntent().getStringExtra(EXTRA_EDIT_POST_ID);
            String editText = getIntent().getStringExtra(EXTRA_EDIT_TEXT);
            String editImageUrl = getIntent().getStringExtra(EXTRA_EDIT_IMAGE_URL);

            if (editPostId != null && !editPostId.isEmpty()) {
                isEditMode = true;
                binding.etPostText.setText(editText != null ? editText : "");
                if (editImageUrl != null && !editImageUrl.isEmpty()) {
                    binding.ivSelectedImagePreview.setVisibility(View.VISIBLE);
                    loadImage(editImageUrl, binding.ivSelectedImagePreview);
                    uploadedImageUrls.add(editImageUrl);
                }
                binding.toolbar.setTitle(R.string.edit_post);
            }
        }
    }

    private void loadCurrentUserInfo() {
        User cachedUser = sessionManager.loadCurrentUser();
        if (cachedUser != null) {
            currentUserName = cachedUser.getName() != null ? cachedUser.getName() : "";
            currentUserPhoto = cachedUser.getPhotoUrl() != null ? cachedUser.getPhotoUrl() : "";
            loadProfileImage(currentUserPhoto, binding.ivUserAvatar, currentUserName);
            binding.tvUserName.setText(currentUserName);
        }

        db.getUserRef(currentUid).addListenerForSingleValueEvent(
                new com.google.firebase.database.ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                        User user = snapshot.getValue(User.class);
                        if (user != null) {
                            currentUserName = user.getName() != null ? user.getName() : "";
                            currentUserPhoto = user.getPhotoUrl() != null ? user.getPhotoUrl() : "";
                            sessionManager.saveCurrentUser(user);
                            loadProfileImage(currentUserPhoto, binding.ivUserAvatar, currentUserName);
                            binding.tvUserName.setText(currentUserName);
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                        showError(error.getMessage());
                    }
                });
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(isEditMode ? R.string.edit_post : R.string.create_post);
        }
        binding.toolbar.setNavigationOnClickListener(v -> {
            hideKeyboard();
            finish();
        });
    }

    private void setupPostText() {
        binding.etPostText.setMinHeight(dp(150));
        binding.etPostText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                int len = s.length();
                binding.tvCharCount.setText(len + "/" + Constants.MAX_POST_LENGTH);
                binding.tvCharCount.setTextColor(
                        len > Constants.MAX_POST_LENGTH
                                ? getColor(R.color.colorError)
                                : getColor(R.color.colorTextSecondary));
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setupImagePicker() {
        binding.btnPickImage.setOnClickListener(v -> {
            if (selectedImageUris.size() >= MAX_IMAGES) {
                showMessage("الحد الأقصى " + MAX_IMAGES + " صور");
                return;
            }
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            startActivityForResult(Intent.createChooser(intent, "اختر الصور"), Constants.REQUEST_CODE_GALLERY);
        });

        binding.btnTakePhoto.setOnClickListener(v -> {
            Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            if (cameraIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(cameraIntent, Constants.REQUEST_CODE_CAMERA);
            }
        });
    }

    private void setupVideoPicker() {
        binding.btnPickVideo.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("video/*");
            startActivityForResult(Intent.createChooser(intent, "اختر فيديو"), Constants.REQUEST_CODE_VIDEO_GALLERY);
        });
    }

    private void setupPollSection() {
        pollContainer = binding.pollOptionsContainer;
        binding.btnAddPoll.setOnClickListener(v -> {
            binding.pollLayout.setVisibility(View.VISIBLE);
            addPollOption();
            addPollOption();
            v.setVisibility(View.GONE);
        });

        binding.btnRemovePoll.setOnClickListener(v -> {
            binding.pollLayout.setVisibility(View.GONE);
            pollContainer.removeAllViews();
            pollOptionEdits.clear();
            binding.btnAddPoll.setVisibility(View.VISIBLE);
        });

        binding.btnAddPollOption.setOnClickListener(v -> {
            if (pollOptionEdits.size() < 4) {
                addPollOption();
            } else {
                showMessage("الحد الأقصى 4 خيارات");
            }
        });
    }

    private void addPollOption() {
        if (pollOptionEdits.size() >= 4) return;
        EditText optionEdit = new EditText(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        int margin = dp(8);
        params.setMargins(0, margin, 0, margin);
        optionEdit.setLayoutParams(params);
        optionEdit.setHint("الخيار " + (pollOptionEdits.size() + 1));
        optionEdit.setBackground(getDrawable(R.drawable.bg_edit_text));
        optionEdit.setPadding(dp(16), dp(12), dp(16), dp(12));
        pollContainer.addView(optionEdit);
        pollOptionEdits.add(optionEdit);
    }

    private void setupCodeBlockSection() {
        String[] languages = {"Java", "Kotlin", "Python", "JavaScript", "C++", "Swift", "Go", "Ruby", "PHP", "HTML"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, languages);
        binding.actvCodeLanguage.setAdapter(adapter);

        binding.btnAddCode.setOnClickListener(v -> {
            binding.codeLayout.setVisibility(View.VISIBLE);
            v.setVisibility(View.GONE);
        });

        binding.btnRemoveCode.setOnClickListener(v -> {
            binding.codeLayout.setVisibility(View.GONE);
            binding.etCodeContent.setText("");
            binding.actvCodeLanguage.setText("");
            binding.btnAddCode.setVisibility(View.VISIBLE);
        });
    }

    private void setupQuoteSection() {
        binding.btnAddQuote.setOnClickListener(v -> {
            binding.quoteLayout.setVisibility(View.VISIBLE);
            v.setVisibility(View.GONE);
        });

        binding.btnRemoveQuote.setOnClickListener(v -> {
            binding.quoteLayout.setVisibility(View.GONE);
            binding.etQuoteText.setText("");
            binding.etQuoteAuthor.setText("");
            binding.btnAddQuote.setVisibility(View.VISIBLE);
        });
    }

    private void setupLinkPreview() {
        binding.btnAddLink.setOnClickListener(v -> {
            binding.linkLayout.setVisibility(View.VISIBLE);
            v.setVisibility(View.GONE);
        });

        binding.btnRemoveLink.setOnClickListener(v -> {
            binding.linkLayout.setVisibility(View.GONE);
            binding.etLinkUrl.setText("");
            binding.btnAddLink.setVisibility(View.VISIBLE);
        });
    }

    private void setupAIFeatures() {
        binding.btnAiSummary.setOnClickListener(v -> {
            String text = binding.etPostText.getText().toString().trim();
            if (text.isEmpty()) {
                showError("اكتب نص المنشور أولاً");
                return;
            }
            showLoading();
            aiHelper.generateSummary(text, new AIHelper.OnSummaryListener() {
                @Override
                public void onSummaryGenerated(String summary) {
                    hideLoading();
                    binding.tvAiSummaryResult.setVisibility(View.VISIBLE);
                    binding.tvAiSummaryResult.setText("ملخص AI: " + summary);
                }
                @Override
                public void onError(String error) {
                    hideLoading();
                    showError(error);
                }
            });
        });

        binding.btnAiTags.setOnClickListener(v -> {
            String text = binding.etPostText.getText().toString().trim();
            if (text.isEmpty()) {
                showError("اكتب نص المنشور أولاً");
                return;
            }
            showLoading();
            aiHelper.suggestTags(text, new AIHelper.OnTagsListener() {
                @Override
                public void onTagsSuggested(List<String> tags) {
                    hideLoading();
                    binding.chipGroupTags.removeAllViews();
                    binding.chipGroupTags.setVisibility(View.VISIBLE);
                    for (String tag : tags) {
                        Chip chip = new Chip(CreatePostActivity.this);
                        chip.setText("#" + tag);
                        chip.setChipBackgroundColorResource(R.color.colorPrimaryLight);
                        chip.setTextColor(getColor(R.color.colorPrimary));
                        chip.setClickable(false);
                        binding.chipGroupTags.addView(chip);
                    }
                }
                @Override
                public void onError(String error) {
                    hideLoading();
                    showError(error);
                }
            });
        });
    }

    private void setupScheduleButton() {
        binding.btnSchedule.setOnClickListener(v -> {
            showScheduleDialog();
        });
    }

    private void showScheduleDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_schedule_post, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("جدولة النشر")
                .setView(dialogView)
                .setPositiveButton("تأكيد", (d, which) -> {
                    isScheduled = true;
                    showMessage("تم جدولة المنشور");
                })
                .setNegativeButton("إلغاء", null)
                .create();
        dialog.show();

        ImageView ivDate = dialogView.findViewById(R.id.ivPickDate);
        ImageView ivTime = dialogView.findViewById(R.id.ivPickTime);

        ivDate.setOnClickListener(dv -> {
            DatePickerDialog datePicker = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                calendarYear = year;
                calendarMonth = month;
                calendarDay = dayOfMonth;
            }, calendarYear, calendarMonth, calendarDay);
            datePicker.show();
        });

        ivTime.setOnClickListener(tv -> {
            TimePickerDialog timePicker = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
                calendarHour = hourOfDay;
                calendarMinute = minute;
            }, calendarHour, calendarMinute, false);
            timePicker.show();
        });

        dialog.setOnDismissListener(d -> {
            if (isScheduled) {
                Calendar scheduledCal = Calendar.getInstance();
                scheduledCal.set(calendarYear, calendarMonth, calendarDay, calendarHour, calendarMinute, 0);
                scheduledAt = scheduledCal.getTimeInMillis();
                if (scheduledAt <= System.currentTimeMillis()) {
                    isScheduled = false;
                    showError("يجب أن يكون الوقت في المستقبل");
                }
            }
        });
    }

    private void setupPublishButton() {
        binding.btnPublish.setOnClickListener(v -> publishPost());
    }

    private void setupDraftButton() {
        binding.btnSaveDraft.setOnClickListener(v -> saveDraft());
    }

    // ==================================================================
    // Publish
    // ==================================================================

    private void publishPost() {
        String postText = binding.etPostText.getText().toString().trim();
        if (postText.isEmpty() && selectedImageUris.isEmpty() && selectedVideoUri == null) {
            showError("اكتب نص المنشور أو أضف وسائط");
            return;
        }
        if (postText.length() > Constants.MAX_POST_LENGTH) {
            showError("تجاوزت الحد الأقصى للنص");
            return;
        }
        if (!isNetworkAvailable()) {
            showError("لا يوجد اتصال بالإنترنت");
            return;
        }

        hideKeyboard();
        showLoading();

        if (selectedImageUris.isEmpty() && selectedVideoUri == null) {
            writePostToDatabase(postText, null, null);
        } else if (selectedVideoUri != null) {
            uploadVideo(postText);
        } else {
            uploadImages(postText);
        }
    }

    private void uploadImages(String postText) {
        uploadedImageUrls.clear();
        StorageReference storageRef = FirebaseStorage.getInstance().getReference()
                .child(Constants.MEDIA).child(currentUid).child(System.currentTimeMillis() + "");

        List<Task<Uri>> uploadTasks = new ArrayList<>();
        for (int i = 0; i < selectedImageUris.size(); i++) {
            Uri imageUri = selectedImageUris.get(i);
            StorageReference imageRef = storageRef.child("image_" + i + "_" + System.currentTimeMillis() + ".jpg");
            UploadTask uploadTask = imageRef.putFile(imageUri);
            uploadTasks.add(uploadTask.continueWithTask(task -> {
                if (!task.isSuccessful()) throw task.getException();
                return imageRef.getDownloadUrl();
            }));
        }

        com.google.android.gms.tasks.Tasks.whenAllSuccess(uploadTasks)
                .addOnSuccessListener(results -> {
                    List<String> urls = new ArrayList<>();
                    for (Object result : results) {
                        urls.add(((Uri) result).toString());
                    }
                    uploadedImageUrls.addAll(urls);
                    writePostToDatabase(postText, uploadedImageUrls, null);
                })
                .addOnFailureListener(e -> {
                    hideLoading();
                    showError("فشل رفع الصور: " + e.getMessage());
                });
    }

    private void uploadVideo(String postText) {
        StorageReference videoRef = FirebaseStorage.getInstance().getReference()
                .child(Constants.MEDIA).child(currentUid)
                .child("video_" + System.currentTimeMillis() + ".mp4");

        videoRef.putFile(selectedVideoUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    return videoRef.getDownloadUrl();
                })
                .addOnSuccessListener(uri -> {
                    uploadedVideoUrl = uri.toString();
                    writePostToDatabase(postText, null, uploadedVideoUrl);
                })
                .addOnFailureListener(e -> {
                    hideLoading();
                    showError("فشل رفع الفيديو: " + e.getMessage());
                });
    }

    private void writePostToDatabase(String postText, List<String> imageUrls, String videoUrl) {
        DatabaseReference postsRef = db.getReference(Constants.POSTS);
        String pushId;

        if (isEditMode && editPostId != null) {
            pushId = editPostId;
        } else {
            pushId = postsRef.push().getKey();
        }

        if (pushId == null) {
            hideLoading();
            showError("حدث خطأ غير متوقع");
            return;
        }

        List<String> hashtags = StringUtils.extractHashtags(postText);
        List<String> mentions = StringUtils.extractMentions(postText);

        List<Map<String, Object>> pollOptions = new ArrayList<>();
        if (binding.pollLayout.getVisibility() == View.VISIBLE) {
            for (EditText edit : pollOptionEdits) {
                String optionText = edit.getText().toString().trim();
                if (!optionText.isEmpty()) {
                    Map<String, Object> option = new HashMap<>();
                    option.put("text", optionText);
                    option.put("votes", 0L);
                    pollOptions.add(option);
                }
            }
        }

        String codeContent = "";
        String codeLanguage = "";
        if (binding.codeLayout.getVisibility() == View.VISIBLE) {
            codeContent = binding.etCodeContent.getText().toString().trim();
            codeLanguage = binding.actvCodeLanguage.getText().toString().trim();
        }

        String quoteText = "";
        String quoteAuthor = "";
        if (binding.quoteLayout.getVisibility() == View.VISIBLE) {
            quoteText = binding.etQuoteText.getText().toString().trim();
            quoteAuthor = binding.etQuoteAuthor.getText().toString().trim();
        }

        String linkUrl = "";
        if (binding.linkLayout.getVisibility() == View.VISIBLE) {
            linkUrl = binding.etLinkUrl.getText().toString().trim();
        }

        String singleImageUrl = "";
        List<String> finalImages = new ArrayList<>();
        if (imageUrls != null && !imageUrls.isEmpty()) {
            if (imageUrls.size() == 1) {
                singleImageUrl = imageUrls.get(0);
            } else {
                finalImages = imageUrls;
            }
        } else if (!uploadedImageUrls.isEmpty()) {
            if (uploadedImageUrls.size() == 1) {
                singleImageUrl = uploadedImageUrls.get(0);
            } else {
                finalImages = uploadedImageUrls;
            }
        }

        Post post = new Post();
        post.setPostId(pushId);
        post.setUid(currentUid);
        post.setUserName(currentUserName);
        post.setUserPhoto(currentUserPhoto);
        post.setText(postText);
        post.setImageUrl(singleImageUrl);
        post.setImages(finalImages);
        post.setVideoUrl(videoUrl != null ? videoUrl : uploadedVideoUrl);
        post.setPollOptions(pollOptions);
        post.setPollVotes(new HashMap<>());
        post.setCodeContent(codeContent);
        post.setCodeLanguage(codeLanguage);
        post.setQuoteText(quoteText);
        post.setQuoteAuthor(quoteAuthor);
        post.setLinkUrl(linkUrl);
        post.setTags(hashtags);
        post.setMentions(mentions);
        post.setScheduled(isScheduled);
        post.setScheduledAt(scheduledAt);
        post.setPinned(false);
        post.setArchived(false);
        post.setTimestamp(isScheduled ? scheduledAt : System.currentTimeMillis());
        post.setLikesCount(0);
        post.setCommentsCount(0);
        post.setSharesCount(0);
        post.setViewsCount(0);

        Map<String, Object> postMap = new HashMap<>();
        postMap.put("postId", post.getPostId());
        postMap.put("uid", post.getUid());
        postMap.put("userName", post.getUserName());
        postMap.put("userPhoto", post.getUserPhoto());
        postMap.put("text", post.getText());
        postMap.put("imageUrl", post.getImageUrl());
        postMap.put("images", post.getImages());
        postMap.put("videoUrl", post.getVideoUrl());
        postMap.put("pollOptions", post.getPollOptions());
        postMap.put("pollVotes", post.getPollVotes());
        postMap.put("codeContent", post.getCodeContent());
        postMap.put("codeLanguage", post.getCodeLanguage());
        postMap.put("quoteText", post.getQuoteText());
        postMap.put("quoteAuthor", post.getQuoteAuthor());
        postMap.put("linkUrl", post.getLinkUrl());
        postMap.put("tags", post.getTags());
        postMap.put("mentions", post.getMentions());
        postMap.put("isScheduled", post.isScheduled());
        postMap.put("scheduledAt", post.getScheduledAt());
        postMap.put("isPinned", post.isPinned());
        postMap.put("isArchived", post.isArchived());
        postMap.put("timestamp", post.getTimestamp());
        postMap.put("likesCount", post.getLikesCount());
        postMap.put("commentsCount", post.getCommentsCount());
        postMap.put("sharesCount", post.getSharesCount());
        postMap.put("viewsCount", post.getViewsCount());
        postMap.put("reactions", new HashMap<String, Long>());
        postMap.put("aiSummary", "");

        postsRef.child(pushId).setValue(postMap)
                .addOnCompleteListener(task -> {
                    hideLoading();
                    if (task.isSuccessful()) {
                        if (isEditMode) {
                            showMessage("تم تحديث المنشور بنجاح");
                        } else {
                            showMessage(isScheduled ? "تم جدولة المنشور" : "تم نشر المنشور بنجاح");
                        }
                        setResult(RESULT_OK);
                        finish();
                    } else {
                        showError("فشل نشر المنشور");
                    }
                });
    }

    // ==================================================================
    // Draft
    // ==================================================================

    private void saveDraft() {
        String text = binding.etPostText.getText().toString().trim();
        if (text.isEmpty() && selectedImageUris.isEmpty()) {
            showMessage("لا يوجد محتوى للحفظ");
            return;
        }
        DatabaseReference draftsRef = db.getReference("drafts").child(currentUid).push();
        Map<String, Object> draft = new HashMap<>();
        draft.put("text", text);
        draft.put("timestamp", System.currentTimeMillis());
        draft.put("uid", currentUid);
        draftsRef.setValue(draft)
                .addOnSuccessListener(v -> showMessage("تم حفظ المسودة"))
                .addOnFailureListener(e -> showError("فشل حفظ المسودة"));
    }

    // ==================================================================
    // Result Handling
    // ==================================================================

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) return;

        if (requestCode == Constants.REQUEST_CODE_GALLERY) {
            ClipData clip = data.getClipData();
            if (clip != null) {
                for (int i = 0; i < clip.getItemCount(); i++) {
                    if (selectedImageUris.size() >= MAX_IMAGES) break;
                    Uri uri = clip.getItemAt(i).getUri();
                    selectedImageUris.add(uri);
                }
            } else if (data.getData() != null) {
                selectedImageUris.add(data.getData());
            }
            updateImagePreview();
        } else if (requestCode == Constants.REQUEST_CODE_CAMERA) {
            if (data.getExtras() != null && data.getExtras().get("data") != null) {
                android.graphics.Bitmap bitmap = (android.graphics.Bitmap) data.getExtras().get("data");
                Uri tempUri = getImageUri(this, bitmap);
                selectedImageUris.add(tempUri);
                updateImagePreview();
            }
        } else if (requestCode == Constants.REQUEST_CODE_VIDEO_GALLERY) {
            if (data.getData() != null) {
                selectedVideoUri = data.getData();
                binding.ivVideoPreview.setVisibility(View.VISIBLE);
                binding.ivVideoPreview.setImageURI(selectedVideoUri);
                showMessage("تم اختيار الفيديو");
            }
        }
    }

    private Uri getImageUri(Context context, android.graphics.Bitmap bitmap) {
        java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = android.provider.MediaStore.Images.Media.insertImage(
                context.getContentResolver(), bitmap, "Title", null);
        return Uri.parse(path);
    }

    private void updateImagePreview() {
        if (!selectedImageUris.isEmpty()) {
            binding.ivSelectedImagePreview.setVisibility(View.VISIBLE);
            loadImage(selectedImageUris.get(0).toString(), binding.ivSelectedImagePreview);
            if (selectedImageUris.size() > 1) {
                binding.tvImageCount.setVisibility(View.VISIBLE);
                binding.tvImageCount.setText("+" + (selectedImageUris.size() - 1));
            }
        } else {
            binding.ivSelectedImagePreview.setVisibility(View.GONE);
            binding.tvImageCount.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        hideKeyboard();
        super.onBackPressed();
    }
}
