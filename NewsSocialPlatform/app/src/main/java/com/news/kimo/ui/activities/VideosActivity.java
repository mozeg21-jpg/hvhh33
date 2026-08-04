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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.news.kimo.R;
import com.news.kimo.databinding.ActivityVideosBinding;
import com.news.kimo.models.Post;
import com.news.kimo.utils.Constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Displays all posts containing a video (videoUrl != null) in a 2-column grid.
 * Each cell shows a video thumbnail with a play icon overlay.
 * Clicking opens PostDetailsActivity. Supports pull-to-refresh and empty state.
 */
public class VideosActivity extends BaseActivity {

    private static final String TAG = "VideosActivity";
    private static final int GRID_SPAN = 2;
    private static final int PAGE_SIZE = 40;

    private ActivityVideosBinding binding;
    private DatabaseReference rootRef;

    private final List<Post> videoPosts = new ArrayList<>();
    private final List<Post> allVideoPosts = new ArrayList<>();
    private MediaAdapter mediaAdapter;
    private ChildEventListener childEventListener;
    private Query activeQuery;

    private boolean isLoadingMore = false;
    private String lastKey = null;
    private boolean hasMore = true;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVideosBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        rootRef = FirebaseDatabase.getInstance().getReference();

        initViews();
        setupRecyclerView();
        loadVideoPosts();
        setupScrollListener();
    }

    // ==================================================================
    // Initialisation
    // ==================================================================

    private void initViews() {
        binding.ivBack.setOnClickListener(v -> onBackPressed());

        // Pull to refresh
        binding.swipeRefresh.setOnRefreshListener(() -> {
            lastKey = null;
            hasMore = true;
            isLoadingMore = false;
            allVideoPosts.clear();
            loadVideoPosts();
            binding.swipeRefresh.setRefreshing(false);
        });

        updateEmptyState();
    }

    private void setupRecyclerView() {
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, GRID_SPAN);
        binding.rvVideos.setLayoutManager(gridLayoutManager);
        mediaAdapter = new MediaAdapter();
        binding.rvVideos.setAdapter(mediaAdapter);
    }

    // ==================================================================
    // Load Video Posts
    // ==================================================================

    private void loadVideoPosts() {
        if (childEventListener != null && activeQuery != null) {
            activeQuery.removeEventListener(childEventListener);
        }

        allVideoPosts.clear();
        videoPosts.clear();
        mediaAdapter.notifyDataSetChanged();

        activeQuery = rootRef.child(Constants.POSTS)
                .orderByChild("timestamp")
                .limitToLast(PAGE_SIZE);

        childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Post post = snapshot.getValue(Post.class);
                if (post != null && post.getVideoUrl() != null && !post.getVideoUrl().isEmpty()) {
                    post.setPostId(snapshot.getKey());
                    allVideoPosts.add(0, post);
                    Collections.sort(allVideoPosts, (a, b) ->
                            Long.compare(b.getTimestamp(), a.getTimestamp()));
                    refreshDisplayList();
                }
                updatePaginationState();
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Post post = snapshot.getValue(Post.class);
                if (post != null) {
                    String key = snapshot.getKey();
                    for (int i = 0; i < allVideoPosts.size(); i++) {
                        if (key.equals(allVideoPosts.get(i).getPostId())) {
                            post.setPostId(key);
                            allVideoPosts.set(i, post);
                            break;
                        }
                    }
                    refreshDisplayList();
                }
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                String key = snapshot.getKey();
                for (int i = 0; i < allVideoPosts.size(); i++) {
                    if (key.equals(allVideoPosts.get(i).getPostId())) {
                        allVideoPosts.remove(i);
                        break;
                    }
                }
                refreshDisplayList();
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "loadVideoPosts cancelled", error.toException());
            }
        };

        activeQuery.addChildEventListener(childEventListener);
    }

    private void refreshDisplayList() {
        videoPosts.clear();
        videoPosts.addAll(allVideoPosts);
        mediaAdapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void updatePaginationState() {
        if (!allVideoPosts.isEmpty()) {
            // Find the oldest key for pagination
            String oldestKey = null;
            for (Post p : allVideoPosts) {
                if (oldestKey == null || p.getPostId().compareTo(oldestKey) < 0) {
                    oldestKey = p.getPostId();
                }
            }
            lastKey = oldestKey;
            hasMore = allVideoPosts.size() >= PAGE_SIZE;
        }
    }

    // ==================================================================
    // Pagination
    // ==================================================================

    private void setupScrollListener() {
        binding.rvVideos.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                GridLayoutManager lm = (GridLayoutManager) recyclerView.getLayoutManager();
                if (lm == null) return;

                int visible = lm.getChildCount();
                int total = lm.getItemCount();
                int firstVisible = lm.findFirstVisibleItemPosition();

                if (!isLoadingMore && hasMore
                        && (visible + firstVisible) >= total - 4
                        && lastKey != null) {
                    loadMoreVideoPosts();
                }
            }
        });
    }

    private void loadMoreVideoPosts() {
        isLoadingMore = true;

        rootRef.child(Constants.POSTS)
                .orderByChild("timestamp")
                .endAt(lastKey)
                .limitToLast(PAGE_SIZE)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<Post> morePosts = new ArrayList<>();
                        for (DataSnapshot snap : snapshot.getChildren()) {
                            if (snap.getKey().equals(lastKey)) continue;
                            Post post = snap.getValue(Post.class);
                            if (post != null && post.getVideoUrl() != null
                                    && !post.getVideoUrl().isEmpty()) {
                                post.setPostId(snap.getKey());
                                if (!containsPost(allVideoPosts, post.getPostId())) {
                                    morePosts.add(post);
                                }
                            }
                        }

                        if (morePosts.isEmpty()) {
                            hasMore = false;
                            isLoadingMore = false;
                            return;
                        }

                        allVideoPosts.addAll(morePosts);
                        Collections.sort(allVideoPosts, (a, b) ->
                                Long.compare(b.getTimestamp(), a.getTimestamp()));
                        refreshDisplayList();
                        updatePaginationState();
                        isLoadingMore = false;
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "loadMoreVideoPosts cancelled", error.toException());
                        isLoadingMore = false;
                    }
                });
    }

    private boolean containsPost(List<Post> list, String postId) {
        for (Post p : list) {
            if (postId.equals(p.getPostId())) return true;
        }
        return false;
    }

    // ==================================================================
    // Empty State
    // ==================================================================

    private void updateEmptyState() {
        if (videoPosts.isEmpty()) {
            binding.layoutEmpty.setVisibility(View.VISIBLE);
            binding.rvVideos.setVisibility(View.GONE);
        } else {
            binding.layoutEmpty.setVisibility(View.GONE);
            binding.rvVideos.setVisibility(View.VISIBLE);
        }
    }

    // ==================================================================
    // Media Adapter
    // ==================================================================

    private class MediaAdapter extends RecyclerView.Adapter<MediaAdapter.MediaViewHolder> {

        @NonNull
        @Override
        public MediaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_media_grid, parent, false);
            return new MediaViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull MediaViewHolder holder, int position) {
            Post post = videoPosts.get(position);
            holder.bind(post);
        }

        @Override
        public int getItemCount() {
            return videoPosts.size();
        }

        class MediaViewHolder extends RecyclerView.ViewHolder {
            ImageView ivMedia;
            ImageView ivPlayIcon;
            TextView tvViews;

            MediaViewHolder(View itemView) {
                super(itemView);
                ivMedia = itemView.findViewById(R.id.ivMedia);
                ivPlayIcon = itemView.findViewById(R.id.ivPlayIcon);
                tvViews = itemView.findViewById(R.id.tvViews);

                // Make play icon visible for video items
                if (ivPlayIcon != null) {
                    ivPlayIcon.setVisibility(View.VISIBLE);
                    ivPlayIcon.setImageResource(R.drawable.ic_play_circle);
                }
            }

            void bind(Post post) {
                // Show first image as thumbnail, or use a placeholder
                if (post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
                    loadImage(post.getImageUrl(), ivMedia);
                } else if (post.getImages() != null && !post.getImages().isEmpty()) {
                    loadImage(post.getImages().get(0), ivMedia);
                } else {
                    ivMedia.setImageResource(R.drawable.ic_placeholder_video);
                }

                if (tvViews != null && post.getViewsCount() > 0) {
                    tvViews.setVisibility(View.VISIBLE);
                    tvViews.setText(String.valueOf(post.getViewsCount()));
                } else if (tvViews != null) {
                    tvViews.setVisibility(View.GONE);
                }

                itemView.setOnClickListener(v -> {
                    Bundle bundle = new Bundle();
                    bundle.putString(Constants.EXTRA_POST_ID, post.getPostId());
                    openActivity(PostDetailsActivity.class, bundle);
                });
            }
        }
    }

    // ==================================================================
    // Lifecycle
    // ==================================================================

    @Override
    protected void onDestroy() {
        if (childEventListener != null && activeQuery != null) {
            activeQuery.removeEventListener(childEventListener);
        }
        super.onDestroy();
    }
}
