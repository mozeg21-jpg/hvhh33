package com.news.kimo.ui.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.news.kimo.R;
import com.news.kimo.databinding.ActivityItemPostBinding;
import com.news.kimo.models.Post;
import com.news.kimo.models.SavedPost;
import com.news.kimo.ui.activities.PostDetailsActivity;
import com.news.kimo.ui.activities.ProfileActivity;
import com.news.kimo.utils.Constants;
import com.news.kimo.utils.DateUtils;
import com.news.kimo.utils.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SavedPostAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_POST = 0;
    private static final int TYPE_LOADING = 1;

    private final Context context;
    private final List<SavedPostItem> items;
    private final String currentUid;
    private final FirebaseFirestore db;
    private boolean isLoadingAdded = false;

    private OnUnsaveClickListener onUnsaveClickListener;
    private PostAdapter.OnPostClickListener onPostClickListener;

    public interface OnUnsaveClickListener {
        void onUnsaveClick(SavedPost savedPost, int position);
    }

    // ---- Wrapper to hold SavedPost + resolved Post ----
    public static class SavedPostItem {
        public SavedPost savedPost;
        public Post post;
        public boolean isLoaded;

        public SavedPostItem(SavedPost savedPost) {
            this.savedPost = savedPost;
            this.isLoaded = false;
        }
    }

    public SavedPostAdapter(Context context, List<SavedPost> savedPosts, String currentUid) {
        this.context = context;
        this.items = new ArrayList<>();
        this.currentUid = currentUid;
        this.db = FirebaseFirestore.getInstance();

        for (SavedPost sp : savedPosts) {
            items.add(new SavedPostItem(sp));
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position < items.size()) {
            return TYPE_POST;
        }
        return TYPE_LOADING;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_LOADING) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_loading_footer, parent, false);
            return new LoadingViewHolder(view);
        }
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ActivityItemPostBinding binding = ActivityItemPostBinding.inflate(inflater, parent, false);
        return new SavedPostViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof SavedPostViewHolder) {
            SavedPostItem item = items.get(position);
            if (!item.isLoaded && item.savedPost.getPostId() != null) {
                loadPostDetails(item, position);
            } else if (item.isLoaded && item.post != null) {
                ((SavedPostViewHolder) holder).bind(item, position);
            }
        }
    }

    @Override
    public int getItemCount() {
        return items.size() + (isLoadingAdded ? 1 : 0);
    }

    // ---- Load Post Details ----

    private void loadPostDetails(SavedPostItem item, int position) {
        db.collection(Constants.POSTS)
                .document(item.savedPost.getPostId())
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        Post post = snapshot.toObject(Post.class);
                        if (post != null) {
                            item.post = post;
                            item.isLoaded = true;
                            notifyItemChanged(position);
                        }
                    }
                });
    }

    // ---- ViewHolder ----

    class SavedPostViewHolder extends RecyclerView.ViewHolder {
        private final ActivityItemPostBinding binding;

        SavedPostViewHolder(ActivityItemPostBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(SavedPostItem item, int position) {
            Post post = item.post;
            SavedPost savedPost = item.savedPost;

            // User avatar
            Glide.with(context)
                    .load(post.getUserPhoto())
                    .apply(RequestOptions.circleCropTransform()
                            .placeholder(R.drawable.ic_default_avatar)
                            .error(R.drawable.ic_default_avatar))
                    .into(binding.ivUserAvatar);

            binding.ivUserAvatar.setOnClickListener(v -> {
                context.startActivity(new Intent(context, ProfileActivity.class)
                        .putExtra(Constants.EXTRA_USER_ID, post.getUid()));
            });

            // User name
            binding.tvUserName.setText(post.getUserName());
            binding.tvUserName.setOnClickListener(v -> {
                context.startActivity(new Intent(context, ProfileActivity.class)
                        .putExtra(Constants.EXTRA_USER_ID, post.getUid()));
            });

            // Verified badge
            binding.ivVerified.setVisibility(View.GONE);

            // Relative date
            binding.tvPostDate.setText(DateUtils.formatRelativeTimeArabic(post.getTimestamp()));

            // More menu
            binding.ivMore.setOnClickListener(v -> {
                // Show unsave option in menu
                android.widget.PopupMenu popup = new android.widget.PopupMenu(context, v);
                popup.getMenu().add(0, 1, 0, R.string.unsave_post);
                popup.setOnMenuItemClickListener(menuItem -> {
                    if (menuItem.getItemId() == 1) {
                        unsavePost(savedPost, position);
                        if (onUnsaveClickListener != null) {
                            onUnsaveClickListener.onUnsaveClick(savedPost, position);
                        }
                    }
                    return true;
                });
                popup.show();
            });

            // Post text with highlighting
            if (post.getText() != null && !post.getText().isEmpty()) {
                binding.tvPostText.setVisibility(View.VISIBLE);
                int hashtagColor = context.getColor(R.color.colorPrimary);
                int mentionColor = context.getColor(R.color.colorAccent);
                binding.tvPostText.setText(StringUtils.highlightAll(post.getText(), hashtagColor, mentionColor));
                binding.tvPostText.setMaxLines(4);
            } else {
                binding.tvPostText.setVisibility(View.GONE);
            }

            // Images ViewPager2
            if (post.getImages() != null && !post.getImages().isEmpty()) {
                binding.viewPagerImages.setVisibility(View.VISIBLE);
                binding.singleImageContainer.setVisibility(View.GONE);
                PostImagePagerAdapter pagerAdapter = new PostImagePagerAdapter(context, post.getImages(), (images, index) -> {
                    if (onPostClickListener != null) {
                        onPostClickListener.onImageClick(images, index);
                    }
                });
                binding.viewPagerImages.setAdapter(pagerAdapter);
                if (binding.dotsIndicator != null) {
                    binding.dotsIndicator.setViewPager2(binding.viewPagerImages);
                }
            } else if (post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
                binding.viewPagerImages.setVisibility(View.GONE);
                binding.singleImageContainer.setVisibility(View.VISIBLE);
                Glide.with(context)
                        .load(post.getImageUrl())
                        .placeholder(R.drawable.ic_image_placeholder)
                        .into(binding.ivSingleImage);
            } else {
                binding.viewPagerImages.setVisibility(View.GONE);
                binding.singleImageContainer.setVisibility(View.GONE);
            }

            // Video
            if (post.getVideoUrl() != null && !post.getVideoUrl().isEmpty()) {
                binding.videoContainer.setVisibility(View.VISIBLE);
                binding.ivPlayIcon.setVisibility(View.VISIBLE);
                Glide.with(context)
                        .load(post.getImageUrl())
                        .placeholder(R.drawable.ic_video_placeholder)
                        .into(binding.ivVideoThumbnail);
            } else {
                binding.videoContainer.setVisibility(View.GONE);
            }

            // Poll
            if (post.getPollOptions() != null && !post.getPollOptions().isEmpty()) {
                binding.pollContainer.setVisibility(View.VISIBLE);
            } else {
                binding.pollContainer.setVisibility(View.GONE);
            }

            // Quote card
            if (post.getQuoteText() != null && !post.getQuoteText().isEmpty()) {
                binding.quoteCard.setVisibility(View.VISIBLE);
                binding.tvQuoteText.setText(post.getQuoteText());
                binding.tvQuoteAuthor.setText(post.getQuoteAuthor() != null ? post.getQuoteAuthor() : "");
            } else {
                binding.quoteCard.setVisibility(View.GONE);
            }

            // Code card
            if (post.getCodeContent() != null && !post.getCodeContent().isEmpty()) {
                binding.codeCard.setVisibility(View.VISIBLE);
                binding.tvCodeContent.setText(post.getCodeContent());
            } else {
                binding.codeCard.setVisibility(View.GONE);
            }

            // Link preview
            if (post.getLinkUrl() != null && !post.getLinkUrl().isEmpty()) {
                binding.linkPreviewCard.setVisibility(View.VISIBLE);
            } else {
                binding.linkPreviewCard.setVisibility(View.GONE);
            }

            // Action row
            formatCount(binding.tvLikeCount, post.getLikesCount());
            formatCount(binding.tvCommentCount, post.getCommentsCount());
            formatCount(binding.tvShareCount, post.getSharesCount());

            // Unsave button override
            binding.ivSave.setImageResource(R.drawable.ic_bookmark_filled);
            binding.ivSave.setOnClickListener(v -> {
                unsavePost(savedPost, position);
                if (onUnsaveClickListener != null) {
                    onUnsaveClickListener.onUnsaveClick(savedPost, position);
                }
            });

            // Reactions row
            if (post.getReactions() != null && !post.getReactions().isEmpty()) {
                binding.reactionsContainer.setVisibility(View.VISIBLE);
            } else {
                binding.reactionsContainer.setVisibility(View.GONE);
            }

            // Card click -> PostDetailsActivity
            binding.cardPost.setOnClickListener(v -> {
                context.startActivity(new Intent(context, PostDetailsActivity.class)
                        .putExtra(Constants.EXTRA_POST_ID, post.getPostId()));
            });
        }

        private void formatCount(android.widget.TextView tv, long count) {
            if (count <= 0) {
                tv.setText("");
            } else if (count >= 1_000_000) {
                tv.setText(String.format("%.1fM", count / 1_000_000.0));
            } else if (count >= 1_000) {
                tv.setText(String.format("%.1fK", count / 1_000.0));
            } else {
                tv.setText(String.valueOf(count));
            }
        }
    }

    // ---- Unsave Post ----

    private void unsavePost(SavedPost savedPost, int position) {
        db.collection(Constants.SAVED_POSTS)
                .document(currentUid)
                .collection("items")
                .document(savedPost.getSavedId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    items.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, items.size() - position);
                });
    }

    // ---- Loading Footer ----

    static class LoadingViewHolder extends RecyclerView.ViewHolder {
        ProgressBar progressBar;

        LoadingViewHolder(@NonNull View itemView) {
            super(itemView);
            progressBar = itemView.findViewById(R.id.progressBar);
        }
    }

    // ---- Pagination ----

    public void addLoadingFooter() {
        if (!isLoadingAdded) {
            isLoadingAdded = true;
            notifyItemInserted(getItemCount() - 1);
        }
    }

    public void removeLoadingFooter() {
        if (isLoadingAdded) {
            isLoadingAdded = false;
            notifyItemRemoved(getItemCount());
        }
    }

    public void addSavedPosts(List<SavedPost> newSavedPosts) {
        int startPos = items.size();
        for (SavedPost sp : newSavedPosts) {
            items.add(new SavedPostItem(sp));
        }
        notifyItemRangeInserted(startPos, newSavedPosts.size());
    }

    public void removeSavedPost(int position) {
        if (position >= 0 && position < items.size()) {
            items.remove(position);
            notifyItemRemoved(position);
        }
    }

    // ---- Setters ----

    public void setOnUnsaveClickListener(OnUnsaveClickListener listener) {
        this.onUnsaveClickListener = listener;
    }

    public void setOnPostClickListener(PostAdapter.OnPostClickListener listener) {
        this.onPostClickListener = listener;
    }
}