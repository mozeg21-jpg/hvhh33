package com.news.kimo.ui.adapters;

import android.content.Context;
import android.content.Intent;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.news.kimo.R;
import com.news.kimo.databinding.ActivityItemPostBinding;
import com.news.kimo.models.Post;
import com.news.kimo.ui.activities.PostDetailsActivity;
import com.news.kimo.ui.activities.ProfileActivity;
import com.news.kimo.utils.Constants;
import com.news.kimo.utils.DateUtils;
import com.news.kimo.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PostAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_POST = 0;
    private static final int TYPE_LOADING = 1;

    private final Context context;
    private final List<Post> postList;
    private final boolean showFullContent;
    private final String currentUid;
    private boolean isLoadingAdded = false;

    private OnPostClickListener onPostClickListener;
    private OnPostOptionClickListener onPostOptionClickListener;
    private OnReactionClickListener onReactionClickListener;

    private static final int MAX_LINES_COLLAPSED = 4;
    private static final String[] REACTION_EMOJIS = {
            Constants.REACTION_LIKE, Constants.REACTION_LOVE, Constants.REACTION_HAHA,
            Constants.REACTION_WOW, Constants.REACTION_SAD, Constants.REACTION_ANGRY
    };

    public interface OnPostClickListener {
        void onPostClick(Post post, int position);
        void onUserClick(String uid);
        void onHashtagClick(String hashtag);
        void onMentionClick(String mention);
        void onImageClick(List<String> images, int index);
        void onVideoClick(String videoUrl);
        void onCommentClick(Post post);
        void onShareClick(Post post);
        void onSaveClick(Post post, int position);
        void onRepostClick(Post post);
    }

    public interface OnPostOptionClickListener {
        void onOptionClick(Post post, View anchor);
        void onPollVoteClick(Post post, int optionIndex);
        void onLinkPreviewClick(String url);
    }

    public interface OnReactionClickListener {
        void onReactionClick(Post post, String reactionType);
        void onReactionLongClick(Post post, View anchor);
    }

    public PostAdapter(Context context, List<Post> postList, boolean showFullContent, String currentUid) {
        this.context = context;
        this.postList = postList != null ? postList : new ArrayList<>();
        this.showFullContent = showFullContent;
        this.currentUid = currentUid;
    }

    @Override
    public int getItemViewType(int position) {
        if (position < postList.size()) {
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
        return new PostViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof PostViewHolder) {
            ((PostViewHolder) holder).bind(postList.get(position), position);
        }
    }

    @Override
    public int getItemCount() {
        return postList.size() + (isLoadingAdded ? 1 : 0);
    }

    @Override
    public long getItemId(int position) {
        if (position < postList.size() && postList.get(position).getPostId() != null) {
            return postList.get(position).getPostId().hashCode();
        }
        return RecyclerView.NO_ID;
    }

    // ---- ViewHolder ----

    class PostViewHolder extends RecyclerView.ViewHolder {
        private final ActivityItemPostBinding binding;

        PostViewHolder(ActivityItemPostBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Post post, int position) {
            // User avatar
            Glide.with(context)
                    .load(post.getUserPhoto())
                    .apply(RequestOptions.circleCropTransform()
                            .placeholder(R.drawable.ic_default_avatar)
                            .error(R.drawable.ic_default_avatar))
                    .into(binding.ivUserAvatar);

            binding.ivUserAvatar.setOnClickListener(v -> {
                if (onPostClickListener != null) {
                    onPostClickListener.onUserClick(post.getUid());
                } else {
                    context.startActivity(new Intent(context, ProfileActivity.class)
                            .putExtra(Constants.EXTRA_USER_ID, post.getUid()));
                }
            });

            // User name
            binding.tvUserName.setText(post.getUserName());
            binding.tvUserName.setOnClickListener(v -> {
                if (onPostClickListener != null) {
                    onPostClickListener.onUserClick(post.getUid());
                } else {
                    context.startActivity(new Intent(context, ProfileActivity.class)
                            .putExtra(Constants.EXTRA_USER_ID, post.getUid()));
                }
            });

            // Verified badge
            binding.ivVerified.setVisibility(View.GONE);

            // Relative date
            binding.tvPostDate.setText(DateUtils.formatRelativeTimeArabic(post.getTimestamp()));

            // More menu
            binding.ivMore.setOnClickListener(v -> {
                if (onPostOptionClickListener != null) {
                    onPostOptionClickListener.onOptionClick(post, v);
                }
            });

            // Post text with hashtag/mention highlighting
            if (post.getText() != null && !post.getText().isEmpty()) {
                binding.tvPostText.setVisibility(View.VISIBLE);
                int hashtagColor = context.getColor(R.color.colorPrimary);
                int mentionColor = context.getColor(R.color.colorAccent);
                binding.tvPostText.setText(StringUtils.highlightAll(post.getText(), hashtagColor, mentionColor));
                binding.tvPostText.setMovementMethod(LinkMovementMethod.getInstance());
                if (!showFullContent) {
                    binding.tvPostText.setMaxLines(MAX_LINES_COLLAPSED);
                } else {
                    binding.tvPostText.setMaxLines(Integer.MAX_VALUE);
                }
            } else {
                binding.tvPostText.setVisibility(View.GONE);
            }

            // Images ViewPager2
            if (post.getImages() != null && !post.getImages().isEmpty()) {
                binding.viewPagerImages.setVisibility(View.VISIBLE);
                binding.singleImageContainer.setVisibility(View.GONE);
                // Setup ViewPager2 adapter for multiple images
                PostImagePagerAdapter pagerAdapter = new PostImagePagerAdapter(context, post.getImages(), (images, index) -> {
                    if (onPostClickListener != null) {
                        onPostClickListener.onImageClick(images, index);
                    }
                });
                binding.viewPagerImages.setAdapter(pagerAdapter);
                binding.dotsIndicator.setViewPager2(binding.viewPagerImages);
            } else if (post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
                binding.viewPagerImages.setVisibility(View.GONE);
                binding.singleImageContainer.setVisibility(View.VISIBLE);
                Glide.with(context)
                        .load(post.getImageUrl())
                        .placeholder(R.drawable.ic_image_placeholder)
                        .error(R.drawable.ic_image_placeholder)
                        .into(binding.ivSingleImage);
                binding.ivSingleImage.setOnClickListener(v -> {
                    if (onPostClickListener != null) {
                        List<String> single = new ArrayList<>();
                        single.add(post.getImageUrl());
                        onPostClickListener.onImageClick(single, 0);
                    }
                });
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
                binding.videoContainer.setOnClickListener(v -> {
                    if (onPostClickListener != null) {
                        onPostClickListener.onVideoClick(post.getVideoUrl());
                    }
                });
            } else {
                binding.videoContainer.setVisibility(View.GONE);
            }

            // Poll
            if (post.getPollOptions() != null && !post.getPollOptions().isEmpty()) {
                binding.pollContainer.setVisibility(View.VISIBLE);
                binding.pollOptionsLayout.removeAllViews();
                long totalVotes = 0;
                Map<String, Long> votes = post.getPollVotes();
                if (votes != null) {
                    for (Long count : votes.values()) {
                        totalVotes += count;
                    }
                }
                for (int i = 0; i < post.getPollOptions().size(); i++) {
                    Map<String, Object> option = post.getPollOptions().get(i);
                    String optionText = (String) option.get("text");
                    long voteCount = 0;
                    if (votes != null && votes.containsKey(String.valueOf(i))) {
                        voteCount = votes.get(String.valueOf(i));
                    }
                    View pollView = LayoutInflater.from(context).inflate(R.layout.item_poll_option, binding.pollOptionsLayout, false);
                    // Bind poll option views programmatically
                    ProgressBar voteBar = pollView.findViewById(R.id.progressBar_vote);
                    if (totalVotes > 0) {
                        voteBar.setProgress((int) ((voteCount * 100) / totalVotes));
                    }
                    final int optionIndex = i;
                    pollView.setOnClickListener(v -> {
                        if (onPostOptionClickListener != null) {
                            onPostOptionClickListener.onPollVoteClick(post, optionIndex);
                        }
                    });
                    binding.pollOptionsLayout.addView(pollView);
                }
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
                binding.tvCodeLanguage.setText(post.getCodeLanguage() != null ? post.getCodeLanguage() : "code");
            } else {
                binding.codeCard.setVisibility(View.GONE);
            }

            // Link preview
            if (post.getLinkUrl() != null && !post.getLinkUrl().isEmpty()) {
                binding.linkPreviewCard.setVisibility(View.VISIBLE);
                binding.linkPreviewCard.setOnClickListener(v -> {
                    if (onPostOptionClickListener != null) {
                        onPostOptionClickListener.onLinkPreviewClick(post.getLinkUrl());
                    }
                });
            } else {
                binding.linkPreviewCard.setVisibility(View.GONE);
            }

            // Action row - Like
            formatCount(binding.tvLikeCount, post.getLikesCount());
            binding.ivLike.setOnClickListener(v -> {
                if (onPostClickListener != null) {
                    onPostClickListener.onReactionClick(post, Constants.REACTION_LIKE);
                }
            });

            // Comment
            formatCount(binding.tvCommentCount, post.getCommentsCount());
            binding.ivComment.setOnClickListener(v -> {
                if (onPostClickListener != null) {
                    onPostClickListener.onCommentClick(post);
                } else {
                    context.startActivity(new Intent(context, PostDetailsActivity.class)
                            .putExtra(Constants.EXTRA_POST_ID, post.getPostId()));
                }
            });

            // Share
            formatCount(binding.tvShareCount, post.getSharesCount());
            binding.ivShare.setOnClickListener(v -> {
                if (onPostClickListener != null) {
                    onPostClickListener.onShareClick(post);
                }
            });

            // Save (bookmark toggle)
            binding.ivSave.setOnClickListener(v -> {
                if (onPostClickListener != null) {
                    onPostClickListener.onSaveClick(post, position);
                }
            });

            // Repost
            binding.ivRepost.setOnClickListener(v -> {
                if (onPostClickListener != null) {
                    onPostClickListener.onRepostClick(post);
                }
            });

            // Reactions row
            if (post.getReactions() != null && !post.getReactions().isEmpty()) {
                binding.reactionsContainer.setVisibility(View.VISIBLE);
                binding.reactionsContainer.removeAllViews();
                int count = 0;
                for (String reactionType : REACTION_EMOJIS) {
                    Long reactionCount = post.getReactions().get(reactionType);
                    if (reactionCount != null && reactionCount > 0) {
                        View reactionView = LayoutInflater.from(context).inflate(R.layout.item_reaction_chip, binding.reactionsContainer, false);
                        reactionView.setOnClickListener(v -> {
                            if (onReactionClickListener != null) {
                                onReactionClickListener.onReactionClick(post, reactionType);
                            }
                        });
                        binding.reactionsContainer.addView(reactionView);
                        count++;
                        if (count >= 6) break;
                    }
                }
            } else {
                binding.reactionsContainer.setVisibility(View.GONE);
            }

            // Card click -> PostDetailsActivity
            binding.cardPost.setOnClickListener(v -> {
                context.startActivity(new Intent(context, PostDetailsActivity.class)
                        .putExtra(Constants.EXTRA_POST_ID, post.getPostId()));
                if (onPostClickListener != null) {
                    onPostClickListener.onPostClick(post, position);
                }
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

    // ---- Loading Footer ----

    static class LoadingViewHolder extends RecyclerView.ViewHolder {
        ProgressBar progressBar;

        LoadingViewHolder(@NonNull View itemView) {
            super(itemView);
            progressBar = itemView.findViewById(R.id.progressBar);
        }
    }

    // ---- Pagination Helpers ----

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

    public void addPost(Post post) {
        postList.add(0, post);
        notifyItemInserted(0);
    }

    public void addPosts(List<Post> newPosts) {
        int startPos = postList.size();
        postList.addAll(newPosts);
        notifyItemRangeInserted(startPos, newPosts.size());
    }

    public void removePost(int position) {
        if (position >= 0 && position < postList.size()) {
            postList.remove(position);
            notifyItemRemoved(position);
        }
    }

    public void updatePost(int position, Post post) {
        if (position >= 0 && position < postList.size()) {
            postList.set(position, post);
            notifyItemChanged(position);
        }
    }

    public Post getPost(int position) {
        if (position >= 0 && position < postList.size()) {
            return postList.get(position);
        }
        return null;
    }

    public List<Post> getPostList() {
        return postList;
    }

    // ---- Setters ----

    public void setOnPostClickListener(OnPostClickListener listener) {
        this.onPostClickListener = listener;
    }

    public void setOnPostOptionClickListener(OnPostOptionClickListener listener) {
        this.onPostOptionClickListener = listener;
    }

    public void setOnReactionClickListener(OnReactionClickListener listener) {
        this.onReactionClickListener = listener;
    }
}
