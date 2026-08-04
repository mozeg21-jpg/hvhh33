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
import com.news.kimo.databinding.ActivityPhotosBinding;
import com.news.kimo.models.Post;
import com.news.kimo.utils.Constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Displays all posts containing images (imageUrl != null or images list not empty)
 * in a 3-column grid. Each cell shows the image thumbnail.
 * Clicking opens PostDetailsActivity. Supports pull-to-refresh and empty state.
 */
public class PhotosActivity extends BaseActivity {

    private static final String TAG = "PhotosActivity";
    private static final int GRID_SPAN = 3;
    private static final int PAGE_SIZE = 60;

    private ActivityPhotosBinding binding;
    private DatabaseReference rootRef;

    private final List<Post> photoPosts = new ArrayList<>();
    private final List<Post> allPhotoPosts = new ArrayList<>();
    private MediaAdapter mediaAdapter;
    private ChildEventListener childEventListener;
    private Query activeQuery;

    private boolean isLoadingMore = false;
    private String lastKey = null;
    private boolean hasMore = true;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPhotosBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        rootRef = FirebaseDatabase.getInstance().getReference();

        initViews();
        setupRecyclerView();
        loadPhotoPosts();
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
            allPhotoPosts.clear();
            loadPhotoPosts();
            binding.swipeRefresh.setRefreshing(false);
        });

        updateEmptyState();
    }

    private void setupRecyclerView() {
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, GRID_SPAN);
        binding.rvPhotos.setLayoutManager(gridLayoutManager);
        mediaAdapter = new MediaAdapter();
        binding.rvPhotos.setAdapter(mediaAdapter);
    }

    // ==================================================================
    // Load Photo Posts
    // ==================================================================

    private void loadPhotoPosts() {
        if (childEventListener != null && activeQuery != null) {
            activeQuery.removeEventListener(childEventListener);
        }

        allPhotoPosts.clear();
        photoPosts.clear();
        mediaAdapter.notifyDataSetChanged();

        activeQuery = rootRef.child(Constants.POSTS)
                .orderByChild("timestamp")
                .limitToLast(PAGE_SIZE);

        childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Post post = snapshot.getValue(Post.class);
                if (post != null && hasPhoto(post)) {
                    post.setPostId(snapshot.getKey());
                    allPhotoPosts.add(0, post);
                    Collections.sort(allPhotoPosts, (a, b) ->
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
                    for (int i = 0; i < allPhotoPosts.size(); i++) {
                        if (key.equals(allPhotoPosts.get(i).getPostId())) {
                            post.setPostId(key);
                            allPhotoPosts.set(i, post);
                            break;
                        }
                    }
                    refreshDisplayList();
                }
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                String key = snapshot.getKey();
                for (int i = 0; i < allPhotoPosts.size(); i++) {
                    if (key.equals(allPhotoPosts.get(i).getPostId())) {
                        allPhotoPosts.remove(i);
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
                Log.e(TAG, "loadPhotoPosts cancelled", error.toException());
            }
        };

        activeQuery.addChildEventListener(childEventListener);
    }

    private boolean hasPhoto(Post post) {
        if (post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
            return true;
        }
        List<String> images = post.getImages();
        return images != null && !images.isEmpty();
    }

    private void refreshDisplayList() {
        photoPosts.clear();
        photoPosts.addAll(allPhotoPosts);
        mediaAdapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void updatePaginationState() {
        if (!allPhotoPosts.isEmpty()) {
            String oldestKey = null;
            for (Post p : allPhotoPosts) {
                if (oldestKey == null || p.getPostId().compareTo(oldestKey) < 0) {
                    oldestKey = p.getPostId();
                }
            }
            lastKey = oldestKey;
            hasMore = allPhotoPosts.size() >= PAGE_SIZE;
        }
    }

    // ==================================================================
    // Pagination
    // ==================================================================

    private void setupScrollListener() {
        binding.rvPhotos.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                GridLayoutManager lm = (GridLayoutManager) recyclerView.getLayoutManager();
                if (lm == null) return;

                int visible = lm.getChildCount();
                int total = lm.getItemCount();
                int firstVisible = lm.findFirstVisibleItemPosition();

                if (!isLoadingMore && hasMore
                        && (visible + firstVisible) >= total - 6
                        && lastKey != null) {
                    loadMorePhotoPosts();
                }
            }
        });
    }

    private void loadMorePhotoPosts() {
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
                            if (post != null && hasPhoto(post)) {
                                post.setPostId(snap.getKey());
                                if (!containsPost(allPhotoPosts, post.getPostId())) {
                                    morePosts.add(post);
                                }
                            }
                        }

                        if (morePosts.isEmpty()) {
                            hasMore = false;
                            isLoadingMore = false;
                            return;
                        }

                        allPhotoPosts.addAll(morePosts);
                        Collections.sort(allPhotoPosts, (a, b) ->
                                Long.compare(b.getTimestamp(), a.getTimestamp()));
                        refreshDisplayList();
                        updatePaginationState();
                        isLoadingMore = false;
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "loadMorePhotoPosts cancelled", error.toException());
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
        if (photoPosts.isEmpty()) {
            binding.layoutEmpty.setVisibility(View.VISIBLE);
            binding.rvPhotos.setVisibility(View.GONE);
        } else {
            binding.layoutEmpty.setVisibility(View.GONE);
            binding.rvPhotos.setVisibility(View.VISIBLE);
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
            Post post = photoPosts.get(position);
            holder.bind(post);
        }

        @Override
        public int getItemCount() {
            return photoPosts.size();
        }

        class MediaViewHolder extends RecyclerView.ViewHolder {
            ImageView ivMedia;
            ImageView ivPlayIcon;
            TextView tvViews;
            TextView tvImageCount;

            MediaViewHolder(View itemView) {
                super(itemView);
                ivMedia = itemView.findViewById(R.id.ivMedia);
                ivPlayIcon = itemView.findViewById(R.id.ivPlayIcon);
                tvViews = itemView.findViewById(R.id.tvViews);
                tvImageCount = itemView.findViewById(R.id.tvImageCount);

                // Hide play icon for photos
                if (ivPlayIcon != null) {
                    ivPlayIcon.setVisibility(View.GONE);
                }
            }

            void bind(Post post) {
                // Determine the best image to show
                String displayImage = null;
                if (post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
                    displayImage = post.getImageUrl();
                } else if (post.getImages() != null && !post.getImages().isEmpty()) {
                    displayImage = post.getImages().get(0);
                }

                if (displayImage != null) {
                    loadImage(displayImage, ivMedia);
                } else {
                    ivMedia.setImageResource(R.drawable.ic_placeholder_image);
                }

                // Show image count badge if multiple images
                if (tvImageCount != null) {
                    List<String> images = post.getImages();
                    if (images != null && images.size() > 1) {
                        tvImageCount.setVisibility(View.VISIBLE);
                        tvImageCount.setText("+" + (images.size() - 1));
                    } else {
                        tvImageCount.setVisibility(View.GONE);
                    }
                }

                // Hide views text for photos
                if (tvViews != null) {
                    tvViews.setVisibility(View.GONE);
                }

                // Single click → open post details
                itemView.setOnClickListener(v -> {
                    Bundle bundle = new Bundle();
                    bundle.putString(Constants.EXTRA_POST_ID, post.getPostId());
                    openActivity(PostDetailsActivity.class, bundle);
                });

                // Long click → open image viewer (if single image)
                itemView.setOnLongClickListener(v -> {
                    String url = post.getImageUrl();
                    if (url == null || url.isEmpty()) {
                        List<String> imgs = post.getImages();
                        if (imgs != null && !imgs.isEmpty()) {
                            url = imgs.get(0);
                        }
                    }
                    if (url != null && !url.isEmpty()) {
                        openImageViewer(url);
                        return true;
                    }
                    return false;
                });
            }

            private void openImageViewer(String imageUrl) {
                Bundle bundle = new Bundle();
                bundle.putString("image_url", imageUrl);
                bundle.putStringArrayList("image_list", new ArrayList<>(
                        photoPosts.get(getAdapterPosition()).getImages() != null
                                ? photoPosts.get(getAdapterPosition()).getImages()
                                : new ArrayList<>()));
                openActivity(com.news.kimo.ui.activities.ImageViewerActivity.class, bundle);
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
