package com.news.kimo.ui.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.GridView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.news.kimo.R;
import com.news.kimo.adapters.PostAdapter;
import com.news.kimo.adapters.MediaGridAdapter;
import com.news.kimo.database.FirestoreHelper;
import com.news.kimo.databinding.ActivityMyProfileBinding;
import com.news.kimo.models.Post;
import com.news.kimo.models.SavedPost;
import com.news.kimo.models.User;
import com.news.kimo.utils.Constants;
import com.news.kimo.utils.SessionManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MyProfileActivity extends BaseActivity {

    private ActivityMyProfileBinding binding;
    private FirestoreHelper db;
    private FirebaseAuth mAuth;
    private SessionManager sessionManager;

    private String currentUid;
    private User currentUser;
    private ValueEventListener userListener;

    // Tabs
    private static final String TAB_POSTS = "المنشورات";
    private static final String TAB_SAVED = "المحفوظات";
    private static final String TAB_MEDIA = "الوسائط";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMyProfileBinding.inflate(getLayoutInflater());
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
        setupParallaxCover();
        loadCurrentUser();
        setupTabsWithViewPager();
        setupActionButtons();
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
    // Parallax Cover
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
    // Load Current User
    // ==================================================================

    private void loadCurrentUser() {
        currentUser = sessionManager.loadCurrentUser();
        if (currentUser != null) {
            bindUserUI(currentUser);
        }

        userListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    currentUser = user;
                    sessionManager.saveCurrentUser(user);
                    bindUserUI(user);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showError(error.getMessage());
            }
        };
        db.getUserRef(currentUid).addValueEventListener(userListener);
    }

    private void bindUserUI(User user) {
        if (user == null) return;

        // Cover
        if (!TextUtils.isEmpty(user.getCoverUrl())) {
            loadImage(user.getCoverUrl(), binding.ivCoverPhoto);
        }

        // Avatar
        loadProfileImage(user.getPhotoUrl(), binding.ivAvatar, user.getName());

        // Name + verified
        binding.tvName.setText(user.getName());
        binding.ivVerifiedBadge.setVisibility(user.isVerified() ? View.VISIBLE : View.GONE);

        // Bio
        if (!TextUtils.isEmpty(user.getBio())) {
            binding.tvBio.setVisibility(View.VISIBLE);
            binding.tvBio.setText(user.getBio());
        } else {
            binding.tvBio.setVisibility(View.GONE);
        }

        // Location
        StringBuilder location = new StringBuilder();
        if (!TextUtils.isEmpty(user.getCountry())) location.append(user.getCountry());
        if (!TextUtils.isEmpty(user.getCity())) {
            if (location.length() > 0) location.append(", ");
            location.append(user.getCity());
        }
        if (location.length() > 0) {
            binding.tvLocation.setVisibility(View.VISIBLE);
            binding.tvLocation.setText(location.toString());
        } else {
            binding.tvLocation.setVisibility(View.GONE);
        }

        // Website
        if (!TextUtils.isEmpty(user.getWebsite())) {
            binding.tvWebsite.setVisibility(View.VISIBLE);
            binding.tvWebsite.setText(user.getWebsite());
            binding.tvWebsite.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(user.getWebsite()));
                startActivity(intent);
            });
        } else {
            binding.tvWebsite.setVisibility(View.GONE);
        }

        // Join date
        if (user.getCreatedAt() > 0) {
            Calendar cal = Calendar.getInstance(Locale.getDefault());
            cal.setTimeInMillis(user.getCreatedAt());
            String joinDate = String.format(Locale.getDefault(), "%s %d",
                    cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()),
                    cal.get(Calendar.YEAR));
            binding.tvJoinDate.setText("انضم في " + joinDate);
            binding.tvJoinDate.setVisibility(View.VISIBLE);
        } else {
            binding.tvJoinDate.setVisibility(View.GONE);
        }

        // Stats (4 columns)
        binding.tvPostsCount.setText(formatCount(user.getPostCount()));
        binding.tvFollowersCount.setText(formatCount(user.getFollowersCount()));
        binding.tvFollowingCount.setText(formatCount(user.getFollowingCount()));
        binding.tvLikesCount.setText(formatCount(user.getLikesCount()));

        // Toolbar title
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(user.getName());
        }
    }

    private String formatCount(long count) {
        if (count >= 1000000) return (count / 1000000) + "M";
        if (count >= 1000) return (count / 1000) + "K";
        return String.valueOf(count);
    }

    // ==================================================================
    // Action Buttons
    // ==================================================================

    private void setupActionButtons() {
        // Edit profile
        binding.btnEditProfile.setOnClickListener(v -> openActivity(EditProfileActivity.class));

        // Settings
        binding.btnSettings.setOnClickListener(v -> openActivity(SettingsActivity.class));

        // Share profile
        binding.btnShareProfile.setOnClickListener(v -> {
            String profileLink = "https://kimo.app/profile/" + currentUid;
            String name = currentUser != null ? currentUser.getName() : "";
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, name + "\n" + profileLink);
            startActivity(Intent.createChooser(shareIntent, "مشاركة الملف الشخصي"));
        });

        // Stats click
        binding.tvFollowersCount.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString(Constants.EXTRA_USER_ID, currentUid);
            bundle.putString("tab", "followers");
            // openActivity(FollowersListActivity.class, bundle);
        });

        binding.tvFollowingCount.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString(Constants.EXTRA_USER_ID, currentUid);
            bundle.putString("tab", "following");
            // openActivity(FollowersListActivity.class, bundle);
        });
    }

    // ==================================================================
    // Tabs + ViewPager
    // ==================================================================

    private void setupTabsWithViewPager() {
        MyProfilePagerAdapter adapter = new MyProfilePagerAdapter(getSupportFragmentManager());
        binding.viewPager.setAdapter(adapter);
        binding.tabLayout.setupWithViewPager(binding.viewPager);
    }

    private class MyProfilePagerAdapter extends FragmentPagerAdapter {

        MyProfilePagerAdapter(@NonNull FragmentManager fm) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            Bundle args = new Bundle();
            args.putString(Constants.EXTRA_USER_ID, currentUid);
            switch (position) {
                case 0: return MyPostsFragment.newInstance(args);
                case 1: return SavedPostsFragment.newInstance(args);
                case 2: return MyMediaFragment.newInstance(args);
                default: return MyPostsFragment.newInstance(args);
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
                case 1: return TAB_SAVED;
                case 2: return TAB_MEDIA;
                default: return TAB_POSTS;
            }
        }
    }

    // ==================================================================
    // Lifecycle
    // ==================================================================

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh user data on resume (in case profile was edited)
        db.getUserRef(currentUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    currentUser = user;
                    sessionManager.saveCurrentUser(user);
                    bindUserUI(user);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (userListener != null) db.getUserRef(currentUid).removeEventListener(userListener);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull android.view.MenuItem item) {
        if (item.getItemId() == R.id.action_sign_out) {
            showSignOutDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showSignOutDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.sign_out)
                .setMessage("هل أنت متأكد من تسجيل الخروج؟")
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    mAuth.signOut();
                    sessionManager.clearAllSessionData();
                    openActivity(LoginActivity.class);
                    finishAffinity();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}

// ======================================================================
// Inner Fragment classes for own profile tabs
// ======================================================================

class MyPostsFragment extends Fragment {

    private String targetUid;
    private RecyclerView rvPosts;
    private PostAdapter postAdapter;
    private final List<Post> postList = new ArrayList<>();

    static MyPostsFragment newInstance(Bundle args) {
        MyPostsFragment f = new MyPostsFragment();
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            targetUid = getArguments().getString(Constants.EXTRA_USER_ID);
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

class SavedPostsFragment extends Fragment {

    private String targetUid;
    private RecyclerView rvPosts;
    private PostAdapter postAdapter;
    private final List<Post> postList = new ArrayList<>();

    static SavedPostsFragment newInstance(Bundle args) {
        SavedPostsFragment f = new SavedPostsFragment();
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            targetUid = getArguments().getString(Constants.EXTRA_USER_ID);
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
        loadSavedPosts();
        return view;
    }

    private void loadSavedPosts() {
        if (TextUtils.isEmpty(targetUid)) return;
        FirestoreHelper.getInstance().getSavedPostsRef(targetUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        postList.clear();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            String postId = ds.child("postId").getValue(String.class);
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
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }
}

class MyMediaFragment extends Fragment {

    private String targetUid;
    private GridView gvMedia;
    private final List<String> mediaUrls = new ArrayList<>();

    static MyMediaFragment newInstance(Bundle args) {
        MyMediaFragment f = new MyMediaFragment();
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            targetUid = getArguments().getString(Constants.EXTRA_USER_ID);
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
