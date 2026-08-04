package com.news.kimo.ui.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.news.kimo.R;
import com.news.kimo.adapters.PostAdapter;
import com.news.kimo.adapters.MediaGridAdapter;
import com.news.kimo.database.FirestoreHelper;
import com.news.kimo.databinding.ActivityProfileBinding;
import com.news.kimo.models.Post;
import com.news.kimo.models.User;
import com.news.kimo.utils.Constants;
import com.news.kimo.utils.DateUtils;
import com.news.kimo.utils.SessionManager;
import com.news.kimo.utils.StringUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProfileActivity extends BaseActivity {

    private ActivityProfileBinding binding;
    private FirestoreHelper db;
    private FirebaseAuth mAuth;
    private SessionManager sessionManager;

    private String targetUid;
    private String currentUid;
    private User targetUser;
    private User currentUser;

    private boolean isFollowing = false;
    private boolean isBlocked = false;
    private boolean isPrivateAndNotFollowing = false;
    private boolean isFollowRequestPending = false;

    private ValueEventListener targetUserListener;
    private ValueEventListener followStatusListener;

    // Tabs
    private static final String TAB_POSTS = "المنشورات";
    private static final String TAB_MEDIA = "الوسائط";
    private static final String TAB_LIKES = "الإعجابات";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
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

        targetUid = getIntent().getStringExtra(Constants.EXTRA_USER_ID);
        if (TextUtils.isEmpty(targetUid)) {
            showError("معرف المستخدم مطلوب");
            finish();
            return;
        }

        if (targetUid.equals(currentUid)) {
            // Open own profile
            openActivity(MyProfileActivity.class);
            finish();
            return;
        }

        setupToolbar();
        setupParallaxCover();
        loadTargetUser();
        checkFollowStatus();
        checkBlockStatus();
        setupTabsWithViewPager();
        setupMenuButton();
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
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    // ==================================================================
    // Parallax Cover Photo
    // ==================================================================

    private void setupParallaxCover() {
        binding.scrollView.setOnScrollChangeListener((NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) -> {
            float parallax = scrollY * 0.5f;
            binding.ivCoverPhoto.setTranslationY(-parallax);

            float alpha = 1f - (scrollY / (float) binding.ivCoverPhoto.getHeight());
            if (alpha < 0) alpha = 0;
            binding.viewCoverOverlay.setAlpha(alpha);
        });
    }

    // ==================================================================
    // Load Target User
    // ==================================================================

    private void loadTargetUser() {
        showLoading();
        targetUserListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                hideLoading();
                targetUser = snapshot.getValue(User.class);
                if (targetUser == null) {
                    showError("المستخدم غير موجود");
                    return;
                }
                bindUserUI();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                hideLoading();
                showError(error.getMessage());
            }
        };
        db.getUserRef(targetUid).addValueEventListener(targetUserListener);
    }

    private void bindUserUI() {
        if (targetUser == null) return;

        // Cover photo
        if (!TextUtils.isEmpty(targetUser.getCoverUrl())) {
            loadImage(targetUser.getCoverUrl(), binding.ivCoverPhoto);
        }

        // Avatar (overlaps cover)
        loadProfileImage(targetUser.getPhotoUrl(), binding.ivAvatar, targetUser.getName());

        // Name + verified
        binding.tvName.setText(targetUser.getName());
        binding.ivVerifiedBadge.setVisibility(targetUser.isVerified() ? View.VISIBLE : View.GONE);

        // Bio
        if (!TextUtils.isEmpty(targetUser.getBio())) {
            binding.tvBio.setVisibility(View.VISIBLE);
            binding.tvBio.setText(targetUser.getBio());
        } else {
            binding.tvBio.setVisibility(View.GONE);
        }

        // Location
        StringBuilder location = new StringBuilder();
        if (!TextUtils.isEmpty(targetUser.getCountry())) location.append(targetUser.getCountry());
        if (!TextUtils.isEmpty(targetUser.getCity())) {
            if (location.length() > 0) location.append(", ");
            location.append(targetUser.getCity());
        }
        if (location.length() > 0) {
            binding.tvLocation.setVisibility(View.VISIBLE);
            binding.tvLocation.setText(location.toString());
        } else {
            binding.tvLocation.setVisibility(View.GONE);
        }

        // Website
        if (!TextUtils.isEmpty(targetUser.getWebsite())) {
            binding.tvWebsite.setVisibility(View.VISIBLE);
            binding.tvWebsite.setText(targetUser.getWebsite());
            binding.tvWebsite.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(targetUser.getWebsite()));
                startActivity(intent);
            });
        } else {
            binding.tvWebsite.setVisibility(View.GONE);
        }

        // Social links
        if (targetUser.getSocialLinks() != null && !targetUser.getSocialLinks().isEmpty()) {
            binding.layoutSocialLinks.setVisibility(View.VISIBLE);
            binding.layoutSocialLinks.removeAllViews();
            for (Map.Entry<String, String> entry : targetUser.getSocialLinks().entrySet()) {
                if (TextUtils.isEmpty(entry.getValue())) continue;
                TextView tv = new TextView(this);
                tv.setText(entry.getKey() + ": " + entry.getValue());
                tv.setTextSize(12);
                tv.setTextColor(getColor(R.color.colorPrimary));
                tv.setPadding(dp(8), dp(4), dp(8), dp(4));
                int finalIdx = 0;
                tv.setOnClickListener(vv -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(entry.getValue()));
                    startActivity(intent);
                });
                binding.layoutSocialLinks.addView(tv);
            }
        } else {
            binding.layoutSocialLinks.setVisibility(View.GONE);
        }

        // Join date
        if (targetUser.getCreatedAt() > 0) {
            Calendar cal = Calendar.getInstance(Locale.getDefault());
            cal.setTimeInMillis(targetUser.getCreatedAt());
            String joinDate = String.format(Locale.getDefault(), "%s %d",
                    cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()),
                    cal.get(Calendar.YEAR));
            binding.tvJoinDate.setText("انضم في " + joinDate);
            binding.tvJoinDate.setVisibility(View.VISIBLE);
        } else {
            binding.tvJoinDate.setVisibility(View.GONE);
        }

        // Stats
        binding.tvPostsCount.setText(String.valueOf(targetUser.getPostCount()));
        binding.tvFollowersCount.setText(formatCount(targetUser.getFollowersCount()));
        binding.tvFollowingCount.setText(formatCount(targetUser.getFollowingCount()));
        binding.tvLikesCount.setText(formatCount(targetUser.getLikesCount()));

        // Toolbar title
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(targetUser.getName());
        }

        // Private account check
        if (targetUser.isPrivate() && !isFollowing) {
            isPrivateAndNotFollowing = true;
            binding.layoutPrivateMessage.setVisibility(View.VISIBLE);
            binding.viewPager.setVisibility(View.GONE);
            binding.tabLayout.setVisibility(View.GONE);
        } else {
            isPrivateAndNotFollowing = false;
            binding.layoutPrivateMessage.setVisibility(View.GONE);
            binding.viewPager.setVisibility(View.VISIBLE);
            binding.tabLayout.setVisibility(View.VISIBLE);
        }
    }

    private String formatCount(long count) {
        if (count >= 1000000) return (count / 1000000) + "M";
        if (count >= 1000) return (count / 1000) + "K";
        return String.valueOf(count);
    }

    // ==================================================================
    // Follow System
    // ==================================================================

    private void checkFollowStatus() {
        followStatusListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    isFollowing = true;
                    isFollowRequestPending = false;
                    binding.btnFollow.setText(R.string.following);
                    binding.btnFollow.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                    binding.btnFollow.setAlpha(1f);
                } else {
                    isFollowing = false;
                    binding.btnFollow.setText(R.string.follow);
                    binding.btnFollow.setAlpha(1f);
                }
                // Update private account visibility
                if (targetUser != null && targetUser.isPrivate() && !isFollowing) {
                    isPrivateAndNotFollowing = true;
                    binding.layoutPrivateMessage.setVisibility(View.VISIBLE);
                    binding.viewPager.setVisibility(View.GONE);
                    binding.tabLayout.setVisibility(View.GONE);
                } else {
                    isPrivateAndNotFollowing = false;
                    binding.layoutPrivateMessage.setVisibility(View.GONE);
                    binding.viewPager.setVisibility(View.VISIBLE);
                    binding.tabLayout.setVisibility(View.VISIBLE);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        db.getReference(Constants.FOLLOWERS).child(targetUid).child(currentUid)
                .addValueEventListener(followStatusListener);
    }

    private void toggleFollow() {
        if (targetUser == null || isBlocked) return;
        DatabaseReference followerRef = db.getReference(Constants.FOLLOWERS).child(targetUid).child(currentUid);
        DatabaseReference followingRef = db.getReference(Constants.FOLLOWING).child(currentUid).child(targetUid);

        followerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Unfollow
                    followerRef.removeValue();
                    followingRef.removeValue();
                    binding.btnFollow.setText(R.string.follow);
                    isFollowing = false;
                } else {
                    // Follow
                    if (targetUser.isPrivate()) {
                        // Send follow request (pending state)
                        Map<String, Object> requestData = new HashMap<>();
                        requestData.put("uid", currentUid);
                        requestData.put("timestamp", System.currentTimeMillis());
                        requestData.put("status", "pending");
                        followerRef.setValue(requestData);
                        binding.btnFollow.setText("معلق");
                        isFollowRequestPending = true;
                        showMessage("تم إرسال طلب المتابعة");
                    } else {
                        Map<String, Object> followData = new HashMap<>();
                        followData.put("uid", currentUid);
                        followData.put("timestamp", System.currentTimeMillis());
                        followerRef.setValue(followData);
                        followingRef.setValue(followData);
                        binding.btnFollow.setText(R.string.following);
                        isFollowing = true;
                    }

                    // Update counters
                    db.getUserRef(targetUid).child("followersCount").runTransaction(new Transaction.Handler() {
                        @NonNull
                        @Override
                        public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                            Long val = currentData.getValue(Long.class);
                            if (val == null) val = 0L;
                            currentData.setValue(isFollowing || isFollowRequestPending ? val + 1 : Math.max(0, val - 1));
                            return Transaction.success(currentData);
                        }
                        @Override
                        public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot d) {}
                    });
                    db.getUserRef(currentUid).child("followingCount").runTransaction(new Transaction.Handler() {
                        @NonNull
                        @Override
                        public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                            Long val = currentData.getValue(Long.class);
                            if (val == null) val = 0L;
                            currentData.setValue(isFollowing || isFollowRequestPending ? val + 1 : Math.max(0, val - 1));
                            return Transaction.success(currentData);
                        }
                        @Override
                        public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot d) {}
                    });
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setupFollowButton() {
        binding.btnFollow.setOnClickListener(v -> toggleFollow());
    }

    // ==================================================================
    // Block System
    // ==================================================================

    private void checkBlockStatus() {
        db.getReference(Constants.BLOCKS).child(currentUid).child(targetUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        isBlocked = snapshot.exists();
                        if (isBlocked) {
                            binding.btnFollow.setVisibility(View.GONE);
                        } else {
                            setupFollowButton();
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void blockUser() {
        DatabaseReference blockRef = db.getReference(Constants.BLOCKS).child(currentUid).child(targetUid);
        blockRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Unblock
                    blockRef.removeValue();
                    // Re-follow if needed
                    isBlocked = false;
                    showMessage("تم إلغاء الحظر");
                    binding.btnFollow.setVisibility(View.VISIBLE);
                } else {
                    // Block
                    Map<String, Object> blockData = new HashMap<>();
                    blockData.put("uid", targetUid);
                    blockData.put("blockedAt", System.currentTimeMillis());
                    blockRef.setValue(blockData);
                    isBlocked = true;
                    showMessage("تم حظر المستخدم");
                    binding.btnFollow.setVisibility(View.GONE);
                    // Remove follow relationship
                    db.getReference(Constants.FOLLOWERS).child(targetUid).child(currentUid).removeValue();
                    db.getReference(Constants.FOLLOWING).child(currentUid).child(targetUid).removeValue();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // ==================================================================
    // Menu
    // ==================================================================

    private void setupMenuButton() {
        binding.ivMenu.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(this, v);
            popup.getMenu().add(0, 0, 0, "حظر المستخدم");
            popup.getMenu().add(0, 1, 0, "الإبلاغ عن المستخدم");
            popup.getMenu().add(0, 2, 0, "مشاركة الملف الشخصي");
            popup.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    case 0: blockUser(); break;
                    case 1: reportUser(); break;
                    case 2: shareProfile(); break;
                }
                return true;
            });
            popup.show();
        });
    }

    private void reportUser() {
        if (targetUser == null) return;
        String[] reportTypes = {
                "إساءة", "محتوى مسيء", "انتحال شخصية", "بريد مزعج", "أخرى"
        };
        new AlertDialog.Builder(this)
                .setTitle("الإبلاغ عن " + targetUser.getName())
                .setItems(reportTypes, (dialog, which) -> {
                    String reportId = db.getReference(Constants.REPORTS).push().getKey();
                    if (reportId == null) return;
                    Map<String, Object> report = new HashMap<>();
                    report.put("reportId", reportId);
                    report.put("reporterUid", currentUid);
                    report.put("reportedUid", targetUid);
                    report.put("type", reportTypes[which]);
                    report.put("timestamp", System.currentTimeMillis());
                    db.getReference(Constants.REPORTS).child(reportId).setValue(report)
                            .addOnSuccessListener(v -> showMessage("تم إرسال البلاغ"))
                            .addOnFailureListener(e -> showError("فشل إرسال البلاغ"));
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void shareProfile() {
        if (targetUser == null) return;
        String profileLink = "https://kimo.app/profile/" + targetUid;
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, targetUser.getName() + "\n" + profileLink);
        startActivity(Intent.createChooser(shareIntent, "مشاركة الملف الشخصي"));
    }

    // ==================================================================
    // Tabs + ViewPager
    // ==================================================================

    private void setupTabsWithViewPager() {
        ProfilePagerAdapter adapter = new ProfilePagerAdapter(getSupportFragmentManager());
        binding.viewPager.setAdapter(adapter);
        binding.tabLayout.setupWithViewPager(binding.viewPager);
    }

    private class ProfilePagerAdapter extends FragmentPagerAdapter {

        ProfilePagerAdapter(@NonNull FragmentManager fm) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            Bundle args = new Bundle();
            args.putString(Constants.EXTRA_USER_ID, targetUid);
            switch (position) {
                case 0:
                    return ProfilePostsFragment.newInstance(args);
                case 1:
                    return ProfileMediaFragment.newInstance(args);
                case 2:
                    return ProfileLikesFragment.newInstance(args);
                default:
                    return ProfilePostsFragment.newInstance(args);
            }
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0: return TAB_POSTS;
                case 1: return TAB_MEDIA;
                case 2: return TAB_LIKES;
                default: return TAB_POSTS;
            }
        }
    }

    // ==================================================================
    // Lifecycle
    // ==================================================================

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (targetUserListener != null) db.getUserRef(targetUid).removeEventListener(targetUserListener);
        if (followStatusListener != null) {
            db.getReference(Constants.FOLLOWERS).child(targetUid).child(currentUid)
                    .removeEventListener(followStatusListener);
        }
    }
}

// ======================================================================
// Placeholder Fragment base (inner classes for tab content)
// ======================================================================

class ProfilePostsFragment extends Fragment {

    private static final String ARG_UID = "uid";
    private String targetUid;
    private RecyclerView rvPosts;
    private PostAdapter postAdapter;
    private final List<Post> postList = new ArrayList<>();

    static ProfilePostsFragment newInstance(Bundle args) {
        ProfilePostsFragment f = new ProfilePostsFragment();
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            targetUid = getArguments().getString(ARG_UID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_posts_list, container, false);
        rvPosts = view.findViewById(R.id.rvPosts);
        rvPosts.setLayoutManager(new LinearLayoutManager(requireContext()));
        postAdapter = new PostAdapter(requireContext(), postList);
        rvPosts.setAdapter(postAdapter);
        loadPosts();
        return view;
    }

    private void loadPosts() {
        if (TextUtils.isEmpty(targetUid)) return;
        FirestoreHelper.getInstance().getReference(Constants.POSTS)
                .orderByChild("uid").equalTo(targetUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        postList.clear();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            Post post = ds.getValue(Post.class);
                            if (post != null && !post.isArchived()) {
                                postList.add(post);
                            }
                        }
                        postAdapter.notifyDataSetChanged();
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }
}

class ProfileMediaFragment extends Fragment {

    private static final String ARG_UID = "uid";
    private String targetUid;
    private GridView gvMedia;
    private final List<String> mediaUrls = new ArrayList<>();

    static ProfileMediaFragment newInstance(Bundle args) {
        ProfileMediaFragment f = new ProfileMediaFragment();
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            targetUid = getArguments().getString(ARG_UID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_media_grid, container, false);
        gvMedia = view.findViewById(R.id.gvMedia);
        loadMedia();
        return view;
    }

    private void loadMedia() {
        if (TextUtils.isEmpty(targetUid)) return;
        FirestoreHelper.getInstance().getReference(Constants.POSTS)
                .orderByChild("uid").equalTo(targetUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        mediaUrls.clear();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            Post post = ds.getValue(Post.class);
                            if (post != null) {
                                if (!TextUtils.isEmpty(post.getImageUrl())) {
                                    mediaUrls.add(post.getImageUrl());
                                }
                                if (post.getImages() != null) {
                                    mediaUrls.addAll(post.getImages());
                                }
                            }
                        }
                        MediaGridAdapter adapter = new MediaGridAdapter(requireContext(), mediaUrls);
                        gvMedia.setAdapter(adapter);
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }
}

class ProfileLikesFragment extends Fragment {

    private static final String ARG_UID = "uid";
    private String targetUid;
    private RecyclerView rvPosts;
    private PostAdapter postAdapter;
    private final List<Post> postList = new ArrayList<>();

    static ProfileLikesFragment newInstance(Bundle args) {
        ProfileLikesFragment f = new ProfileLikesFragment();
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            targetUid = getArguments().getString(ARG_UID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_posts_list, container, false);
        rvPosts = view.findViewById(R.id.rvPosts);
        rvPosts.setLayoutManager(new LinearLayoutManager(requireContext()));
        postAdapter = new PostAdapter(requireContext(), postList);
        rvPosts.setAdapter(postAdapter);
        loadLikedPosts();
        return view;
    }

    private void loadLikedPosts() {
        if (TextUtils.isEmpty(targetUid)) return;
        // Query likes by this user, then fetch corresponding posts
        FirestoreHelper.getInstance().getReference(Constants.LIKES)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        postList.clear();
                        for (DataSnapshot postSnap : snapshot.getChildren()) {
                            if (postSnap.child(targetUid).exists()) {
                                String postId = postSnap.getKey();
                                if (postId == null) continue;
                                FirestoreHelper.getInstance().getPostRef(postId)
                                        .addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot postSnapshot) {
                                                Post post = postSnapshot.getValue(Post.class);
                                                if (post != null) {
                                                    postList.add(post);
                                                    postAdapter.notifyItemInserted(postList.size() - 1);
                                                }
                                            }
                                            @Override
                                            public void onCancelled(@NonNull DatabaseError error) {}
                                        });
                            }
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }
}