package com.news.kimo.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
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

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.news.kimo.R;
import com.news.kimo.databinding.ActivityHashtagBinding;
import com.news.kimo.models.Hashtag;
import com.news.kimo.models.Post;
import com.news.kimo.utils.Constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Displays all posts containing a specific hashtag.
 * Receives the hashtag name (without #) as an intent extra.
 * Supports pagination, pull-to-refresh, sharing, and empty state.
 */
public class HashtagActivity extends BaseActivity {

    private static final String TAG = "HashtagActivity";
    private static final int PAGE_SIZE = 20;

    private ActivityHashtagBinding binding;
    private DatabaseReference rootRef;

    private String hashtagName;
    private String hashtagLower;
    private Hashtag hashtagInfo;
    private long postCount = 0;

    private final List<Post> postList = new ArrayList<>();
    private PostAdapter postAdapter;
    private ValueEventListener postsListener;
    private ValueEventListener hashtagListener;
    private Query activePostsQuery;
    private String lastPostKey = null;
    private boolean isLoadingMore = false;
    private boolean hasMoreData = true;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHashtagBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        rootRef = FirebaseDatabase.getInstance().getReference();

        if (getIntent() != null && getIntent().hasExtra("hashtag")) {
            hashtagName = getIntent().getStringExtra("hashtag");
        }
        if (hashtagName == null || hashtagName.trim().isEmpty()) {
            hashtagName = "";
        }
        hashtagLower = hashtagName.toLowerCase(Locale.getDefault());

        initViews();
        setupRecyclerView();
        loadHashtagInfo();
        loadPosts();
        setupPagination();
    }

    // ==================================================================
    // Initialisation
    // ==================================================================

    private void initViews() {
        binding.ivBack.setOnClickListener(v -> onBackPressed());
        binding.toolbarTitle.setText("#" + hashtagName);

        // Pull to refresh
        binding.swipeRefresh.setOnRefreshListener(() -> {
            lastPostKey = null;
            hasMoreData = true;
            isLoadingMore = false;
            loadPosts();
            binding.swipeRefresh.setRefreshing(false);
        });

        // Share
        binding.ivShare.setOnClickListener(v -> shareHashtag());

        // Post count
        binding.tvPostCount.setVisibility(View.GONE);
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        binding.rvPosts.setLayoutManager(layoutManager);
        postAdapter = new PostAdapter();
        binding.rvPosts.setAdapter(postAdapter);
    }

    // ==================================================================
    // Load Hashtag Info
    // ==================================================================

    private void loadHashtagInfo() {
        hashtagListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Hashtag tag = snapshot.getValue(Hashtag.class);
                if (tag != null) {
                    hashtagInfo = tag;
                    postCount = tag.getPostCount();
                    binding.tvPostCount.setVisibility(View.VISIBLE);
                    binding.tvPostCount.setText(postCount + " " + getString(R.string.posts));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "loadHashtagInfo cancelled", error.toException());
            }
        };
        rootRef.child(Constants.HASHTAGS).child(hashtagLower)
                .addValueEventListener(hashtagListener);
    }

    // ==================================================================
    // Load Posts
    // ==================================================================

    private void loadPosts() {
        postList.clear();
        postAdapter.notifyDataSetChanged();

        Query query = rootRef.child(Constants.POSTS)
                .orderByChild("timestamp")
                .limitToLast(PAGE_SIZE);

        if (postsListener != null && activePostsQuery != null) {
            activePostsQuery.removeEventListener(postsListener);
        }

        postsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Post> newPosts = new ArrayList<>();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    Post post = snap.getValue(Post.class);
                    if (post != null) {
                        post.setPostId(snap.getKey());
                        if (containsHashtag(post)) {
                            newPosts.add(post);
                        }
                    }
                }
                Collections.sort(newPosts, (a, b) ->
                        Long.compare(b.getTimestamp(), a.getTimestamp()));

                postList.clear();
                postList.addAll(newPosts);
                postAdapter.notifyDataSetChanged();

                // Determine pagination key
                if (!snapshot.getChildren().iterator().hasNext()) {
                    hasMoreData = false;
                } else {
                    // Find the oldest key for pagination
                    DataSnapshot first = null;
                    for (DataSnapshot snap : snapshot.getChildren()) {
                        if (first == null || snap.getKey().compareTo(first.getKey()) < 0) {
                            first = snap;
                        }
                    }
                    if (first != null) {
                        lastPostKey = first.getKey();
                    }
                    hasMoreData = newPosts.size() >= PAGE_SIZE;
                }

                isLoadingMore = false;
                updateEmptyState();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "loadPosts cancelled", error.toException());
                isLoadingMore = false;
                updateEmptyState();
            }
        };

        activePostsQuery = query;
        query.addValueEventListener(postsListener);
    }

    private boolean containsHashtag(Post post) {
        List<String> tags = post.getTags();
        if (tags == null || tags.isEmpty()) return false;
        for (String tag : tags) {
            if (tag != null && tag.equalsIgnoreCase(hashtagLower)) {
                return true;
            }
        }
        // Also check in text for #hashtag
        if (post.getText() != null && post.getText().toLowerCase(Locale.getDefault())
                .contains("#" + hashtagLower)) {
            return true;
        }
        return false;
    }

    // ==================================================================
    // Pagination
    // ==================================================================

    private void setupPagination() {
        binding.rvPosts.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager lm = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (lm == null) return;

                int visibleItems = lm.getChildCount();
                int totalItems = lm.getItemCount();
                int firstVisible = lm.findFirstVisibleItemPosition();

                if (!isLoadingMore && hasMoreData
                        && (visibleItems + firstVisible) >= totalItems - 3
                        && lastPostKey != null) {
                    loadMorePosts();
                }
            }
        });
    }

    private void loadMorePosts() {
        isLoadingMore = true;

        Query query = rootRef.child(Constants.POSTS)
                .orderByChild("timestamp")
                .endAt(lastPostKey)
                .limitToLast(PAGE_SIZE);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Post> morePosts = new ArrayList<>();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    if (snap.getKey().equals(lastPostKey)) continue;
                    Post post = snap.getValue(Post.class);
                    if (post != null) {
                        post.setPostId(snap.getKey());
                        if (containsHashtag(post)) {
                            morePosts.add(post);
                        }
                    }
                }

                if (morePosts.isEmpty()) {
                    hasMoreData = false;
                    isLoadingMore = false;
                    return;
                }

                Collections.sort(morePosts, (a, b) ->
                        Long.compare(b.getTimestamp(), a.getTimestamp()));

                int before = postList.size();
                postList.addAll(morePosts);
                postAdapter.notifyItemRangeInserted(before, morePosts.size());

                // Update last key
                DataSnapshot first = null;
                for (DataSnapshot snap : snapshot.getChildren()) {
                    if (first == null || snap.getKey().compareTo(first.getKey()) < 0) {
                        first = snap;
                    }
                }
                if (first != null) {
                    lastPostKey = first.getKey();
                }

                hasMoreData = morePosts.size() >= PAGE_SIZE;
                isLoadingMore = false;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "loadMorePosts cancelled", error.toException());
                isLoadingMore = false;
            }
        });
    }

    // ==================================================================
    // Empty State
    // ==================================================================

    private void updateEmptyState() {
        if (postList.isEmpty()) {
            binding.layoutEmpty.setVisibility(View.VISIBLE);
            binding.rvPosts.setVisibility(View.GONE);
        } else {
            binding.layoutEmpty.setVisibility(View.GONE);
            binding.rvPosts.setVisibility(View.VISIBLE);
        }
    }

    // ==================================================================
    // Share
    // ==================================================================

    private void shareHashtag() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, "#" + hashtagName + " - " +
                getString(R.string.app_name));
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share)));
    }

    // ==================================================================
    // Post Adapter
    // ==================================================================

    private class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

        @NonNull
        @Override
        public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_post, parent, false);
            return new PostViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
            Post post = postList.get(position);
            holder.bind(post);
        }

        @Override
        public int getItemCount() {
            return postList.size();
        }

        class PostViewHolder extends RecyclerView.ViewHolder {
            ImageView ivUserPhoto, ivPostImage, ivVideoIcon, ivShare, ivMore;
            TextView tvUserName, tvText, tvTime, tvLikes, tvComments, tvShares;
            View layoutStats;

            PostViewHolder(View itemView) {
                super(itemView);
                ivUserPhoto = itemView.findViewById(R.id.ivUserPhoto);
                ivPostImage = itemView.findViewById(R.id.ivPostImage);
                ivVideoIcon = itemView.findViewById(R.id.ivVideoIcon);
                ivShare = itemView.findViewById(R.id.ivShare);
                ivMore = itemView.findViewById(R.id.ivMore);
                tvUserName = itemView.findViewById(R.id.tvUserName);
                tvText = itemView.findViewById(R.id.tvText);
                tvTime = itemView.findViewById(R.id.tvTime);
                tvLikes = itemView.findViewById(R.id.tvLikes);
                tvComments = itemView.findViewById(R.id.tvComments);
                tvShares = itemView.findViewById(R.id.tvShares);
                layoutStats = itemView.findViewById(R.id.layoutStats);
            }

            void bind(Post post) {
                tvUserName.setText(post.getUserName() != null ? post.getUserName() : "");
                loadCircularImage(post.getUserPhoto(), ivUserPhoto);

                if (post.getText() != null && !post.getText().isEmpty()) {
                    tvText.setVisibility(View.VISIBLE);
                    tvText.setText(post.getText());
                } else {
                    tvText.setVisibility(View.GONE);
                }

                tvTime.setText(getRelativeTime(post.getTimestamp()));

                if (post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
                    ivPostImage.setVisibility(View.VISIBLE);
                    loadImage(post.getImageUrl(), ivPostImage);
                } else {
                    ivPostImage.setVisibility(View.GONE);
                }

                if (post.getVideoUrl() != null && !post.getVideoUrl().isEmpty()) {
                    ivVideoIcon.setVisibility(View.VISIBLE);
                } else {
                    ivVideoIcon.setVisibility(View.GONE);
                }

                if (layoutStats != null) {
                    tvLikes.setText(String.valueOf(post.getLikesCount()));
                    tvComments.setText(String.valueOf(post.getCommentsCount()));
                    tvShares.setText(String.valueOf(post.getSharesCount()));
                }

                if (ivShare != null) {
                    ivShare.setOnClickListener(v -> {
                        Intent intent = new Intent(Intent.ACTION_SEND);
                        intent.setType("text/plain");
                        intent.putExtra(Intent.EXTRA_TEXT, post.getText());
                        startActivity(Intent.createChooser(intent, getString(R.string.share)));
                    });
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
        if (postsListener != null && activePostsQuery != null) {
            activePostsQuery.removeEventListener(postsListener);
        }
        if (hashtagListener != null) {
            rootRef.child(Constants.HASHTAGS).child(hashtagLower)
                    .removeEventListener(hashtagListener);
        }
        super.onDestroy();
    }
}
