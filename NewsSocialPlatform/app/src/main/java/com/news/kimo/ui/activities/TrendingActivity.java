package com.news.kimo.ui.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.news.kimo.R;
import com.news.kimo.databinding.ActivityTrendingBinding;
import com.news.kimo.models.Trending;
import com.news.kimo.utils.Constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Activity displaying trending content with tabs:
 * الأكثر مشاهدة, الأكثر إعجاباً, الأكثر تعليقاً, الوسوم الرائجة, الأكثر نشاطاً.
 * Each tab queries Firebase trending/ node with a different orderBy field.
 * Supports pull-to-refresh and click navigation to post/hashtag/user.
 */
public class TrendingActivity extends BaseActivity {

    private static final String TAG = "TrendingActivity";
    private static final int PAGE_SIZE = 50;

    private ActivityTrendingBinding binding;
    private DatabaseReference rootRef;

    private TabLayout tabLayout;
    private RecyclerView rvTrending;
    private SwipeRefreshLayout swipeRefresh;
    private View layoutEmpty;
    private TextView tvEmptyText;

    private TrendingAdapter trendingAdapter;
    private final List<Trending> trendingList = new ArrayList<>();

    private ValueEventListener activeListener;
    private Query activeQuery;
    private int currentTab = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTrendingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        rootRef = FirebaseDatabase.getInstance().getReference();

        initViews();
        setupTabs();
        setupRecyclerView();
        setupSwipeRefresh();
        loadTrending();
    }

    // ==================================================================
    // Initialisation
    // ==================================================================

    private void initViews() {
        binding.ivBack.setOnClickListener(v -> onBackPressed());
        tabLayout = binding.tabLayout;
        rvTrending = binding.rvTrending;
        swipeRefresh = binding.swipeRefresh;
        layoutEmpty = binding.layoutEmpty;
        tvEmptyText = binding.tvEmptyText;
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_most_viewed));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_most_liked));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_most_commented));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_trending_hashtags));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_most_active));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                loadTrending();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                loadTrending();
            }
        });
    }

    private void setupRecyclerView() {
        rvTrending.setLayoutManager(new LinearLayoutManager(this));
        trendingAdapter = new TrendingAdapter();
        rvTrending.setAdapter(trendingAdapter);
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setColorSchemeResources(R.color.colorPrimary);
        swipeRefresh.setOnRefreshListener(() -> {
            loadTrending();
            swipeRefresh.setRefreshing(false);
        });
    }

    // ==================================================================
    // Firebase
    // ==================================================================

    private void loadTrending() {
        // Remove previous listener
        if (activeListener != null && activeQuery != null) {
            activeQuery.removeEventListener(activeListener);
        }

        String orderByField = getOrderByField();
        String filterType = getFilterType();

        activeQuery = rootRef.child(Constants.TRENDING)
                .orderByChild(orderByField)
                .limitToLast(PAGE_SIZE);

        activeListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                trendingList.clear();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    Trending trending = snap.getValue(Trending.class);
                    if (trending != null) {
                        trending.setTrendingId(snap.getKey());
                        // Apply type filter if needed
                        if (filterType == null || filterType.equals(trending.getType())) {
                            trendingList.add(0, trending);
                        }
                    }
                }
                // Sort descending by count
                Collections.sort(trendingList, (t1, t2) -> Long.compare(t2.getCount(), t1.getCount()));
                trendingAdapter.notifyDataSetChanged();
                updateEmptyState();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "loadTrending cancelled", error.toException());
                showError(getString(R.string.error_generic));
            }
        };

        activeQuery.addValueEventListener(activeListener);
    }

    private String getOrderByField() {
        switch (currentTab) {
            case 0: return "count";       // الأكثر مشاهدة - ordered by views
            case 1: return "count";       // الأكثر إعجاباً - ordered by likes
            case 2: return "count";       // الأكثر تعليقاً - ordered by comments
            case 3: return "count";       // الوسوم الرائجة - hashtags by post count
            case 4: return "timestamp";   // الأكثر نشاطاً - most recent
            default: return "count";
        }
    }

    private String getFilterType() {
        switch (currentTab) {
            case 0: return "view";        // Most viewed posts
            case 1: return "like";        // Most liked posts
            case 2: return "comment";     // Most commented posts
            case 3: return "hashtag";     // Trending hashtags
            case 4: return null;           // Most active (all types)
            default: return null;
        }
    }

    // ==================================================================
    // Empty State
    // ==================================================================

    private void updateEmptyState() {
        if (trendingList.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            rvTrending.setVisibility(View.GONE);
            tvEmptyText.setText(getEmptyMessage());
        } else {
            layoutEmpty.setVisibility(View.GONE);
            rvTrending.setVisibility(View.VISIBLE);
        }
    }

    private String getEmptyMessage() {
        switch (currentTab) {
            case 0: return getString(R.string.no_trending_views);
            case 1: return getString(R.string.no_trending_likes);
            case 2: return getString(R.string.no_trending_comments);
            case 3: return getString(R.string.no_trending_hashtags);
            case 4: return getString(R.string.no_trending_active);
            default: return getString(R.string.no_trending);
        }
    }

    // ==================================================================
    // Navigation
    // ==================================================================

    private void navigateToTrending(Trending trending) {
        Bundle bundle = new Bundle();
        if ("post".equals(trending.getType()) && trending.getItemId() != null) {
            bundle.putString(Constants.EXTRA_POST_ID, trending.getItemId());
            openActivity(PostDetailsActivity.class, bundle);
        } else if ("hashtag".equals(trending.getType()) && trending.getItemId() != null) {
            bundle.putString("hashtag_name", trending.getItemId());
            openActivity(com.news.kimo.ui.activities.HashtagActivity.class, bundle);
        } else if ("user".equals(trending.getType()) && trending.getItemId() != null) {
            bundle.putString(Constants.EXTRA_USER_ID, trending.getItemId());
            openActivity(ProfileActivity.class, bundle);
        }
    }

    // ==================================================================
    // Trending Adapter
    // ==================================================================

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
            holder.bind(trending, position);
        }

        @Override
        public int getItemCount() {
            return trendingList.size();
        }

        class TrendingViewHolder extends RecyclerView.ViewHolder {
            TextView tvRank, tvTitle, tvSubtitle, tvCount, tvTime;
            ImageView ivImage;
            View ivTypeIcon;

            TrendingViewHolder(View itemView) {
                super(itemView);
                tvRank = itemView.findViewById(R.id.tvRank);
                tvTitle = itemView.findViewById(R.id.tvTitle);
                tvSubtitle = itemView.findViewById(R.id.tvSubtitle);
                tvCount = itemView.findViewById(R.id.tvCount);
                tvTime = itemView.findViewById(R.id.tvTime);
                ivImage = itemView.findViewById(R.id.ivImage);
                ivTypeIcon = itemView.findViewById(R.id.ivTypeIcon);
            }

            void bind(Trending trending, int position) {
                tvRank.setText(String.format(Locale.getDefault(), "%d", position + 1));
                tvTitle.setText(trending.getTitle() != null ? trending.getTitle() : "");
                tvSubtitle.setText(trending.getSubtitle() != null ? trending.getSubtitle() : "");
                tvCount.setText(String.format(Locale.getDefault(), "%,d", trending.getCount()));
                tvTime.setText(getRelativeTime(trending.getTimestamp()));

                // Load image if available
                if (trending.getImageUrl() != null && !trending.getImageUrl().isEmpty()) {
                    ivImage.setVisibility(View.VISIBLE);
                    loadImage(trending.getImageUrl(), ivImage);
                } else {
                    ivImage.setVisibility(View.GONE);
                }

                // Type icon background
                if ("hashtag".equals(trending.getType())) {
                    ivTypeIcon.setBackgroundResource(R.drawable.bg_hashtag_circle);
                } else if ("user".equals(trending.getType())) {
                    ivTypeIcon.setBackgroundResource(R.drawable.bg_user_circle);
                } else {
                    ivTypeIcon.setBackgroundResource(R.drawable.bg_post_circle);
                }

                itemView.setOnClickListener(v -> navigateToTrending(trending));
            }
        }
    }

    // ==================================================================
    // Lifecycle
    // ==================================================================

    @Override
    protected void onDestroy() {
        if (activeListener != null && activeQuery != null) {
            activeQuery.removeEventListener(activeListener);
        }
        super.onDestroy();
    }
}
