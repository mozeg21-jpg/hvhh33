package com.news.kimo.ui.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.bumptech.glide.Glide;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.news.kimo.R;
import com.news.kimo.adapters.PostAdapter;
import com.news.kimo.adapters.MediaGridAdapter;
import com.news.kimo.databinding.FragmentProfileBinding;
import com.news.kimo.models.Post;
import com.news.kimo.models.SavedPost;
import com.news.kimo.models.User;
import com.news.kimo.ui.activities.EditProfileActivity;
import com.news.kimo.ui.activities.FollowingActivity;
import com.news.kimo.ui.activities.PostDetailsActivity;
import com.news.kimo.ui.activities.SavedPostsActivity;
import com.news.kimo.ui.activities.SettingsActivity;
import com.news.kimo.utils.Constants;
import com.news.kimo.utils.SessionManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Profile fragment for the bottom navigation bar.
 * Shows the current user's profile with cover photo, avatar, stats,
 * and tabs for posts, saved, and media. Supports edit profile,
 * settings navigation, share profile, and real-time data refresh.
 */
public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";
    private static final String TAB_POSTS = "المنشورات";
    private static final String TAB_SAVED = "المحفوظات";
    private static final String TAB_MEDIA = "الوسائط";

    private FragmentProfileBinding binding;
    private DatabaseReference rootRef;
    private FirebaseAuth auth;
    private SessionManager sessionManager;
    private String currentUid;
    private User currentUser;

    private ValueEventListener userListener;

    // UI references
    private ImageView ivCoverPhoto, ivAvatar, ivVerifiedBadge;
    private TextView tvName, tvBio, tvLocation, tvWebsite, tvJoinDate;
    private TextView tvPostsCount, tvFollowersCount, tvFollowingCount, tvLikesCount;
    private LinearLayout btnEditProfile, btnSettings, btnShareProfile;
    private TabLayout tabLayout;
    private ViewPager viewPager;
    private NestedScrollView scrollView;

    public static ProfileFragment getInstance() {
        return new ProfileFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        rootRef = FirebaseDatabase.getInstance().getReference();
        auth = FirebaseAuth.getInstance();
        sessionManager = SessionManager.getInstance(requireContext());
        currentUid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews();
        setupParallaxCover();
        setupTabs();
        setupActionButtons();
        loadCurrentUser();
    }

    // ==================================================================
    // Initialisation
    // ==================================================================

    private void initViews() {
        ivCoverPhoto = binding.ivCoverPhoto;
        ivAvatar = binding.ivAvatar;
        ivVerifiedBadge = binding.ivVerifiedBadge;
        tvName = binding.tvName;
        tvBio = binding.tvBio;
        tvLocation = binding.tvLocation;
        tvWebsite = binding.tvWebsite;
        tvJoinDate = binding.tvJoinDate;
        tvPostsCount = binding.tvPostsCount;
        tvFollowersCount = binding.tvFollowersCount;
        tvFollowingCount = binding.tvFollowingCount;
        tvLikesCount = binding.tvLikesCount;
        btnEditProfile = binding.btnEditProfile;
        btnSettings = binding.btnSettings;
        btnShareProfile = binding.btnShareProfile;
        tabLayout = binding.tabLayout;
        viewPager = binding.viewPager;
        scrollView = binding.scrollView;
    }

    private void setupParallaxCover() {
        scrollView.setOnScrollChangeListener((NestedScrollView v, int scrollX, int scrollY,
                                                     int oldScrollX, int oldScrollY) -> {
            float parallax = scrollY * 0.5f;
            ivCoverPhoto.setTranslationY(-parallax);

            float alpha = 1f - (scrollY / (float) ivCoverPhoto.getHeight());
            if (alpha < 0) alpha = 0;
            binding.viewCoverOverlay.setAlpha(alpha);
        });
    }

    private void setupTabs() {
        ProfilePagerAdapter adapter = new ProfilePagerAdapter(getChildFragmentManager());
        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(3);
        tabLayout.setupWithViewPager(viewPager);
    }

    private void setupActionButtons() {
        // Edit profile
        btnEditProfile.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), EditProfileActivity.class)));

        // Settings
        btnSettings.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), SettingsActivity.class)));

        // Share profile
        btnShareProfile.setOnClickListener(v -> {
            String profileLink = "https://kimo.app/profile/" + currentUid;
            String name = currentUser != null ? currentUser.getName() : "";
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, name + "\n" + profileLink);
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_profile)));
        });

        // Followers count click
        tvFollowersCount.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), FollowingActivity.class);
            intent.putExtra(Constants.EXTRA_USER_ID, currentUid);
            intent.putExtra("tab", 0);
            startActivity(intent);
        });

        // Following count click
        tvFollowingCount.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), FollowingActivity.class);
            intent.putExtra(Constants.EXTRA_USER_ID, currentUid);
            intent.putExtra("tab", 1);
            startActivity(intent);
        });
    }

    // ==================================================================
    // Firebase: Load Current User
    // ==================================================================

    private void loadCurrentUser() {
        // Show cached user first
        currentUser = sessionManager.loadCurrentUser();
        if (currentUser != null) {
            bindUserUI(currentUser);
        }

        // Real-time listener
        userListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    user.setUid(snapshot.getKey());
                    currentUser = user;
                    sessionManager.saveCurrentUser(user);
                    bindUserUI(user);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "loadCurrentUser cancelled", error.toException());
            }
        };

        rootRef.child(Constants.USERS).child(currentUid)
                .addValueEventListener(userListener);
    }

    // ==================================================================
    // Bind UI
    // ==================================================================

    private void bindUserUI(User user) {
        if (user == null) return;

        // Cover photo
        if (!TextUtils.isEmpty(user.getCoverUrl())) {
            Glide.with(requireContext())
                    .load(user.getCoverUrl())
                    .centerCrop()
                    .placeholder(R.drawable.ic_placeholder_cover)
                    .into(ivCoverPhoto);
        }

        // Avatar with fallback initial
        if (!TextUtils.isEmpty(user.getPhotoUrl())) {
            Glide.with(requireContext())
                    .load(user.getPhotoUrl())
                    .circleCrop()
                    .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.ic_placeholder_avatar)
                    .error(R.drawable.ic_placeholder_avatar)
                    .into(ivAvatar);
        } else {
            // Generate letter avatar fallback
            ivAvatar.setImageDrawable(createLetterDrawable(user.getName()));
        }

        // Name + verified badge
        tvName.setText(user.getName());
        ivVerifiedBadge.setVisibility(user.isVerified() ? View.VISIBLE : View.GONE);

        // Bio
        if (!TextUtils.isEmpty(user.getBio())) {
            tvBio.setVisibility(View.VISIBLE);
            tvBio.setText(user.getBio());
        } else {
            tvBio.setVisibility(View.GONE);
        }

        // Location
        StringBuilder location = new StringBuilder();
        if (!TextUtils.isEmpty(user.getCountry())) location.append(user.getCountry());
        if (!TextUtils.isEmpty(user.getCity())) {
            if (location.length() > 0) location.append(", ");
            location.append(user.getCity());
        }
        if (location.length() > 0) {
            tvLocation.setVisibility(View.VISIBLE);
            tvLocation.setText(location.toString());
        } else {
            tvLocation.setVisibility(View.GONE);
        }

        // Website
        if (!TextUtils.isEmpty(user.getWebsite())) {
            tvWebsite.setVisibility(View.VISIBLE);
            tvWebsite.setText(user.getWebsite());
            tvWebsite.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(user.getWebsite()));
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to open website", e);
                }
            });
        } else {
            tvWebsite.setVisibility(View.GONE);
        }

        // Join date
        if (user.getCreatedAt() > 0) {
            Calendar cal = Calendar.getInstance(Locale.getDefault());
            cal.setTimeInMillis(user.getCreatedAt());
            String joinDate = String.format(Locale.getDefault(), "%s %d",
                    cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()),
                    cal.get(Calendar.YEAR));
            tvJoinDate.setText("انضم في " + joinDate);
            tvJoinDate.setVisibility(View.VISIBLE);
        } else {
            tvJoinDate.setVisibility(View.GONE);
        }

        // Stats
        tvPostsCount.setText(formatCount(user.getPostCount()));
        tvFollowersCount.setText(formatCount(user.getFollowersCount()));
        tvFollowingCount.setText(formatCount(user.getFollowingCount()));
        tvLikesCount.setText(formatCount(user.getLikesCount()));
    }

    private String formatCount(long count) {
        if (count >= 1_000_000) return (count / 1_000_000) + "M";
        if (count >= 1_000) return (count / 1_000) + "K";
        return String.valueOf(count);
    }

    /**
     * Create a circular letter drawable as avatar fallback.
     */
    private android.graphics.drawable.Drawable createLetterDrawable(String name) {
        String letter = "?";
        if (!TextUtils.isEmpty(name)) letter = name.trim().substring(0, 1);

        int size = (int) (48 * getResources().getDisplayMetrics().density);
        android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(
                size, size, android.graphics.Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);

        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        bg.setColor(requireContext().getColor(R.color.colorPrimary));
        bg.setBounds(0, 0, size, size);
        bg.draw(canvas);

        android.graphics.Paint paint = new android.graphics.Paint();
        paint.setColor(android.graphics.Color.WHITE);
        paint.setAntiAlias(true);
        paint.setTextAlign(android.graphics.Paint.Align.CENTER);
        paint.setTextSize(size * 0.45f);
        android.graphics.Rect bounds = new android.graphics.Rect();
        paint.getTextBounds(letter, 0, letter.length(), bounds);
        float y = (size / 2f) + (bounds.height() / 2f);
        canvas.drawText(letter, size / 2f, y, paint);

        return new android.graphics.drawable.BitmapDrawable(getResources(), bitmap);
    }

    // ==================================================================
    // Pager Adapter
    // ==================================================================

    private class ProfilePagerAdapter extends FragmentPagerAdapter {

        ProfilePagerAdapter(@NonNull FragmentManager fm) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            Bundle args = new Bundle();
            args.putString(Constants.EXTRA_USER_ID, currentUid);
            switch (position) {
                case 0: return ProfilePostsFragment.getInstance(args);
                case 1: return ProfileSavedFragment.getInstance(args);
                case 2: return ProfileMediaFragment.getInstance(args);
                default: return ProfilePostsFragment.getInstance(args);
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
    // Refresh on Resume
    // ==================================================================

    @Override
    public void onResume() {
        super.onResume();
        // Refresh user data in case profile was edited
        rootRef.child(Constants.USERS).child(currentUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        User user = snapshot.getValue(User.class);
                        if (user != null) {
                            user.setUid(snapshot.getKey());
                            currentUser = user;
                            sessionManager.saveCurrentUser(user);
                            bindUserUI(user);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "onResume refresh cancelled", error.toException());
                    }
                });
    }

    // ==================================================================
    // Lifecycle
    // ==================================================================

    @Override
    public void onDestroyView() {
        if (userListener != null && currentUid != null) {
            rootRef.child(Constants.USERS).child(currentUid).removeEventListener(userListener);
        }
        super.onDestroyView();
    }

    // ==================================================================
    // Inner Tab Fragments
    // ==================================================================

    /**
     * Fragment showing the current user's posts.
     */
    public static class ProfilePostsFragment extends Fragment {

        private static final String ARG_UID = "arg_uid";
        private String targetUid;
        private RecyclerView rvPosts;
        private PostAdapter postAdapter;
        private final List<Post> postList = new ArrayList<>();
        private DatabaseReference rootRef;
        private ChildEventListener postsListener;

        public static ProfilePostsFragment getInstance(Bundle args) {
            ProfilePostsFragment f = new ProfilePostsFragment();
            f.setArguments(args);
            return f;
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            rootRef = FirebaseDatabase.getInstance().getReference();
            if (getArguments() != null) {
                targetUid = getArguments().getString(ARG_UID);
            }
        }

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
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

            postsListener = new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                    Post post = snapshot.getValue(Post.class);
                    if (post != null && !post.isArchived()) {
                        post.setPostId(snapshot.getKey());
                        postList.add(0, post);
                        Collections.sort(postList, (p1, p2) ->
                                Long.compare(p2.getTimestamp(), p1.getTimestamp()));
                        postAdapter.notifyDataSetChanged();
                    }
                }

                @Override
                public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                    Post post = snapshot.getValue(Post.class);
                    if (post != null) {
                        post.setPostId(snapshot.getKey());
                        for (int i = 0; i < postList.size(); i++) {
                            if (post.getPostId().equals(postList.get(i).getPostId())) {
                                postList.set(i, post);
                                postAdapter.notifyItemChanged(i);
                                break;
                            }
                        }
                    }
                }

                @Override
                public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                    String key = snapshot.getKey();
                    for (int i = 0; i < postList.size(); i++) {
                        if (key.equals(postList.get(i).getPostId())) {
                            postList.remove(i);
                            postAdapter.notifyItemRemoved(i);
                            break;
                        }
                    }
                }

                @Override
                public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("ProfilePostsFrag", "loadPosts cancelled", error.toException());
                }
            };

            rootRef.child(Constants.POSTS)
                    .orderByChild("uid").equalTo(targetUid)
                    .addChildEventListener(postsListener);
        }

        @Override
        public void onDestroyView() {
            if (postsListener != null) {
                rootRef.child(Constants.POSTS).removeEventListener(postsListener);
            }
            super.onDestroyView();
        }
    }

    /**
     * Fragment showing the current user's saved posts.
     */
    public static class ProfileSavedFragment extends Fragment {

        private static final String ARG_UID = "arg_uid";
        private String targetUid;
        private RecyclerView rvPosts;
        private PostAdapter postAdapter;
        private final List<Post> postList = new ArrayList<>();
        private DatabaseReference rootRef;
        private ChildEventListener savedListener;

        public static ProfileSavedFragment getInstance(Bundle args) {
            ProfileSavedFragment f = new ProfileSavedFragment();
            f.setArguments(args);
            return f;
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            rootRef = FirebaseDatabase.getInstance().getReference();
            if (getArguments() != null) {
                targetUid = getArguments().getString(ARG_UID);
            }
        }

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
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

            savedListener = new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                    SavedPost sp = snapshot.getValue(SavedPost.class);
                    if (sp != null && sp.getPostId() != null) {
                        loadPostDetails(sp.getPostId());
                    }
                }

                @Override
                public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                }

                @Override
                public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                    // Find and remove the post from the list
                    String postId = snapshot.child("postId").getValue(String.class);
                    if (postId != null) {
                        for (int i = 0; i < postList.size(); i++) {
                            if (postId.equals(postList.get(i).getPostId())) {
                                postList.remove(i);
                                postAdapter.notifyItemRemoved(i);
                                break;
                            }
                        }
                    }
                }

                @Override
                public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("ProfileSavedFrag", "loadSavedPosts cancelled", error.toException());
                }
            };

            rootRef.child(Constants.SAVED_POSTS)
                    .child(targetUid)
                    .orderByChild("timestamp")
                    .addChildEventListener(savedListener);
        }

        private void loadPostDetails(String postId) {
            rootRef.child(Constants.POSTS).child(postId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            Post post = snapshot.getValue(Post.class);
                            if (post != null) {
                                post.setPostId(snapshot.getKey());
                                // Avoid duplicates
                                for (int i = 0; i < postList.size(); i++) {
                                    if (post.getPostId().equals(postList.get(i).getPostId())) return;
                                }
                                postList.add(0, post);
                                postAdapter.notifyDataSetChanged();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                        }
                    });
        }

        @Override
        public void onDestroyView() {
            if (savedListener != null) {
                rootRef.child(Constants.SAVED_POSTS)
                        .child(targetUid).removeEventListener(savedListener);
            }
            super.onDestroyView();
        }
    }

    /**
     * Fragment showing the current user's media (images/videos) in a grid.
     */
    public static class ProfileMediaFragment extends Fragment {

        private static final String ARG_UID = "arg_uid";
        private String targetUid;
        private GridView gvMedia;
        private final List<String> mediaUrls = new ArrayList<>();
        private DatabaseReference rootRef;
        private ValueEventListener mediaListener;

        public static ProfileMediaFragment getInstance(Bundle args) {
            ProfileMediaFragment f = new ProfileMediaFragment();
            f.setArguments(args);
            return f;
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            rootRef = FirebaseDatabase.getInstance().getReference();
            if (getArguments() != null) {
                targetUid = getArguments().getString(ARG_UID);
            }
        }

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_media_grid, container, false);
            gvMedia = view.findViewById(R.id.gvMedia);
            loadMedia();
            return view;
        }

        private void loadMedia() {
            if (TextUtils.isEmpty(targetUid)) return;

            mediaListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    mediaUrls.clear();
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        Post post = ds.getValue(Post.class);
                        if (post != null && !post.isArchived()) {
                            if (!TextUtils.isEmpty(post.getImageUrl())) {
                                mediaUrls.add(post.getImageUrl());
                            }
                            if (post.getImages() != null) {
                                for (String img : post.getImages()) {
                                    if (!TextUtils.isEmpty(img)) {
                                        mediaUrls.add(img);
                                    }
                                }
                            }
                        }
                    }
                    MediaGridAdapter adapter = new MediaGridAdapter(requireContext(), mediaUrls);
                    gvMedia.setAdapter(adapter);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("ProfileMediaFrag", "loadMedia cancelled", error.toException());
                }
            };

            rootRef.child(Constants.POSTS)
                    .orderByChild("uid").equalTo(targetUid)
                    .addValueEventListener(mediaListener);
        }

        @Override
        public void onDestroyView() {
            if (mediaListener != null && targetUid != null) {
                rootRef.child(Constants.POSTS)
                        .orderByChild("uid").equalTo(targetUid)
                        .removeEventListener(mediaListener);
            }
            super.onDestroyView();
        }
    }
}
