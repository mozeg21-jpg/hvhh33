package com.news.kimo.ui.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.news.kimo.R;
import com.news.kimo.databinding.ItemCommentBinding;
import com.news.kimo.models.Comment;
import com.news.kimo.ui.activities.ProfileActivity;
import com.news.kimo.utils.Constants;
import com.news.kimo.utils.DateUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private final Context context;
    private final List<Comment> commentList;
    private final String postId;
    private final String postOwnerUid;
    private final String currentUid;
    private final FirebaseFirestore db;
    private final Map<String, Boolean> likeCache;

    private OnReplyClickListener onReplyClickListener;
    private OnCommentDeleteListener onCommentDeleteListener;
    private OnCommentLikeListener onCommentLikeListener;

    public interface OnReplyClickListener {
        void onReplyClick(Comment comment);
    }

    public interface OnCommentDeleteListener {
        void onCommentDeleted(Comment comment, int position);
    }

    public interface OnCommentLikeListener {
        void onCommentLike(Comment comment, int position);
    }

    public CommentAdapter(Context context, List<Comment> commentList, String postId,
                          String postOwnerUid, String currentUid) {
        this.context = context;
        this.commentList = commentList != null ? commentList : new ArrayList<>();
        this.postId = postId;
        this.postOwnerUid = postOwnerUid;
        this.currentUid = currentUid;
        this.db = FirebaseFirestore.getInstance();
        this.likeCache = new HashMap<>();
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemCommentBinding binding = ItemCommentBinding.inflate(inflater, parent, false);
        return new CommentViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        holder.bind(commentList.get(position), position);
    }

    @Override
    public int getItemCount() {
        return commentList.size();
    }

    @Override
    public long getItemId(int position) {
        if (position < commentList.size() && commentList.get(position).getCommentId() != null) {
            return commentList.get(position).getCommentId().hashCode();
        }
        return RecyclerView.NO_ID;
    }

    // ---- ViewHolder ----

    class CommentViewHolder extends RecyclerView.ViewHolder {
        private final ItemCommentBinding binding;

        CommentViewHolder(ItemCommentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Comment comment, int position) {
            // Load commenter photo from users/{uid}/photoUrl with cache
            String cachedPhoto = com.news.kimo.utils.CacheHelper.getInstance().getUserPhoto(comment.getUid());
            if (cachedPhoto != null) {
                Glide.with(context)
                        .load(cachedPhoto)
                        .apply(RequestOptions.circleCropTransform()
                                .placeholder(R.drawable.ic_default_avatar)
                                .error(R.drawable.ic_default_avatar))
                        .into(binding.ivCommenterAvatar);
            } else {
                db.collection(Constants.USERS).document(comment.getUid())
                        .get()
                        .addOnSuccessListener(snapshot -> {
                            if (snapshot.exists()) {
                                String photoUrl = snapshot.getString("photoUrl");
                                String userName = snapshot.getString("name");
                                if (photoUrl != null) {
                                    com.news.kimo.utils.CacheHelper.getInstance().cacheUserPhoto(comment.getUid(), photoUrl);
                                    Glide.with(context)
                                            .load(photoUrl)
                                            .apply(RequestOptions.circleCropTransform()
                                                    .placeholder(R.drawable.ic_default_avatar)
                                                    .error(R.drawable.ic_default_avatar))
                                            .into(binding.ivCommenterAvatar);
                                }
                                if (userName != null && !userName.isEmpty()) {
                                    binding.tvCommenterName.setText(userName);
                                }
                            }
                        });
                // Load from comment data initially
                Glide.with(context)
                        .load(comment.getUserPhoto())
                        .apply(RequestOptions.circleCropTransform()
                                .placeholder(R.drawable.ic_default_avatar)
                                .error(R.drawable.ic_default_avatar))
                        .into(binding.ivCommenterAvatar);
            }

            // Avatar click -> profile
            binding.ivCommenterAvatar.setOnClickListener(v -> {
                context.startActivity(new Intent(context, ProfileActivity.class)
                        .putExtra(Constants.EXTRA_USER_ID, comment.getUid()));
            });

            // Name
            if (comment.getUserName() != null && !comment.getUserName().isEmpty()) {
                binding.tvCommenterName.setText(comment.getUserName());
            }
            binding.tvCommenterName.setOnClickListener(v -> {
                context.startActivity(new Intent(context, ProfileActivity.class)
                        .putExtra(Constants.EXTRA_USER_ID, comment.getUid()));
            });

            // Relative time
            binding.tvCommentTime.setText(DateUtils.formatRelativeTimeArabic(comment.getTimestamp()));

            // Delete button - visible if commenter or post owner
            boolean canDelete = comment.getUid().equals(currentUid) || postOwnerUid.equals(currentUid);
            binding.ivDeleteComment.setVisibility(canDelete ? View.VISIBLE : View.GONE);
            binding.ivDeleteComment.setOnClickListener(v -> deleteComment(comment, position));

            // Content
            binding.tvCommentContent.setText(comment.getText());

            // Comment image if present
            if (comment.getImageUrl() != null && !comment.getImageUrl().isEmpty()) {
                binding.ivCommentImage.setVisibility(View.VISIBLE);
                Glide.with(context)
                        .load(comment.getImageUrl())
                        .placeholder(R.drawable.ic_image_placeholder)
                        .error(R.drawable.ic_image_placeholder)
                        .into(binding.ivCommentImage);
            } else {
                binding.ivCommentImage.setVisibility(View.GONE);
            }

            // Reply button
            binding.btnReply.setOnClickListener(v -> {
                if (onReplyClickListener != null) {
                    onReplyClickListener.onReplyClick(comment);
                }
            });

            // Like button + count
            binding.tvLikeCount.setText(formatCount(comment.getLikesCount()));
            boolean isLiked = likeCache.containsKey(comment.getCommentId()) && likeCache.get(comment.getCommentId());
            if (isLiked) {
                binding.ivLike.setImageResource(R.drawable.ic_like_filled);
                binding.tvLikeCount.setTextColor(context.getColor(R.color.colorPrimary));
            } else {
                binding.ivLike.setImageResource(R.drawable.ic_like_outline);
                binding.tvLikeCount.setTextColor(context.getColor(R.color.textSecondary));
            }
            binding.ivLike.setOnClickListener(v -> {
                if (onCommentLikeListener != null) {
                    onCommentLikeListener.onCommentLike(comment, position);
                } else {
                    toggleLike(comment, position);
                }
            });
        }
    }

    // ---- Delete Comment ----

    private void deleteComment(Comment comment, int position) {
        db.collection(Constants.COMMENTS)
                .document(postId)
                .collection("items")
                .document(comment.getCommentId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // Decrement post commentsCount
                    db.collection(Constants.POSTS)
                            .document(postId)
                            .update("commentsCount", FieldValue.increment(-1));

                    commentList.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, commentList.size() - position);

                    if (onCommentDeleteListener != null) {
                        onCommentDeleteListener.onCommentDeleted(comment, position);
                    }
                });
    }

    // ---- Toggle Like ----

    private void toggleLike(Comment comment, int position) {
        boolean isCurrentlyLiked = likeCache.containsKey(comment.getCommentId()) && likeCache.get(comment.getCommentId());
        DocumentReference likeRef = db.collection(Constants.LIKES)
                .document(comment.getCommentId())
                .collection("items")
                .document(currentUid);

        if (isCurrentlyLiked) {
            likeRef.delete();
            likeCache.put(comment.getCommentId(), false);
            comment.setLikesCount(Math.max(0, comment.getLikesCount() - 1));
        } else {
            Map<String, Object> likeData = new HashMap<>();
            likeData.put("uid", currentUid);
            likeData.put("timestamp", System.currentTimeMillis());
            likeRef.set(likeData);
            likeCache.put(comment.getCommentId(), true);
            comment.setLikesCount(comment.getLikesCount() + 1);
        }
        notifyItemChanged(position);
    }

    // ---- Helpers ----

    private String formatCount(long count) {
        if (count <= 0) return "";
        if (count >= 1_000_000) return String.format("%.1fM", count / 1_000_000.0);
        if (count >= 1_000) return String.format("%.1fK", count / 1_000.0);
        return String.valueOf(count);
    }

    public void addComment(Comment comment) {
        commentList.add(0, comment);
        notifyItemInserted(0);
    }

    public void addComments(List<Comment> newComments) {
        int startPos = commentList.size();
        commentList.addAll(newComments);
        notifyItemRangeInserted(startPos, newComments.size());
    }

    public void removeComment(int position) {
        if (position >= 0 && position < commentList.size()) {
            commentList.remove(position);
            notifyItemRemoved(position);
        }
    }

    public void setLiked(String commentId, boolean liked) {
        likeCache.put(commentId, liked);
    }

    // ---- Setters ----

    public void setOnReplyClickListener(OnReplyClickListener listener) {
        this.onReplyClickListener = listener;
    }

    public void setOnCommentDeleteListener(OnCommentDeleteListener listener) {
        this.onCommentDeleteListener = listener;
    }

    public void setOnCommentLikeListener(OnCommentLikeListener listener) {
        this.onCommentLikeListener = listener;
    }
}