package com.news.kimo.ui.activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.news.kimo.R;
import com.news.kimo.databinding.ActivitySearchBinding;
import com.news.kimo.models.Hashtag;
import com.news.kimo.models.Post;
import com.news.kimo.models.Trending;
import com.news.kimo.models.User;
import com.news.kimo.utils.CacheHelper;
import com.news.kimo.utils.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * Search activity supporting real-time search for users, posts, and hashtags.
 * Features recent search chips, clearable history, trending section,
 * and TabLayout filtering: الكل, المستخدمين, المنشورات, الوسوم.
 */
public class SearchActivity extends BaseActivity {

    private static final String TAG = "SearchActivity";
    private static final int SEARCH_DELAY_MS = 400;

    private ActivitySearchBinding binding;
    private DatabaseReference rootRef;
    private FirebaseAuth auth;
    private CacheHelper cacheHelper;

    private EditText etSearch;
    private RecyclerView rvResults;
    private TabLayout tabLayout;
    private ChipGroup chipRecentSearches;
    private View layoutTrending;
    private View layoutRecentSearches;
    private View layoutEmpty;

    private SearchResultsAdapter resultsAdapter;
    private TrendingAdapter trendingAdapter;

    private final List<Object> searchResults = new ArrayList<>();
    private final List<Trending> trendingList = new ArrayList<>();
    private final List<String> recentSearches = new ArrayList<>();

    private String currentQuery = "";
    private int currentTab = 0;
    private Runnable searchRunnable;
    private final android.os.Handler searchHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private ValueEventListener activeQueryListener;
    private Query activeSearchQuery;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySearchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        rootRef = FirebaseDatabase.getInstance().getReference();
        auth = FirebaseAuth.getInstance();
        cacheHelper = new CacheHelper(this);

        initViews();
        setupTabs();
        setupSearchInput();
        loadRecentSearches();
        loadTrending();
    }

    // ==================================================================
    // Initialisation
    // ==================================================================

    private void initViews() {
        etSearch = binding.etSearch;
        rvResults = binding.rvResults;
        tabLayout = binding.tabLayout;
        chipRecentSearches = binding.chipRecentSearches;
        layoutTrending = binding.layoutTrending;
        layoutRecentSearches = binding.layoutRecentSearches;
        layoutEmpty = binding.layoutEmpty;

        binding.ivBack.setOnClickListener(v -> onBackPressed());

        // Setup results RecyclerView
        rvResults.setLayoutManager(new LinearLayoutManager(this));
        resultsAdapter = new SearchResultsAdapter();
        rvResults.setAdapter(resultsAdapter);

        // Setup trending RecyclerView
        binding.rvTrending.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        trendingAdapter = new TrendingAdapter();
        binding.rvTrending.setAdapter(trendingAdapter);

        // Clear recent searches button
        binding.tvClearHistory.setOnClickListener(v -> clearRecentSearches());

        // Auto-focus search field
        etSearch.requestFocus();
        android.view.inputmethod.InputMethodManager imm =
                (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(etSearch, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_all));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_users));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_posts));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_hashtags));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                if (!currentQuery.isEmpty()) {
                    performSearch(currentQuery);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    private void setupSearchInput() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String query = s.toString().trim();
                // Debounce search
                searchHandler.removeCallbacks(searchRunnable);
                searchRunnable = () -> {
                    if (query.isEmpty()) {
                        showDefaultView();
                    } else {
                        performSearch(query);
                    }
                };
                searchHandler.postDelayed(searchRunnable, SEARCH_DELAY_MS);
            }
        });

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                String query = etSearch.getText().toString().trim();
                if (!query.isEmpty()) {
                    searchHandler.removeCallbacks(searchRunnable);
                    performSearch(query);
                    hideKeyboard();
                }
                return true;
            }
            return false;
        });
    }

    // ==================================================================
    // Recent Searches
    // ==================================================================

    private void loadRecentSearches() {
        recentSearches.clear();
        recentSearches.addAll(cacheHelper.loadSearchHistory());
        showRecentSearchChips();
    }

    private void showRecentSearchChips() {
        chipRecentSearches.removeAllViews();
        if (recentSearches.isEmpty()) {
            layoutRecentSearches.setVisibility(View.GONE);
            return;
        }
        layoutRecentSearches.setVisibility(View.VISIBLE);
        for (String query : recentSearches) {
            Chip chip = new Chip(this);
            chip.setText(query);
            chip.setChipBackgroundColorResource(R.color.colorSurface);
            chip.setTextColor(getColor(R.color.colorTextPrimary));
            chip.setCloseIconVisible(true);
            chip.setCloseIconResource(R.drawable.ic_close);
            chip.setOnClickListener(v -> {
                etSearch.setText(query);
                etSearch.setSelection(query.length());
                performSearch(query);
            });
            chip.setOnCloseIconClickListener(v -> {
                recentSearches.remove(query);
                saveRecentSearchesToCache();
                showRecentSearchChips();
            });
            chipRecentSearches.addView(chip);
        }
    }

    private void saveRecentSearchesToCache() {
        cacheHelper.clearSearchHistory();
        for (String query : recentSearches) {
            cacheHelper.saveSearchHistory(query);
        }
    }

    private void clearRecentSearches() {
        cacheHelper.clearSearchHistory();
        recentSearches.clear();
        showRecentSearchChips();
        showMessage(getString(R.string.search_history_cleared));
    }

    // ==================================================================
    // Trending
    // ==================================================================

    private void loadTrending() {
        rootRef.child(Constants.TRENDING)
                .orderByChild("count")
                .limitToLast(10)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        trendingList.clear();
                        for (DataSnapshot snap : snapshot.getChildren()) {
                            Trending trending = snap.getValue(Trending.class);
                            if (trending != null) {
                                trending.setTrendingId(snap.getKey());
                                trendingList.add(0, trending);
                            }
                        }
                        trendingAdapter.notifyDataSetChanged();
                        layoutTrending.setVisibility(trendingList.isEmpty() ? View.GONE : View.VISIBLE);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "loadTrending cancelled", error.toException());
                    }
                });
    }

    // ==================================================================
    // Search Logic
    // ==================================================================

    private void performSearch(String query) {
        currentQuery = query;
        // Save to recent searches
        cacheHelper.saveSearchHistory(query);
        recentSearches.remove(query);
        recentSearches.add(0, query);
        showRecentSearchChips();

        // Remove previous listener
        if (activeQueryListener != null && activeSearchQuery != null) {
            activeSearchQuery.removeEventListener(activeQueryListener);
        }

        // Hide default views, show results
        layoutTrending.setVisibility(View.GONE);
        layoutRecentSearches.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.GONE);
        rvResults.setVisibility(View.VISIBLE);

        String prefix = query.toLowerCase(Locale.getDefault());
        String endPrefix = prefix + "\uf8ff";

        switch (currentTab) {
            case 0: // الكل - search all
                searchAll(prefix, endPrefix);
                break;
            case 1: // المستخدمين - users
                searchUsers(prefix, endPrefix);
                break;
            case 2: // المنشورات - posts
                searchPosts(prefix, endPrefix);
                break;
            case 3: // الوسوم - hashtags
                searchHashtags(prefix, endPrefix);
                break;
        }
    }

    private void searchAll(String prefix, String endPrefix) {
        searchResults.clear();
        final int[] completedCount = {0};
        final int totalSearches = 3;

        // Search users
        rootRef.child(Constants.USERS)
                .orderByChild("name")
                .startAt(prefix)
                .endAt(endPrefix)
                .limitToFirst(5)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot snap : snapshot.getChildren()) {
                            User user = snap.getValue(User.class);
                            if (user != null) {
                                user.setUid(snap.getKey());
                                searchResults.add(user);
                            }
                        }
                        completedCount[0]++;
                        checkAllSearchComplete(completedCount[0], totalSearches);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        completedCount[0]++;
                        checkAllSearchComplete(completedCount[0], totalSearches);
                    }
                });

        // Search posts
        rootRef.child(Constants.POSTS)
                .orderByChild("text")
                .startAt(prefix)
                .endAt(endPrefix)
                .limitToFirst(5)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot snap : snapshot.getChildren()) {
                            Post post = snap.getValue(Post.class);
                            if (post != null) {
                                post.setPostId(snap.getKey());
                                searchResults.add(post);
                            }
                        }
                        completedCount[0]++;
                        checkAllSearchComplete(completedCount[0], totalSearches);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        completedCount[0]++;
                        checkAllSearchComplete(completedCount[0], totalSearches);
                    }
                });

        // Search hashtags
        rootRef.child(Constants.HASHTAGS)
                .orderByChild("name")
                .startAt(prefix)
                .endAt(endPrefix)
                .limitToFirst(5)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot snap : snapshot.getChildren()) {
                            Hashtag hashtag = snap.getValue(Hashtag.class);
                            if (hashtag != null) {
                                hashtag.setTagId(snap.getKey());
                                searchResults.add(hashtag);
                            }
                        }
                        completedCount[0]++;
                        checkAllSearchComplete(completedCount[0], totalSearches);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        completedCount[0]++;
                        checkAllSearchComplete(completedCount[0], totalSearches);
                    }
                });
    }

    private void checkAllSearchComplete(int completed, int total) {
        if (completed >= total) {
            resultsAdapter.notifyDataSetChanged();
            if (searchResults.isEmpty()) {
                layoutEmpty.setVisibility(View.VISIBLE);
                rvResults.setVisibility(View.GONE);
            }
        }
    }

    private void searchUsers(String prefix, String endPrefix) {
        searchResults.clear();
        activeSearchQuery = rootRef.child(Constants.USERS)
                .orderByChild("name")
                .startAt(prefix)
                .endAt(endPrefix)
                .limitToFirst(20);

        activeQueryListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                searchResults.clear();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    User user = snap.getValue(User.class);
                    if (user != null) {
                        user.setUid(snap.getKey());
                        searchResults.add(user);
                    }
                }
                resultsAdapter.notifyDataSetChanged();
                handleEmptyState();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "searchUsers cancelled", error.toException());
            }
        };
        activeSearchQuery.addValueEventListener(activeQueryListener);
    }

    private void searchPosts(String prefix, String endPrefix) {
        searchResults.clear();
        activeSearchQuery = rootRef.child(Constants.POSTS)
                .orderByChild("text")
                .startAt(prefix)
                .endAt(endPrefix)
                .limitToFirst(20);

        activeQueryListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                searchResults.clear();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    Post post = snap.getValue(Post.class);
                    if (post != null) {
                        post.setPostId(snap.getKey());
                        searchResults.add(post);
                    }
                }
                resultsAdapter.notifyDataSetChanged();
                handleEmptyState();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "searchPosts cancelled", error.toException());
            }
        };
        activeSearchQuery.addValueEventListener(activeQueryListener);
    }

    private void searchHashtags(String prefix, String endPrefix) {
        searchResults.clear();
        activeSearchQuery = rootRef.child(Constants.HASHTAGS)
                .orderByChild("name")
                .startAt(prefix)
                .endAt(endPrefix)
                .limitToFirst(20);

        activeQueryListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                searchResults.clear();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    Hashtag hashtag = snap.getValue(Hashtag.class);
                    if (hashtag != null) {
                        hashtag.setTagId(snap.getKey());
                        searchResults.add(hashtag);
                    }
                }
                resultsAdapter.notifyDataSetChanged();
                handleEmptyState();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "searchHashtags cancelled", error.toException());
            }
        };
        activeSearchQuery.addValueEventListener(activeQueryListener);
    }

    private void handleEmptyState() {
        if (searchResults.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            rvResults.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            rvResults.setVisibility(View.VISIBLE);
        }
    }

    private void showDefaultView() {
        currentQuery = "";
        searchResults.clear();
        resultsAdapter.notifyDataSetChanged();
        layoutTrending.setVisibility(trendingList.isEmpty() ? View.GONE : View.VISIBLE);
        layoutRecentSearches.setVisibility(recentSearches.isEmpty() ? View.GONE : View.VISIBLE);
        layoutEmpty.setVisibility(View.GONE);
        rvResults.setVisibility(View.GONE);
    }

    // ==================================================================
    // Navigation from results
    // ==================================================================

    private void navigateToUser(User user) {
        Bundle bundle = new Bundle();
        bundle.putString(Constants.EXTRA_USER_ID, user.getUid());
        openActivity(ProfileActivity.class, bundle);
    }

    private void navigateToPost(Post post) {
        Bundle bundle = new Bundle();
        bundle.putString(Constants.EXTRA_POST_ID, post.getPostId());
        openActivity(PostDetailsActivity.class, bundle);
    }

    private void navigateToHashtag(Hashtag hashtag) {
        // Navigate to HashtagActivity (to be implemented)
        Bundle bundle = new Bundle();
        bundle.putString("hashtag_name", hashtag.getName());
        openActivity(com.news.kimo.ui.activities.HashtagActivity.class, bundle);
    }

    private void navigateToTrending(Trending trending) {
        if ("post".equals(trending.getType()) && trending.getItemId() != null) {
            Bundle bundle = new Bundle();
            bundle.putString(Constants.EXTRA_POST_ID, trending.getItemId());
            openActivity(PostDetailsActivity.class, bundle);
        } else if ("hashtag".equals(trending.getType()) && trending.getItemId() != null) {
            Bundle bundle = new Bundle();
            bundle.putString("hashtag_name", trending.getItemId());
            openActivity(com.news.kimo.ui.activities.HashtagActivity.class, bundle);
        } else if ("user".equals(trending.getType()) && trending.getItemId() != null) {
            Bundle bundle = new Bundle();
            bundle.putString(Constants.EXTRA_USER_ID, trending.getItemId());
            openActivity(ProfileActivity.class, bundle);
        }
    }

    // ==================================================================
    // Adapters
    // ==================================================================

    /**
     * Adapter for mixed search results (users, posts, hashtags).
     */
    private class SearchResultsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private static final int TYPE_USER = 1;
        private static final int TYPE_POST = 2;
        private static final int TYPE_HASHTAG = 3;

        @Override
        public int getItemViewType(int position) {
            Object item = searchResults.get(position);
            if (item instanceof User) return TYPE_USER;
            if (item instanceof Post) return TYPE_POST;
            if (item instanceof Hashtag) return TYPE_HASHTAG;
            return TYPE_POST;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            switch (viewType) {
                case TYPE_USER:
                    View userView = inflater.inflate(R.layout.item_search_user, parent, false);
                    return new UserViewHolder(userView);
                case TYPE_HASHTAG:
                    View hashView = inflater.inflate(R.layout.item_search_hashtag, parent, false);
                    return new HashtagViewHolder(hashView);
                default:
                    View postView = inflater.inflate(R.layout.item_search_post, parent, false);
                    return new PostViewHolder(postView);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            Object item = searchResults.get(position);
            if (holder instanceof UserViewHolder && item instanceof User) {
                ((UserViewHolder) holder).bind((User) item);
            } else if (holder instanceof PostViewHolder && item instanceof Post) {
                ((PostViewHolder) holder).bind((Post) item);
            } else if (holder instanceof HashtagViewHolder && item instanceof Hashtag) {
                ((HashtagViewHolder) holder).bind((Hashtag) item);
            }
        }

        @Override
        public int getItemCount() {
            return searchResults.size();
        }
    }

    private class UserViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        TextView tvName, tvBio;

        UserViewHolder(View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            tvName = itemView.findViewById(R.id.tvName);
            tvBio = itemView.findViewById(R.id.tvBio);
        }

        void bind(User user) {
            tvName.setText(user.getName());
            tvBio.setText(user.getBio() != null ? user.getBio() : "");
            loadCircularImage(user.getPhotoUrl(), ivAvatar);
            itemView.setOnClickListener(v -> navigateToUser(user));
        }
    }

    private class PostViewHolder extends RecyclerView.ViewHolder {
        ImageView ivUserPhoto, ivPostImage;
        TextView tvUserName, tvText, tvTime;

        PostViewHolder(View itemView) {
            super(itemView);
            ivUserPhoto = itemView.findViewById(R.id.ivUserPhoto);
            ivPostImage = itemView.findViewById(R.id.ivPostImage);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvText = itemView.findViewById(R.id.tvText);
            tvTime = itemView.findViewById(R.id.tvTime);
        }

        void bind(Post post) {
            tvUserName.setText(post.getUserName());
            tvText.setText(post.getText() != null ? post.getText() : "");
            tvTime.setText(getRelativeTime(post.getTimestamp()));
            loadCircularImage(post.getUserPhoto(), ivUserPhoto);
            if (post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
                ivPostImage.setVisibility(View.VISIBLE);
                loadImage(post.getImageUrl(), ivPostImage);
            } else {
                ivPostImage.setVisibility(View.GONE);
            }
            itemView.setOnClickListener(v -> navigateToPost(post));
        }
    }

    private class HashtagViewHolder extends RecyclerView.ViewHolder {
        TextView tvHashtagName, tvPostCount;

        HashtagViewHolder(View itemView) {
            super(itemView);
            tvHashtagName = itemView.findViewById(R.id.tvHashtagName);
            tvPostCount = itemView.findViewById(R.id.tvPostCount);
        }

        void bind(Hashtag hashtag) {
            tvHashtagName.setText("#" + hashtag.getName());
            tvPostCount.setText(String.format(Locale.getDefault(), "%d منشور", hashtag.getPostCount()));
            itemView.setOnClickListener(v -> navigateToHashtag(hashtag));
        }
    }

    /**
     * Simple adapter for trending items displayed horizontally.
     */
    private class TrendingAdapter extends RecyclerView.Adapter<TrendingAdapter.TrendingViewHolder> {

        @NonNull
        @Override
        public TrendingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_trending, parent, false);
            return new TrendingViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull TrendingViewHolder holder, int position) {
            Trending trending = trendingList.get(position);
            holder.tvTitle.setText(trending.getTitle());
            holder.tvSubtitle.setText(trending.getSubtitle());
            holder.tvCount.setText(String.format(Locale.getDefault(), "%,d", trending.getCount()));
            holder.itemView.setOnClickListener(v -> navigateToTrending(trending));
        }

        @Override
        public int getItemCount() {
            return trendingList.size();
        }

        class TrendingViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvSubtitle, tvCount;

            TrendingViewHolder(View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvTitle);
                tvSubtitle = itemView.findViewById(R.id.tvSubtitle);
                tvCount = itemView.findViewById(R.id.tvCount);
            }
        }
    }

    // ==================================================================
    // Lifecycle
    // ==================================================================

    @Override
    protected void onDestroy() {
        searchHandler.removeCallbacks(searchRunnable);
        if (activeQueryListener != null && activeSearchQuery != null) {
            activeSearchQuery.removeEventListener(activeQueryListener);
        }
        super.onDestroy();
    }
}
