package com.news.kimo.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.news.kimo.R;
import com.news.kimo.databinding.FragmentExploreBinding;
import com.news.kimo.models.Hashtag;
import com.news.kimo.models.Post;
import com.news.kimo.models.User;
import com.news.kimo.ui.activities.ChatActivity;
import com.news.kimo.ui.activities.HashtagActivity;
import com.news.kimo.ui.activities.PostDetailsActivity;
import com.news.kimo.ui.activities.ProfileActivity;
import com.news.kimo.ui.activities.SearchActivity;
import com.news.kimo.utils.Constants;
import com.news.kimo.utils.SessionManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Explore fragment displayed from the bottom navigation.
 * Features search bar, TabLayout (الكل, المستخدمين, الوسوم, الصور),
 * trending posts, suggested users, trending hashtags, and recent images grid.
 */
public class ExploreFragment extends Fragment {

    private static final String TAG = "ExploreFragment";
    private static final int SUGGESTED_USERS_LIMIT = 15;
    private static final int TRENDING_POSTS_LIMIT = 20;
    private static final int IMAGE_GRID_SPAN = 3;

    private FragmentExploreBinding binding;
    private DatabaseReference rootRef;
    private FirebaseAuth auth;
    private SessionManager sessionManager;
    private String currentUid;

    private TabLayout tabLayout;
    private RecyclerView rvContent;
    private SwipeRefreshLayout swipeRefresh;
    private View layoutSearch, layoutEmpty;
    private TextView tvEmptyText;

    private ExploreAdapter adapter;
    private final List<Object> itemList = new ArrayList<>(); // Mixed: User, Post, Hashtag
    private final List<User> suggestedUsers = new ArrayList<>();
    private final List<Post> trendingPosts = new ArrayList<>();
    private final List<Hashtag> trendingHashtags = new ArrayList<>();
    private final List<Post> imagePosts = new ArrayList<>();

    private int currentTab = 0;
    private ValueEventListener trendingPostsListener;
    private Query trendingPostsQuery;
    private ValueEventListener suggestedUsersListener;
    private Query suggestedUsersQuery;
    private ValueEventListener hashtagsListener;
    private Query hashtagsQuery;
    private ValueEventListener imagePostsListener;
    private Query imagePostsQuery;

    public static ExploreFragment getInstance() {
        return new ExploreFragment();
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
        binding = FragmentExploreBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews();
        setupTabs();
        setupRecyclerView();
        setupSwipeRefresh();
        loadDataForTab(currentTab);
    }

    // ==================================================================
    // Initialisation
    // ==================================================================

    private void initViews() {
        layoutSearch = binding.layoutSearch;
        tabLayout = binding.tabLayout;
        rvContent = binding.rvContent;
        swipeRefresh = binding.swipeRefresh;
        layoutEmpty = binding.layoutEmpty;
        tvEmptyText = binding.tvEmptyText;

        layoutSearch.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), SearchActivity.class);
            startActivity(intent);
        });
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_all));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_users));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_hashtags));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_images));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                detachAllListeners();
                itemList.clear();
                adapter.notifyDataSetChanged();
                loadDataForTab(currentTab);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    private void setupRecyclerView() {
        if (currentTab == 3) {
            // Images tab: grid layout
            rvContent.setLayoutManager(new GridLayoutManager(requireContext(), IMAGE_GRID_SPAN));
        } else {
            rvContent.setLayoutManager(new LinearLayoutManager(requireContext()));
        }
        adapter = new ExploreAdapter();
        rvContent.setAdapter(adapter);
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setColorSchemeResources(R.color.colorPrimary);
        swipeRefresh.setOnRefreshListener(() -> {
            detachAllListeners();
            itemList.clear();
            suggestedUsers.clear();
            trendingPosts.clear();
            trendingHashtags.clear();
            imagePosts.clear();
            adapter.notifyDataSetChanged();
            loadDataForTab(currentTab);
            swipeRefresh.setRefreshing(false);
        });
    }

    // ==================================================================
    // Data Loading
    // ==================================================================

    private void loadDataForTab(int tab) {
        updateRecyclerViewLayout(tab);
        switch (tab) {
            case 0: // الكل
                loadTrendingPosts();
                loadSuggestedUsers();
                break;
            case 1: // المستخدمين
                loadSuggestedUsers();
                break;
            case 2: // الوسوم
                loadTrendingHashtags();
                break;
            case 3: // الصور
                loadImagePosts();
                break;
        }
    }

    private void updateRecyclerViewLayout(int tab) {
        if (tab == 3) {
            rvContent.setLayoutManager(new GridLayoutManager(requireContext(), IMAGE_GRID_SPAN));
        } else {
            rvContent.setLayoutManager(new LinearLayoutManager(requireContext()));
        }
    }

    // --- Trending Posts ---

    private void loadTrendingPosts() {
        trendingPostsQuery = rootRef.child(Constants.POSTS)
                .orderByChild("viewsCount")
                .limitToLast(TRENDING_POSTS_LIMIT);

        trendingPostsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                trendingPosts.clear();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    Post post = snap.getValue(Post.class);
                    if (post != null) {
                        post.setPostId(snap.getKey());
                        trendingPosts.add(post);
                    }
                }
                Collections.sort(trendingPosts, (p1, p2) -> Long.compare(p2.getViewsCount(), p1.getViewsCount()));
                mergeAllTabData();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "loadTrendingPosts cancelled", error.toException());
            }
        };
        trendingPostsQuery.addValueEventListener(trendingPostsListener);
    }

    // --- Suggested Users ---

    private void loadSuggestedUsers() {
        // Load random users that the current user doesn't follow
        rootRef.child(Constants.USERS)
                .orderByChild("postCount")
                .limitToLast(50)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<User> allUsers = new ArrayList<>();
                        for (DataSnapshot snap : snapshot.getChildren()) {
                            User user = snap.getValue(User.class);
                            if (user != null && !snap.getKey().equals(currentUid)) {
                                user.setUid(snap.getKey());
                                allUsers.add(user);
                            }
                        }
                        // Shuffle for random suggestion
                        Collections.shuffle(allUsers, new Random());
                        // Filter out already following
                        filterAlreadyFollowing(allUsers);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "loadSuggestedUsers cancelled", error.toException());
                    }
                });
    }

    private void filterAlreadyFollowing(List<User> allUsers) {
        rootRef.child(Constants.FOLLOWING).child(currentUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<String> followingUids = new ArrayList<>();
                        for (DataSnapshot snap : snapshot.getChildren()) {
                            followingUids.add(snap.getKey());
                        }
                        suggestedUsers.clear();
                        for (User user : allUsers) {
                            if (!followingUids.contains(user.getUid()) &&
                                    suggestedUsers.size() < SUGGESTED_USERS_LIMIT) {
                                suggestedUsers.add(user);
                            }
                        }
                        if (currentTab == 0) {
                            mergeAllTabData();
                        } else {
                            itemList.clear();
                            itemList.addAll(suggestedUsers);
                            adapter.notifyDataSetChanged();
                        }
                        updateEmptyState();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        suggestedUsers.clear();
                        suggestedUsers.addAll(allUsers);
                        if (suggestedUsers.size() > SUGGESTED_USERS_LIMIT) {
                            suggestedUsers.subList(SUGGESTED_USERS_LIMIT, suggestedUsers.size()).clear();
                        }
                        itemList.clear();
                        itemList.addAll(suggestedUsers);
                        adapter.notifyDataSetChanged();
                        updateEmptyState();
                    }
                });
    }

    // --- Trending Hashtags ---

    private void loadTrendingHashtags() {
        hashtagsQuery = rootRef.child(Constants.HASHTAGS)
                .orderByChild("postCount")
                .limitToLast(30);

        hashtagsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                trendingHashtags.clear();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    Hashtag hashtag = snap.getValue(Hashtag.class);
                    if (hashtag != null) {
                        hashtag.setTagId(snap.getKey());
                        trendingHashtags.add(hashtag);
                    }
                }
                Collections.sort(trendingHashtags, (h1, h2) -> Long.compare(h2.getPostCount(), h1.getPostCount()));
                itemList.clear();
                itemList.addAll(trendingHashtags);
                adapter.notifyDataSetChanged();
                updateEmptyState();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "loadTrendingHashtags cancelled", error.toException());
            }
        };
        hashtagsQuery.addValueEventListener(hashtagsListener);
    }

    // --- Image Posts ---

    private void loadImagePosts() {
        imagePostsQuery = rootRef.child(Constants.POSTS)
                .orderByChild("timestamp")
                .limitToLast(50);

        imagePostsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                imagePosts.clear();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    Post post = snap.getValue(Post.class);
                    if (post != null && post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
                        post.setPostId(snap.getKey());
                        imagePosts.add(post);
                    }
                }
                Collections.sort(imagePosts, (p1, p2) -> Long.compare(p2.getTimestamp(), p1.getTimestamp()));
                itemList.clear();
                itemList.addAll(imagePosts);
                adapter.notifyDataSetChanged();
                updateEmptyState();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "loadImagePosts cancelled", error.toException());
            }
        };
        imagePostsQuery.addValueEventListener(imagePostsListener);
    }

    // --- Merge for "All" tab ---

    private void mergeAllTabData() {
        itemList.clear();
        // Add trending posts header + posts
        itemList.add("header_trending");
        itemList.addAll(trendingPosts);
        // Add suggested users header + users
        itemList.add("header_suggested");
        itemList.addAll(suggestedUsers);
        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    // ==================================================================
    // Empty State
    // ==================================================================

    private void updateEmptyState() {
        boolean hasContent = false;
        for (Object item : itemList) {
            if (!(item instanceof String)) {
                hasContent = true;
                break;
            }
        }
        if (!hasContent) {
            layoutEmpty.setVisibility(View.VISIBLE);
            rvContent.setVisibility(View.GONE);
            tvEmptyText.setText(getEmptyMessage());
        } else {
            layoutEmpty.setVisibility(View.GONE);
            rvContent.setVisibility(View.VISIBLE);
        }
    }

    private int getEmptyMessage() {
        switch (currentTab) {
            case 1: return R.string.no_suggested_users;
            case 2: return R.string.no_trending_hashtags;
            case 3: return R.string.no_images;
            default: return R.string.no_explore_content;
        }
    }

    // ==================================================================
    // Detach Listeners
    // ==================================================================

    private void detachAllListeners() {
        if (trendingPostsListener != null && trendingPostsQuery != null) {
            trendingPostsQuery.removeEventListener(trendingPostsListener);
        }
        if (suggestedUsersListener != null && suggestedUsersQuery != null) {
            suggestedUsersQuery.removeEventListener(suggestedUsersListener);
        }
        if (hashtagsListener != null && hashtagsQuery != null) {
            hashtagsQuery.removeEventListener(hashtagsListener);
        }
        if (imagePostsListener != null && imagePostsQuery != null) {
            imagePostsQuery.removeEventListener(imagePostsListener);
        }
    }

    // ==================================================================
    // Navigation
    // ==================================================================

    private void openUserProfile(String uid) {
        Intent intent = new Intent(requireContext(), ProfileActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.EXTRA_USER_ID, uid);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    private void openPostDetails(String postId) {
        Intent intent = new Intent(requireContext(), PostDetailsActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.EXTRA_POST_ID, postId);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    private void openHashtag(String name) {
        Intent intent = new Intent(requireContext(), HashtagActivity.class);
        intent.putExtra("hashtag_name", name);
        startActivity(intent);
    }

    // ==================================================================
    // Explore Adapter
    // ==================================================================

    private class ExploreAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private static final int TYPE_HEADER = 0;
        private static final int TYPE_POST = 1;
        private static final int TYPE_USER = 2;
        private static final int TYPE_HASHTAG = 3;
        private static final int TYPE_IMAGE = 4;

        @Override
        public int getItemViewType(int position) {
            if (position < itemList.size()) {
                Object item = itemList.get(position);
                if (item instanceof String) return TYPE_HEADER;
                if (item instanceof User) return TYPE_USER;
                if (item instanceof Post) {
                    if (currentTab == 3) return TYPE_IMAGE;
                    return TYPE_POST;
                }
                if (item instanceof Hashtag) return TYPE_HASHTAG;
            }
            return TYPE_POST;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            switch (viewType) {
                case TYPE_HEADER:
                    return new HeaderViewHolder(inflater.inflate(R.layout.item_section_header, parent, false));
                case TYPE_USER:
                    return new UserViewHolder(inflater.inflate(R.layout.item_user_follow, parent, false));
                case TYPE_HASHTAG:
                    return new HashtagViewHolder(inflater.inflate(R.layout.item_hashtag, parent, false));
                case TYPE_IMAGE:
                    return new ImageViewHolder(inflater.inflate(R.layout.item_image_grid, parent, false));
                default:
                    return new PostViewHolder(inflater.inflate(R.layout.item_post_compact, parent, false));
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            Object item = itemList.get(position);
            if (holder instanceof HeaderViewHolder && item instanceof String) {
                ((HeaderViewHolder) holder).bind((String) item);
            } else if (holder instanceof UserViewHolder && item instanceof User) {
                ((UserViewHolder) holder).bind((User) item);
            } else if (holder instanceof PostViewHolder && item instanceof Post) {
                ((PostViewHolder) holder).bind((Post) item);
            } else if (holder instanceof HashtagViewHolder && item instanceof Hashtag) {
                ((HashtagViewHolder) holder).bind((Hashtag) item);
            } else if (holder instanceof ImageViewHolder && item instanceof Post) {
                ((ImageViewHolder) holder).bind((Post) item);
            }
        }

        @Override
        public int getItemCount() {
            return itemList.size();
        }
    }

    // --- ViewHolders ---

    private class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        HeaderViewHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
        }
        void bind(String header) {
            if ("header_trending".equals(header)) {
                tvTitle.setText(R.string.trending_posts);
            } else if ("header_suggested".equals(header)) {
                tvTitle.setText(R.string.suggested_users);
            }
        }
    }

    private class UserViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPhoto;
        TextView tvName, tvBio;
        android.widget.Button btnFollow;

        UserViewHolder(View itemView) {
            super(itemView);
            ivPhoto = itemView.findViewById(R.id.ivPhoto);
            tvName = itemView.findViewById(R.id.tvName);
            tvBio = itemView.findViewById(R.id.tvBio);
            btnFollow = itemView.findViewById(R.id.btnAction);
        }

        void bind(User user) {
            tvName.setText(user.getName());
            tvBio.setText(user.getBio() != null ? user.getBio() : "");
            Glide.with(requireContext())
                    .load(user.getPhotoUrl())
                    .circleCrop()
                    .placeholder(R.drawable.ic_placeholder_avatar)
                    .error(R.drawable.ic_placeholder_avatar)
                    .into(ivPhoto);

            btnFollow.setText(R.string.follow);
            btnFollow.setOnClickListener(v -> {
                followUser(user.getUid());
                btnFollow.setText(R.string.following);
            });

            itemView.setOnClickListener(v -> openUserProfile(user.getUid()));
        }

        private void followUser(String uid) {
            long now = System.currentTimeMillis();
            java.util.Map<String, Object> data = new java.util.HashMap<>();
            data.put("followerUid", currentUid);
            data.put("followingUid", uid);
            data.put("timestamp", now);
            data.put("status", "accepted");

            rootRef.child(Constants.FOLLOWING).child(currentUid).child(uid).setValue(data);
            rootRef.child(Constants.FOLLOWERS).child(uid).child(currentUid).setValue(data);
        }
    }

    private class PostViewHolder extends RecyclerView.ViewHolder {
        ImageView ivUserPhoto, ivPostImage;
        TextView tvUserName, tvText, tvStats;

        PostViewHolder(View itemView) {
            super(itemView);
            ivUserPhoto = itemView.findViewById(R.id.ivUserPhoto);
            ivPostImage = itemView.findViewById(R.id.ivPostImage);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvText = itemView.findViewById(R.id.tvText);
            tvStats = itemView.findViewById(R.id.tvStats);
        }

        void bind(Post post) {
            tvUserName.setText(post.getUserName());
            tvText.setText(post.getText() != null ? post.getText() : "");
            tvStats.setText(String.format(Locale.getDefault(), "%d إعجاب · %d تعليق",
                    post.getLikesCount(), post.getCommentsCount()));

            Glide.with(requireContext())
                    .load(post.getUserPhoto())
                    .circleCrop()
                    .placeholder(R.drawable.ic_placeholder_avatar)
                    .into(ivUserPhoto);

            if (post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
                ivPostImage.setVisibility(View.VISIBLE);
                Glide.with(requireContext()).load(post.getImageUrl()).centerCrop().into(ivPostImage);
            } else {
                ivPostImage.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> openPostDetails(post.getPostId()));
        }
    }

    private class HashtagViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvPostCount;

        HashtagViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvHashtagName);
            tvPostCount = itemView.findViewById(R.id.tvPostCount);
        }

        void bind(Hashtag hashtag) {
            tvName.setText("#" + hashtag.getName());
            tvPostCount.setText(String.format(Locale.getDefault(), "%d منشور", hashtag.getPostCount()));
            itemView.setOnClickListener(v -> openHashtag(hashtag.getName()));
        }
    }

    private class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;

        ImageViewHolder(View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivImage);
        }

        void bind(Post post) {
            if (post.getImageUrl() != null) {
                Glide.with(requireContext())
                        .load(post.getImageUrl())
                        .centerCrop()
                        .placeholder(R.drawable.ic_placeholder_image)
                        .into(ivImage);
            }
            itemView.setOnClickListener(v -> openPostDetails(post.getPostId()));
        }
    }

    // ==================================================================
    // Lifecycle
    // ==================================================================

    @Override
    public void onDestroyView() {
        detachAllListeners();
        super.onDestroyView();
    }
}
