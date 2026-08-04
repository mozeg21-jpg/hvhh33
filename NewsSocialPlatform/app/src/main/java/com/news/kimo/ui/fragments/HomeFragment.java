package com.news.kimo.ui.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.news.kimo.R;
import com.news.kimo.database.FirestoreHelper;
import com.news.kimo.databinding.FragmentHomeBinding;
import com.news.kimo.models.Post;
import com.news.kimo.ui.activities.PostDetailsActivity;
import com.news.kimo.utils.CacheHelper;
import com.news.kimo.utils.Constants;
import com.news.kimo.utils.NetworkHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Home feed fragment displaying a paginated list of posts from
 * Firebase Realtime Database ordered by timestamp (descending).
 * <p>
 * Features:
 * <ul>
 *   <li>SwipeRefreshLayout for manual pull-to-refresh</li>
 *   <li>Shimmer loading on initial load</li>
 *   <li>Cached data shown immediately via {@link CacheHelper}</li>
 *   <li>Sync banner while loading from network</li>
 *   <li>Real-time updates via {@link ChildEventListener}</li>
 *   <li>Scroll-based pagination (load more when near bottom)</li>
 *   <li>Empty state when no posts exist</li>
 * </ul>
 */
public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private static final int PAGE_SIZE = Constants.PAGINATION_SIZE;

    private FragmentHomeBinding binding;
    private PostAdapter postAdapter;
    private CacheHelper cacheHelper;
    private NetworkHelper networkHelper;
    private ShimmerFrameLayout shimmerLayout;

    private final List<Post> postList = new ArrayList<>();
    private Query postsQuery;
    private ChildEventListener activeChildListener;
    private ValueEventListener singleLoadListener;

    private long lastLoadedTimestamp = Long.MAX_VALUE;
    private boolean isLoadingMore = false;
    private boolean isInitialLoad = true;
    private boolean allPostsLoaded = false;
    private Post lastPostInList;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cacheHelper = new CacheHelper(requireContext());
        networkHelper = new NetworkHelper(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews();
        initRecyclerView();
        initSwipeRefresh();
        showCachedPosts();
        loadPostsFromNetwork();
    }

    // ==================================================================
    // Initialisation
    // ==================================================================

    private void initViews() {
        shimmerLayout = binding.shimmerFrameLayout;
    }

    private void initRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        layoutManager.setReverseLayout(true);
        layoutManager.setStackFromEnd(true);

        binding.recyclerViewPosts.setLayoutManager(layoutManager);
        binding.recyclerViewPosts.setHasFixedSize(true);

        postAdapter = new PostAdapter(postList, (post, position) -> {
            // Post click → navigate to PostDetailsActivity
            Bundle bundle = new Bundle();
            bundle.putString(Constants.EXTRA_POST_ID, post.getPostId());
            // Use the parent activity (BaseActivity) helper
            if (getActivity() instanceof com.news.kimo.ui.activities.BaseActivity) {
                ((com.news.kimo.ui.activities.BaseActivity) getActivity())
                        .openActivity(PostDetailsActivity.class, bundle);
            }
        });

        binding.recyclerViewPosts.setAdapter(postAdapter);

        // Scroll-based pagination
        binding.recyclerViewPosts.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                LinearLayoutManager lm = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (lm == null) return;

                int visibleItemCount = lm.getChildCount();
                int totalItemCount = lm.getItemCount();
                int firstVisibleItem = lm.findFirstVisibleItemPosition();

                // When the user scrolls near the top (since reverse layout, top = start)
                if (!isLoadingMore && !allPostsLoaded) {
                    if ((visibleItemCount + firstVisibleItem) >= totalItemCount - 3
                            && firstVisibleItem >= 0) {
                        loadMorePosts();
                    }
                }
            }
        });
    }

    private void initSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeResources(R.color.colorPrimary);
        binding.swipeRefresh.setOnRefreshListener(() -> {
            refreshPosts();
        });
    }

    // ==================================================================
    // Cached Data
    // ==================================================================

    /**
     * Immediately shows cached posts while the network fetch is in progress.
     */
    private void showCachedPosts() {
        List<Post> cachedPosts = cacheHelper.loadPosts();
        if (!cachedPosts.isEmpty()) {
            postList.clear();
            postList.addAll(cachedPosts);
            postAdapter.notifyDataSetChanged();
            binding.recyclerViewPosts.setVisibility(View.VISIBLE);
            binding.layoutEmptyState.setVisibility(View.GONE);
            Log.d(TAG, "Showing " + cachedPosts.size() + " cached posts");
        }
    }

    // ==================================================================
    // Network Loading
    // ==================================================================

    /**
     * Loads posts from Firebase with shimmer, then attaches a real-time
     * listener for live updates.
     */
    private void loadPostsFromNetwork() {
        if (isInitialLoad) {
            showShimmer();
            showSyncBanner();
        }

        // Initial single-value load to get the first page
        postsQuery = FirestoreHelper.getInstance().getReference(Constants.POSTS)
                .orderByChild("timestamp")
                .limitToLast(PAGE_SIZE);

        postsQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                hideShimmer();
                isInitialLoad = false;

                List<Post> fetchedPosts = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Post post = child.getValue(Post.class);
                    if (post != null) {
                        post.setPostId(child.getKey());
                        // Skip scheduled/archived posts
                        if (!post.isScheduled() && !post.isArchived()) {
                            fetchedPosts.add(post);
                        }
                    }
                }

                // Sort descending by timestamp (newest first)
                Collections.sort(fetchedPosts, (a, b) ->
                        Long.compare(b.getTimestamp(), a.getTimestamp()));

                updatePostList(fetchedPosts);
                attachRealtimeListener();
                hideSyncBanner();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                hideShimmer();
                isInitialLoad = false;
                hideSyncBanner();
                Log.e(TAG, "Failed to load posts", error.toException());

                if (postList.isEmpty()) {
                    binding.layoutEmptyState.setVisibility(View.VISIBLE);
                    binding.recyclerViewPosts.setVisibility(View.GONE);
                }
            }
        });
    }

    /**
     * Attaches a real-time {@link ChildEventListener} for live post updates.
     */
    private void attachRealtimeListener() {
        if (activeChildListener != null) {
            postsQuery.removeEventListener(activeChildListener);
        }

        activeChildListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Post post = snapshot.getValue(Post.class);
                if (post == null) return;
                post.setPostId(snapshot.getKey());

                // Skip scheduled/archived
                if (post.isScheduled() || post.isArchived()) return;

                // Avoid duplicate
                for (int i = 0; i < postList.size(); i++) {
                    if (post.getPostId().equals(postList.get(i).getPostId())) {
                        return;
                    }
                }

                postList.add(0, post); // Add at beginning (newest first)
                postAdapter.notifyItemInserted(0);
                binding.recyclerViewPosts.scrollToPosition(0);
                updateEmptyState();
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Post post = snapshot.getValue(Post.class);
                if (post == null) return;
                post.setPostId(snapshot.getKey());

                for (int i = 0; i < postList.size(); i++) {
                    if (post.getPostId().equals(postList.get(i).getPostId())) {
                        postList.set(i, post);
                        postAdapter.notifyItemChanged(i);
                        break;
                    }
                }
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                String postId = snapshot.getKey();
                for (int i = 0; i < postList.size(); i++) {
                    if (postId.equals(postList.get(i).getPostId())) {
                        postList.remove(i);
                        postAdapter.notifyItemRemoved(i);
                        updateEmptyState();
                        break;
                    }
                }
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                // Not needed for this feed
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "Real-time listener cancelled", error.toException());
            }
        };

        postsQuery.addChildEventListener(activeChildListener);
    }

    // ==================================================================
    // Pagination
    // ==================================================================

    /**
     * Loads the next page of posts older than the last loaded post.
     */
    private void loadMorePosts() {
        if (isLoadingMore || allPostsLoaded) return;
        if (postList.isEmpty()) return;

        isLoadingMore = true;
        postAdapter.showLoadingFooter();

        // Get the oldest post's timestamp
        Post oldestPost = postList.get(postList.size() - 1);
        lastLoadedTimestamp = oldestPost.getTimestamp();

        Query moreQuery = FirestoreHelper.getInstance().getReference(Constants.POSTS)
                .orderByChild("timestamp")
                .endAt(lastLoadedTimestamp - 1)
                .limitToLast(PAGE_SIZE);
        moreQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                isLoadingMore = false;
                postAdapter.hideLoadingFooter();

                List<Post> newPosts = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Post post = child.getValue(Post.class);
                    if (post != null) {
                        post.setPostId(child.getKey());
                        if (!post.isScheduled() && !post.isArchived()) {
                            // Avoid duplicates
                            boolean exists = false;
                            for (Post p : postList) {
                                if (p.getPostId().equals(post.getPostId())) {
                                    exists = true;
                                    break;
                                }
                            }
                            if (!exists) {
                                newPosts.add(post);
                            }
                        }
                    }
                }

                if (newPosts.isEmpty()) {
                    allPostsLoaded = true;
                    return;
                }

                // Sort descending and append
                Collections.sort(newPosts, (a, b) ->
                        Long.compare(b.getTimestamp(), a.getTimestamp()));

                int startPos = postList.size();
                postList.addAll(newPosts);
                postAdapter.notifyItemRangeInserted(startPos, newPosts.size());

                // Cache the updated list
                cacheHelper.savePosts(postList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                isLoadingMore = false;
                postAdapter.hideLoadingFooter();
                Log.e(TAG, "Failed to load more posts", error.toException());
            }
        });
    }

    // ==================================================================
    // Pull to Refresh
    // ==================================================================

    /**
     * Refreshes the entire post list from the network.
     */
    private void refreshPosts() {
        allPostsLoaded = false;
        isLoadingMore = false;
        lastLoadedTimestamp = Long.MAX_VALUE;

        // Remove old listener
        if (activeChildListener != null && postsQuery != null) {
            postsQuery.removeEventListener(activeChildListener);
            activeChildListener = null;
        }

        postList.clear();
        postAdapter.notifyDataSetChanged();

        loadPostsFromNetwork();

        // Stop the refresh indicator after a timeout to avoid spinning forever
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (binding.swipeRefresh.isRefreshing()) {
                binding.swipeRefresh.setRefreshing(false);
            }
        }, 5000);
    }

    // ==================================================================
    // Data Updates
    // ==================================================================

    /**
     * Replaces the post list and caches the result.
     *
     * @param fetchedPosts the newly fetched posts
     */
    private void updatePostList(List<Post> fetchedPosts) {
        postList.clear();
        postList.addAll(fetchedPosts);
        postAdapter.notifyDataSetChanged();
        updateEmptyState();

        // Cache for offline access
        cacheHelper.savePosts(postList);

        // Stop the swipe-refresh indicator
        binding.swipeRefresh.setRefreshing(false);
    }

    /**
     * Shows or hides the empty-state layout based on the post list size.
     */
    private void updateEmptyState() {
        if (postList.isEmpty()) {
            binding.layoutEmptyState.setVisibility(View.VISIBLE);
            binding.recyclerViewPosts.setVisibility(View.GONE);
        } else {
            binding.layoutEmptyState.setVisibility(View.GONE);
            binding.recyclerViewPosts.setVisibility(View.VISIBLE);
        }
    }

    // ==================================================================
    // Shimmer / UI States
    // ==================================================================

    private void showShimmer() {
        if (shimmerLayout != null) {
            shimmerLayout.setVisibility(View.VISIBLE);
            binding.recyclerViewPosts.setVisibility(View.GONE);
            shimmerLayout.startShimmer();
        }
    }

    private void hideShimmer() {
        if (shimmerLayout != null) {
            shimmerLayout.stopShimmer();
            shimmerLayout.setVisibility(View.GONE);
        }
        if (!postList.isEmpty()) {
            binding.recyclerViewPosts.setVisibility(View.VISIBLE);
        }
    }

    private void showSyncBanner() {
        if (binding.syncBanner != null) {
            binding.syncBanner.setVisibility(View.VISIBLE);
        }
    }

    private void hideSyncBanner() {
        if (binding.syncBanner != null) {
            binding.syncBanner.setVisibility(View.GONE);
        }
    }

    // ==================================================================
    // Lifecycle
    // ==================================================================

    @Override
    public void onDestroyView() {
        if (activeChildListener != null && postsQuery != null) {
            postsQuery.removeEventListener(activeChildListener);
            activeChildListener = null;
        }
        if (shimmerLayout != null) {
            shimmerLayout.stopShimmer();
        }
        binding = null;
        super.onDestroyView();
    }

    // ==================================================================
    // Adapter Interface
    // ==================================================================

    /**
     * Callback interface for post item click events.
     */
    public interface OnPostClickListener {
        void onPostClick(Post post, int position);
    }
}
