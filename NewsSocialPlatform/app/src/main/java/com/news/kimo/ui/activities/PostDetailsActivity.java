package com.news.kimo.ui.activities;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.news.kimo.R;
import com.news.kimo.adapters.CommentAdapter;
import com.news.kimo.adapters.ImageViewPagerAdapter;
import com.news.kimo.database.FirestoreHelper;
import com.news.kimo.databinding.ActivityPostDetailsBinding;
import com.news.kimo.models.Comment;
import com.news.kimo.models.Post;
import com.news.kimo.models.Reaction;
import com.news.kimo.models.User;
import com.news.kimo.utils.Constants;
import com.news.kimo.utils.DateUtils;
import com.news.kimo.utils.SessionManager;
import com.news.kimo.utils.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PostDetailsActivity extends BaseActivity {

    private ActivityPostDetailsBinding binding;
    private FirestoreHelper db;
    private FirebaseAuth mAuth;
    private SessionManager sessionManager;

    private String postId;
    private String currentUid;
    private Post currentPost;
    private User postOwner;
    private User currentUser;

    private boolean isPostOwner = false;
    private boolean isLiked = false;
    private boolean isSaved = false;
    private String myReactionType = null;

    private ExoPlayer exoPlayer;

    private CommentAdapter commentAdapter;
    private final List<Comment> commentList = new ArrayList<>();
    private String replyToCommentId = null;
    private String replyToCommentText = "";

    private ValueEventListener postListener;
    private ChildEventListener commentChildListener;
    private ValueEventListener likesListener;
    private ValueEventListener savedListener;

    // Reaction emoji arrays
    private static final int[] REACTION_ICONS = {
            R.drawable.ic_reaction_like,
            R.drawable.ic_reaction_love,
            R.drawable.ic_reaction_haha,
            R.drawable.ic_reaction_wow,
            R.drawable.ic_reaction_sad,
            R.drawable.ic_reaction_angry
    };
    private static final String[] REACTION_TYPES = {
            Constants.REACTION_LIKE,
            Constants.REACTION_LOVE,
            Constants.REACTION_HAHA,
            Constants.REACTION_WOW,
            Constants.REACTION_SAD,
            Constants.REACTION_ANGRY
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPostDetailsBinding.inflate(getLayoutInflater());
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
        currentUser = sessionManager.loadCurrentUser();

        postId = getIntent().getStringExtra(Constants.EXTRA_POST_ID);
        if (postId == null || postId.isEmpty()) {
            showError("معرف المنشور مطلوب");
            finish();
            return;
        }

        setupToolbar();
        loadPostRealtime();
        checkLikeStatus();
        checkSavedStatus();
        setupCommentInput();
        setupKeyboardHandling();
        incrementViewCount();
    }

    // ==================================================================
    // Toolbar
    // ==================================================================

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");
        }
        binding.toolbar.setNavigationOnClickListener(v -> {
            releasePlayer();
            finish();
        });
    }

    // ==================================================================
    // Load Post (Realtime)
    // ==================================================================

    private void loadPostRealtime() {
        showLoading();
        postListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                hideLoading();
                currentPost = snapshot.getValue(Post.class);
                if (currentPost == null) {
                    showError("المنشور غير موجود");
                    return;
                }
                isPostOwner = currentUid.equals(currentPost.getUid());
                bindPostUI();
                loadPostOwner();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                hideLoading();
                showError(error.getMessage());
            }
        };
        db.getPostRef(postId).addValueEventListener(postListener);
    }

    private void bindPostUI() {
        if (currentPost == null) return;

        // User row
        loadProfileImage(currentPost.getUserPhoto(), binding.ivUserAvatar, currentPost.getUserName());
        binding.tvUserName.setText(currentPost.getUserName());
        binding.tvPostTime.setText(getRelativeTime(currentPost.getTimestamp()));

        if (postOwner != null && postOwner.isVerified()) {
            binding.ivVerifiedBadge.setVisibility(View.VISIBLE);
        } else {
            binding.ivVerifiedBadge.setVisibility(View.GONE);
        }

        // Follow button (only if other user)
        if (isPostOwner) {
            binding.btnFollowUser.setVisibility(View.GONE);
        } else {
            binding.btnFollowUser.setVisibility(View.VISIBLE);
            checkFollowStatus();
        }

        // Text
        if (!TextUtils.isEmpty(currentPost.getText())) {
            binding.tvPostText.setVisibility(View.VISIBLE);
            binding.tvPostText.setText(currentPost.getText());
        } else {
            binding.tvPostText.setVisibility(View.GONE);
        }

        // Images
        if (currentPost.getImages() != null && currentPost.getImages().size() > 1) {
            setupViewPager();
            binding.viewPagerImages.setVisibility(View.VISIBLE);
            binding.ivSingleImage.setVisibility(View.GONE);
        } else if (!TextUtils.isEmpty(currentPost.getImageUrl())) {
            binding.ivSingleImage.setVisibility(View.VISIBLE);
            binding.viewPagerImages.setVisibility(View.GONE);
            loadImage(currentPost.getImageUrl(), binding.ivSingleImage);
        } else {
            binding.ivSingleImage.setVisibility(View.GONE);
            binding.viewPagerImages.setVisibility(View.GONE);
        }

        // Video
        if (!TextUtils.isEmpty(currentPost.getVideoUrl())) {
            binding.playerView.setVisibility(View.VISIBLE);
            setupExoPlayer(currentPost.getVideoUrl());
        } else {
            binding.playerView.setVisibility(View.GONE);
        }

        // Poll
        if (currentPost.getPollOptions() != null && !currentPost.getPollOptions().isEmpty()) {
            binding.layoutPoll.setVisibility(View.VISIBLE);
            setupPoll();
        } else {
            binding.layoutPoll.setVisibility(View.GONE);
        }

        // Quote
        if (!TextUtils.isEmpty(currentPost.getQuoteText())) {
            binding.layoutQuote.setVisibility(View.VISIBLE);
            binding.tvQuoteText.setText(currentPost.getQuoteText());
            binding.tvQuoteAuthor.setText(
                    TextUtils.isEmpty(currentPost.getQuoteAuthor()) ? ""
                            : "— " + currentPost.getQuoteAuthor());
        } else {
            binding.layoutQuote.setVisibility(View.GONE);
        }

        // Code block
        if (!TextUtils.isEmpty(currentPost.getCodeContent())) {
            binding.layoutCode.setVisibility(View.VISIBLE);
            binding.tvCodeContent.setText(currentPost.getCodeContent());
            binding.tvCodeLanguage.setText(
                    TextUtils.isEmpty(currentPost.getCodeLanguage()) ? ""
                            : currentPost.getCodeLanguage());
        } else {
            binding.layoutCode.setVisibility(View.GONE);
        }

        // Link preview
        if (!TextUtils.isEmpty(currentPost.getLinkUrl())) {
            binding.layoutLink.setVisibility(View.VISIBLE);
            binding.tvLinkUrl.setText(currentPost.getLinkUrl());
        } else {
            binding.layoutLink.setVisibility(View.GONE);
        }

        // Counts
        updateActionCounts();

        // More options menu
        binding.ivMoreOptions.setOnClickListener(v -> showMoreOptions());
    }

    private void setupViewPager() {
        List<String> images = currentPost.getImages();
        ImageViewPagerAdapter adapter = new ImageViewPagerAdapter(this, images);
        binding.viewPagerImages.setAdapter(adapter);
        if (images.size() > 1) {
            binding.viewPagerImages.setCurrentItem(0, false);
            binding.tvImageIndicator.setVisibility(View.VISIBLE);
            binding.tvImageIndicator.setText("1/" + images.size());
            binding.viewPagerImages.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    binding.tvImageIndicator.setText((position + 1) + "/" + images.size());
                }
            });
        } else {
            binding.tvImageIndicator.setVisibility(View.GONE);
        }
    }

    private void setupExoPlayer(String videoUrl) {
        releasePlayer();
        exoPlayer = new ExoPlayer.Builder(this).build();
        MediaItem mediaItem = MediaItem.fromUri(videoUrl);
        exoPlayer.setMediaItem(mediaItem);
        binding.playerView.setPlayer(exoPlayer);
        exoPlayer.prepare();
    }

    private void releasePlayer() {
        if (exoPlayer != null) {
            exoPlayer.release();
            exoPlayer = null;
        }
    }

    private void loadPostOwner() {
        String ownerUid = currentPost.getUid();
        db.getUserRef(ownerUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                postOwner = snapshot.getValue(User.class);
                if (postOwner != null) {
                    if (postOwner.isVerified()) {
                        binding.ivVerifiedBadge.setVisibility(View.VISIBLE);
                    }
                    binding.tvUserName.setText(postOwner.getName());
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // ==================================================================
    // Reactions / Like System
    // ==================================================================

    private void checkLikeStatus() {
        likesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                isLiked = snapshot.exists();
                if (isLiked) {
                    Reaction reaction = snapshot.getValue(Reaction.class);
                    myReactionType = reaction != null ? reaction.getType() : Constants.REACTION_LIKE;
                } else {
                    myReactionType = null;
                }
                updateLikeButton();
                updateReactionsRow();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        db.getReference(Constants.LIKES).child(postId).child(currentUid)
                .addValueEventListener(likesListener);
    }

    private void updateLikeButton() {
        if (isLiked) {
            binding.btnLike.setImageResource(R.drawable.ic_reaction_like_filled);
            binding.btnLike.setColorFilter(ContextCompat.getColor(this, R.color.colorError));
        } else {
            binding.btnLike.setImageResource(R.drawable.ic_reaction_like);
            binding.btnLike.setColorFilter(null);
        }
    }

    private void updateReactionsRow() {
        if (currentPost == null || currentPost.getReactions() == null) {
            binding.layoutReactions.setVisibility(View.GONE);
            return;
        }
        Map<String, Long> reactions = currentPost.getReactions();
        long total = 0;
        for (Long count : reactions.values()) {
            total += count != null ? count : 0;
        }
        if (total <= 0) {
            binding.layoutReactions.setVisibility(View.GONE);
            return;
        }
        binding.layoutReactions.setVisibility(View.VISIBLE);
        LinearLayout container = binding.reactionsContainer;
        container.removeAllViews();
        int idx = 0;
        for (Map.Entry<String, Long> entry : reactions.entrySet()) {
            if (entry.getValue() == null || entry.getValue() <= 0) continue;
            View chip = LayoutInflater.from(this).inflate(R.layout.item_reaction_chip, container, false);
            ImageView ivIcon = chip.findViewById(R.id.ivReactionIcon);
            TextView tvCount = chip.findViewById(R.id.tvReactionCount);
            int iconIdx = getReactionIconIndex(entry.getKey());
            if (iconIdx >= 0) ivIcon.setImageResource(REACTION_ICONS[iconIdx]);
            tvCount.setText(String.valueOf(entry.getValue()));
            container.addView(chip);
            idx++;
            if (idx >= 6) break;
        }
    }

    private int getReactionIconIndex(String type) {
        for (int i = 0; i < REACTION_TYPES.length; i++) {
            if (REACTION_TYPES[i].equals(type)) return i;
        }
        return 0;
    }

    private void toggleReaction(String type) {
        DatabaseReference likeRef = db.getReference(Constants.LIKES).child(postId).child(currentUid);
        DatabaseReference postRef = db.getPostRef(postId);

        if (isLiked) {
            if (type.equals(myReactionType)) {
                // Remove reaction
                likeRef.removeValue((error, ref) -> {
                    postRef.child("likesCount").runTransaction(new Transaction.Handler() {
                        @NonNull
                        @Override
                        public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                            Long val = currentData.getValue(Long.class);
                            if (val == null) val = 0L;
                            currentData.setValue(Math.max(0, val - 1));
                            return Transaction.success(currentData);
                        }
                        @Override
                        public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {}
                    });
                    postRef.child("reactions").child(type).runTransaction(new Transaction.Handler() {
                        @NonNull
                        @Override
                        public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                            Long val = currentData.getValue(Long.class);
                            if (val == null) val = 0L;
                            currentData.setValue(Math.max(0, val - 1));
                            return Transaction.success(currentData);
                        }
                        @Override
                        public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {}
                    });
                });
            } else {
                // Change reaction type
                String oldType = myReactionType;
                Map<String, Object> update = new HashMap<>();
                update.put("type", type);
                likeRef.updateChildren(update);
                if (oldType != null) {
                    postRef.child("reactions").child(oldType).runTransaction(new Transaction.Handler() {
                        @NonNull
                        @Override
                        public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                            Long val = currentData.getValue(Long.class);
                            if (val == null) val = 0L;
                            currentData.setValue(Math.max(0, val - 1));
                            return Transaction.success(currentData);
                        }
                        @Override
                        public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {}
                    });
                }
                postRef.child("reactions").child(type).runTransaction(new Transaction.Handler() {
                    @NonNull
                    @Override
                    public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                        Long val = currentData.getValue(Long.class);
                        if (val == null) val = 0L;
                        currentData.setValue(val + 1);
                        return Transaction.success(currentData);
                    }
                    @Override
                    public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {}
                });
            }
        } else {
            // Add reaction
            Reaction reaction = new Reaction(postId, currentUid, type, System.currentTimeMillis());
            likeRef.setValue(reaction, (error, ref) -> {
                postRef.child("likesCount").runTransaction(new Transaction.Handler() {
                    @NonNull
                    @Override
                    public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                        Long val = currentData.getValue(Long.class);
                        if (val == null) val = 0L;
                        currentData.setValue(val + 1);
                        return Transaction.success(currentData);
                    }
                    @Override
                    public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {}
                });
                postRef.child("reactions").child(type).runTransaction(new Transaction.Handler() {
                    @NonNull
                    @Override
                    public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                        Long val = currentData.getValue(Long.class);
                        if (val == null) val = 0L;
                        currentData.setValue(val + 1);
                        return Transaction.success(currentData);
                    }
                    @Override
                    public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {}
                });
            });
        }
    }

    private void showReactionPicker() {
        View popupView = LayoutInflater.from(this).inflate(R.layout.popup_reaction_picker, null);
        PopupWindow popupWindow = new PopupWindow(popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true);
        popupWindow.setOutsideTouchable(true);

        LinearLayout container = popupView.findViewById(R.id.reactionsContainer);
        for (int i = 0; i < REACTION_ICONS.length; i++) {
            ImageView ivReaction = new ImageView(this);
            ivReaction.setImageResource(REACTION_ICONS[i]);
            ivReaction.setPadding(dp(8), dp(4), dp(8), dp(4));
            int finalI = i;
            ivReaction.setOnClickListener(v -> {
                toggleReaction(REACTION_TYPES[finalI]);
                popupWindow.dismiss();
            });
            container.addView(ivReaction);
        }

        popupWindow.showAsDropDown(binding.btnLike, 0, -dp(120));
    }

    // ==================================================================
    // Action Buttons
    // ==================================================================

    private void updateActionCounts() {
        if (currentPost == null) return;
        binding.tvLikeCount.setText(formatCount(currentPost.getLikesCount()));
        binding.tvCommentCount.setText(formatCount(currentPost.getCommentsCount()));
        binding.tvShareCount.setText(formatCount(currentPost.getSharesCount()));
    }

    private String formatCount(long count) {
        if (count >= 1000000) return (count / 1000000) + "M";
        if (count >= 1000) return (count / 1000) + "K";
        return String.valueOf(count);
    }

    private void setupActionButtons() {
        binding.btnLike.setOnClickListener(v -> {
            if (currentPost == null) return;
            String type = isLiked ? myReactionType : Constants.REACTION_LIKE;
            toggleReaction(type);
        });

        binding.btnLike.setOnLongClickListener(v -> {
            showReactionPicker();
            return true;
        });

        binding.btnComment.setOnClickListener(v -> {
            binding.etCommentInput.requestFocus();
            android.view.inputmethod.InputMethodManager imm =
                    (android.view.inputmethod.InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(binding.etCommentInput, 0);
        });

        binding.btnShare.setOnClickListener(v -> sharePost());

        binding.btnSave.setOnClickListener(v -> toggleSavePost());

        binding.btnRepost.setOnClickListener(v -> repostPost());
    }

    // ==================================================================
    // Share
    // ==================================================================

    private void sharePost() {
        if (currentPost == null) return;
        String shareText = currentPost.getText() != null ? currentPost.getText() : "";
        String deepLink = "https://kimo.app/posts/" + postId;

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText + "\n\n" + deepLink);
        startActivity(Intent.createChooser(shareIntent, "مشاركة المنشور"));

        // Update share count
        db.getPostRef(postId).child("sharesCount").runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                Long val = currentData.getValue(Long.class);
                if (val == null) val = 0L;
                currentData.setValue(val + 1);
                return Transaction.success(currentData);
            }
            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {}
        });
    }

    // ==================================================================
    // Save / Bookmark
    // ==================================================================

    private void checkSavedStatus() {
        savedListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                isSaved = snapshot.exists();
                updateSaveButton();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        db.getSavedPostsRef(currentUid).child(postId).addValueEventListener(savedListener);
    }

    private void toggleSavePost() {
        DatabaseReference savedRef = db.getSavedPostsRef(currentUid).child(postId);
        if (isSaved) {
            savedRef.removeValue()
                    .addOnSuccessListener(v -> showMessage("تم إزالة الحفظ"))
                    .addOnFailureListener(e -> showError(e.getMessage()));
        } else {
            Map<String, Object> saveData = new HashMap<>();
            saveData.put("postId", postId);
            saveData.put("savedAt", System.currentTimeMillis());
            savedRef.setValue(saveData)
                    .addOnSuccessListener(v -> showMessage("تم حفظ المنشور"))
                    .addOnFailureListener(e -> showError(e.getMessage()));
        }
    }

    private void updateSaveButton() {
        if (isSaved) {
            binding.btnSave.setImageResource(R.drawable.ic_bookmark_filled);
            binding.btnSave.setColorFilter(ContextCompat.getColor(this, R.color.colorPrimary));
        } else {
            binding.btnSave.setImageResource(R.drawable.ic_bookmark);
            binding.btnSave.setColorFilter(null);
        }
    }

    // ==================================================================
    // Repost
    // ==================================================================

    private void repostPost() {
        if (currentPost == null || !isNetworkAvailable()) {
            showError("لا يوجد اتصال");
            return;
        }
        String newPostId = db.getReference(Constants.POSTS).push().getKey();
        if (newPostId == null) return;

        Map<String, Object> repost = new HashMap<>();
        repost.put("postId", newPostId);
        repost.put("uid", currentUid);
        repost.put("userName", currentUser != null ? currentUser.getName() : "");
        repost.put("userPhoto", currentUser != null ? currentUser.getPhotoUrl() : "");
        repost.put("repostId", postId);
        repost.put("repostUserName", currentPost.getUserName());
        repost.put("timestamp", System.currentTimeMillis());
        repost.put("likesCount", 0L);
        repost.put("commentsCount", 0L);
        repost.put("sharesCount", 0L);
        repost.put("viewsCount", 0L);

        db.getReference(Constants.POSTS).child(newPostId).setValue(repost)
                .addOnSuccessListener(v -> showMessage("تم إعادة النشر"))
                .addOnFailureListener(e -> showError("فشل إعادة النشر"));
    }

    // ==================================================================
    // Comments
    // ==================================================================

    private void setupCommentInput() {
        binding.rvComments.setLayoutManager(new LinearLayoutManager(this));
        commentAdapter = new CommentAdapter(this, commentList, currentUid, new CommentAdapter.OnCommentActionListener() {
            @Override
            public void onReply(Comment comment) {
                replyToCommentId = comment.getCommentId();
                replyToCommentText = comment.getText();
                binding.layoutReplyPreview.setVisibility(View.VISIBLE);
                binding.tvReplyPreviewText.setText("الرد على " + comment.getUserName() + ": " +
                        StringUtils.truncateText(comment.getText(), 50));
                binding.etCommentInput.requestFocus();
                binding.ivCancelReply.setOnClickListener(v -> {
                    replyToCommentId = null;
                    replyToCommentText = "";
                    binding.layoutReplyPreview.setVisibility(View.GONE);
                });
            }

            @Override
            public void onDelete(Comment comment) {
                deleteComment(comment);
            }

            @Override
            public void onUserClick(String uid) {
                if (!uid.equals(currentUid)) {
                    Bundle bundle = new Bundle();
                    bundle.putString(Constants.EXTRA_USER_ID, uid);
                    openActivity(ProfileActivity.class, bundle);
                }
            }
        });
        binding.rvComments.setAdapter(commentAdapter);
        loadComments();

        binding.btnSendComment.setOnClickListener(v -> addComment());

        binding.etCommentInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                addComment();
                return true;
            }
            return false;
        });
    }

    private void loadComments() {
        commentChildListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Comment comment = snapshot.getValue(Comment.class);
                if (comment != null) {
                    commentList.add(comment);
                    commentAdapter.notifyItemInserted(commentList.size() - 1);
                    binding.rvComments.scrollToPosition(commentList.size() - 1);
                }
            }
            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                String removedId = snapshot.getKey();
                for (int i = 0; i < commentList.size(); i++) {
                    if (commentList.get(i).getCommentId() != null &&
                            commentList.get(i).getCommentId().equals(removedId)) {
                        commentList.remove(i);
                        commentAdapter.notifyItemRemoved(i);
                        break;
                    }
                }
            }
            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        db.getCommentsRef(postId).addChildEventListener(commentChildListener);
    }

    private void addComment() {
        String text = binding.etCommentInput.getText().toString().trim();
        if (text.isEmpty()) return;
        if (!isNetworkAvailable()) {
            showError("لا يوجد اتصال بالإنترنت");
            return;
        }

        String userName = currentUser != null ? currentUser.getName() : "";
        String userPhoto = currentUser != null ? currentUser.getPhotoUrl() : "";

        if (replyToCommentId != null) {
            // Reply to comment
            String replyId = db.getReference(Constants.REPLIES)
                    .child(replyToCommentId).push().getKey();
            if (replyId == null) return;

            Map<String, Object> reply = new HashMap<>();
            reply.put("commentId", replyId);
            reply.put("postId", postId);
            reply.put("uid", currentUid);
            reply.put("userName", userName);
            reply.put("userPhoto", userPhoto);
            reply.put("text", text);
            reply.put("timestamp", System.currentTimeMillis());
            reply.put("parentId", replyToCommentId);

            db.getReference(Constants.REPLIES).child(replyToCommentId).child(replyId).setValue(reply)
                    .addOnSuccessListener(v -> {
                        // Update reply count on comment
                        db.getCommentsRef(postId).child(replyToCommentId).child("replyCount")
                                .runTransaction(new Transaction.Handler() {
                                    @NonNull
                                    @Override
                                    public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                                        Long val = currentData.getValue(Long.class);
                                        if (val == null) val = 0L;
                                        currentData.setValue(val + 1);
                                        return Transaction.success(currentData);
                                    }
                                    @Override
                                    public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot d) {}
                                });
                        binding.etCommentInput.setText("");
                        replyToCommentId = null;
                        replyToCommentText = "";
                        binding.layoutReplyPreview.setVisibility(View.GONE);
                        hideKeyboard();
                    });
        } else {
            // Top-level comment
            String commentId = db.getCommentsRef(postId).push().getKey();
            if (commentId == null) return;

            Map<String, Object> commentMap = new HashMap<>();
            commentMap.put("commentId", commentId);
            commentMap.put("postId", postId);
            commentMap.put("uid", currentUid);
            commentMap.put("userName", userName);
            commentMap.put("userPhoto", userPhoto);
            commentMap.put("text", text);
            commentMap.put("timestamp", System.currentTimeMillis());
            commentMap.put("likesCount", 0L);
            commentMap.put("replyCount", 0L);
            commentMap.put("parentId", "");

            db.getCommentsRef(postId).child(commentId).setValue(commentMap)
                    .addOnSuccessListener(v -> {
                        // Update post comment count
                        db.getPostRef(postId).child("commentsCount").runTransaction(new Transaction.Handler() {
                            @NonNull
                            @Override
                            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                                Long val = currentData.getValue(Long.class);
                                if (val == null) val = 0L;
                                currentData.setValue(val + 1);
                                return Transaction.success(currentData);
                            }
                            @Override
                            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot d) {}
                        });
                        binding.etCommentInput.setText("");
                        hideKeyboard();
                    });
        }
    }

    private void deleteComment(Comment comment) {
        boolean canDelete = currentUid.equals(comment.getUid()) || isPostOwner;
        if (!canDelete) {
            showError("لا يمكنك حذف هذا التعليق");
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("حذف التعليق")
                .setMessage("هل أنت متأكد من حذف هذا التعليق؟")
                .setPositiveButton("حذف", (d, w) -> {
                    db.getCommentsRef(postId).child(comment.getCommentId()).removeValue()
                            .addOnSuccessListener(v -> {
                                db.getPostRef(postId).child("commentsCount").runTransaction(new Transaction.Handler() {
                                    @NonNull
                                    @Override
                                    public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                                        Long val = currentData.getValue(Long.class);
                                        if (val == null) val = 0L;
                                        currentData.setValue(Math.max(0, val - 1));
                                        return Transaction.success(currentData);
                                    }
                                    @Override
                                    public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot d) {}
                                });
                            });
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    // ==================================================================
    // Poll
    // ==================================================================

    private void setupPoll() {
        LinearLayout pollContainer = binding.pollOptionsContainer;
        pollContainer.removeAllViews();
        if (currentPost.getPollOptions() == null) return;

        Map<String, Long> votes = currentPost.getPollVotes() != null ? currentPost.getPollVotes() : new HashMap<>();
        long totalVotes = 0;
        for (Long v : votes.values()) {
            totalVotes += v != null ? v : 0;
        }

        for (int i = 0; i < currentPost.getPollOptions().size(); i++) {
            Map<String, Object> option = currentPost.getPollOptions().get(i);
            String optionText = (String) option.get("text");
            if (optionText == null) continue;

            View optionView = LayoutInflater.from(this).inflate(R.layout.item_poll_option, pollContainer, false);
            TextView tvOptionText = optionView.findViewById(R.id.tvPollOptionText);
            TextView tvOptionPercent = optionView.findViewById(R.id.tvPollPercent);
            View progressView = optionView.findViewById(R.id.progressPoll);

            tvOptionText.setText(optionText);

            String optionKey = "option_" + i;
            long optionVotes = votes.containsKey(optionKey) ? votes.get(optionKey) : 0;
            int percent = totalVotes > 0 ? (int) ((optionVotes * 100) / totalVotes) : 0;
            tvOptionPercent.setText(percent + "%");
            android.view.ViewGroup.LayoutParams lp = progressView.getLayoutParams();
            lp.width = dp(percent * 2);
            progressView.setLayoutParams(lp);

            int finalI = i;
            optionView.setOnClickListener(v -> votePoll(optionKey, finalI));
            pollContainer.addView(optionView);
        }
    }

    private void votePoll(String optionKey, int optionIndex) {
        DatabaseReference pollVoteRef = db.getPostRef(postId)
                .child("pollVotes").child(optionKey).child(currentUid);
        pollVoteRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    pollVoteRef.removeValue();
                } else {
                    pollVoteRef.setValue(true);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // ==================================================================
    // Follow
    // ==================================================================

    private void checkFollowStatus() {
        if (currentPost == null) return;
        String targetUid = currentPost.getUid();
        db.getReference(Constants.FOLLOWERS).child(targetUid).child(currentUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        boolean isFollowing = snapshot.exists();
                        if (isFollowing) {
                            binding.btnFollowUser.setText(R.string.following);
                            binding.btnFollowUser.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                        } else {
                            binding.btnFollowUser.setText(R.string.follow);
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void toggleFollow() {
        if (currentPost == null || isPostOwner) return;
        String targetUid = currentPost.getUid();
        DatabaseReference followerRef = db.getReference(Constants.FOLLOWERS).child(targetUid).child(currentUid);
        DatabaseReference followingRef = db.getReference(Constants.FOLLOWING).child(currentUid).child(targetUid);

        followerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    followerRef.removeValue();
                    followingRef.removeValue();
                    binding.btnFollowUser.setText(R.string.follow);
                } else {
                    Map<String, Object> followData = new HashMap<>();
                    followData.put("uid", currentUid);
                    followData.put("timestamp", System.currentTimeMillis());
                    followerRef.setValue(followData);
                    followingRef.setValue(followData);
                    binding.btnFollowUser.setText(R.string.following);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // ==================================================================
    // More Options
    // ==================================================================

    private void showMoreOptions() {
        if (!isPostOwner) return;
        String[] options = {"تعديل", "حذف", "تثبيت", "أرشفة", "نسخ الرابط"};
        new AlertDialog.Builder(this)
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: editPost(); break;
                        case 1: deletePost(); break;
                        case 2: togglePin(); break;
                        case 3: toggleArchive(); break;
                        case 4: copyPostLink(); break;
                    }
                })
                .show();
    }

    private void editPost() {
        if (currentPost == null) return;
        Bundle bundle = new Bundle();
        bundle.putString("editPostId", currentPost.getPostId());
        bundle.putString("editText", currentPost.getText());
        bundle.putString("editImageUrl", currentPost.getImageUrl());
        openActivity(CreatePostActivity.class, bundle);
    }

    private void deletePost() {
        new AlertDialog.Builder(this)
                .setTitle("حذف المنشور")
                .setMessage("هل أنت متأكد من حذف هذا المنشور؟ لا يمكن التراجع عن هذا الإجراء.")
                .setPositiveButton("حذف", (d, w) -> {
                    showLoading();
                    db.getPostRef(postId).removeValue();
                    db.getReference(Constants.LIKES).child(postId).removeValue();
                    db.getCommentsRef(postId).removeValue();
                    db.getReference(Constants.MEDIA).child(postId).removeValue()
                            .addOnSuccessListener(v -> {
                                hideLoading();
                                showMessage("تم حذف المنشور");
                                setResult(RESULT_OK);
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                hideLoading();
                                showError("فشل حذف المنشور");
                            });
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void togglePin() {
        if (currentPost == null) return;
        boolean newPinState = !currentPost.isPinned();
        db.getPostRef(postId).child("isPinned").setValue(newPinState)
                .addOnSuccessListener(v ->
                        showMessage(newPinState ? "تم تثبيت المنشور" : "تم إلغاء التثبيت"));
    }

    private void toggleArchive() {
        if (currentPost == null) return;
        boolean newArchiveState = !currentPost.isArchived();
        db.getPostRef(postId).child("isArchived").setValue(newArchiveState)
                .addOnSuccessListener(v ->
                        showMessage(newArchiveState ? "تم أرشفة المنشور" : "تم إلغاء الأرشفة"));
    }

    private void copyPostLink() {
        String deepLink = "https://kimo.app/posts/" + postId;
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            ClipData clip = ClipData.newPlainText("post_link", deepLink);
            clipboard.setPrimaryClip(clip);
            showMessage("تم نسخ الرابط");
        }
    }

    // ==================================================================
    // View Count
    // ==================================================================

    private void incrementViewCount() {
        db.getPostRef(postId).child("viewsCount").runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                Long val = currentData.getValue(Long.class);
                if (val == null) val = 0L;
                currentData.setValue(val + 1);
                return Transaction.success(currentData);
            }
            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {}
        });
    }

    // ==================================================================
    // Keyboard Handling
    // ==================================================================

    private void setupKeyboardHandling() {
        binding.getRoot().getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            int heightDiff = binding.getRoot().getRootView().getHeight() - binding.getRoot().getHeight();
            if (heightDiff > dp(200)) {
                // Keyboard is open
                binding.bottomBar.setPadding(0, 0, 0, dp(4));
            } else {
                binding.bottomBar.setPadding(0, 0, 0, dp(8));
            }
        });
    }

    // ==================================================================
    // Lifecycle
    // ==================================================================

    @Override
    protected void onResume() {
        super.onResume();
        if (exoPlayer != null && !exoPlayer.isPlaying()) {
            exoPlayer.play();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (exoPlayer != null) exoPlayer.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releasePlayer();
        if (postListener != null) db.getPostRef(postId).removeEventListener(postListener);
        if (commentChildListener != null) db.getCommentsRef(postId).removeEventListener(commentChildListener);
        if (likesListener != null) {
            db.getReference(Constants.LIKES).child(postId).child(currentUid).removeEventListener(likesListener);
        }
        if (savedListener != null) db.getSavedPostsRef(currentUid).child(postId).removeEventListener(savedListener);
    }

    @Override
    public void onBackPressed() {
        releasePlayer();
        setResult(RESULT_OK);
        super.onBackPressed();
    }
}
