package com.news.kimo.ui.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.news.kimo.R;
import com.news.kimo.databinding.ItemMediaBinding;
import com.news.kimo.models.Post;
import com.news.kimo.ui.activities.PostDetailsActivity;
import com.news.kimo.utils.Constants;

import java.util.ArrayList;
import java.util.List;

public class MediaAdapter extends RecyclerView.Adapter<MediaAdapter.MediaViewHolder> {

    private final Context context;
    private final List<Post> mediaPostList;

    public MediaAdapter(Context context, List<Post> mediaPostList) {
        this.context = context;
        this.mediaPostList = mediaPostList != null ? mediaPostList : new ArrayList<>();
    }

    @NonNull
    @Override
    public MediaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemMediaBinding binding = ItemMediaBinding.inflate(inflater, parent, false);
        return new MediaViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MediaViewHolder holder, int position) {
        holder.bind(mediaPostList.get(position), position);
    }

    @Override
    public int getItemCount() {
        return mediaPostList.size();
    }

    @Override
    public long getItemId(int position) {
        if (position < mediaPostList.size() && mediaPostList.get(position).getPostId() != null) {
            return mediaPostList.get(position).getPostId().hashCode();
        }
        return RecyclerView.NO_ID;
    }

    // ---- ViewHolder ----

    class MediaViewHolder extends RecyclerView.ViewHolder {
        private final ItemMediaBinding binding;

        MediaViewHolder(ItemMediaBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Post post, int position) {
            // Determine which image to show
            String imageUrl = null;
            boolean isVideo = false;

            if (post.getVideoUrl() != null && !post.getVideoUrl().isEmpty()) {
                isVideo = true;
                // Show first image as video thumbnail, or videoUrl if no image
                if (post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
                    imageUrl = post.getImageUrl();
                }
            } else if (post.getImages() != null && !post.getImages().isEmpty()) {
                imageUrl = post.getImages().get(0);
            } else if (post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
                imageUrl = post.getImageUrl();
            }

            // Load the image (square, rounded)
            if (imageUrl != null) {
                Glide.with(context)
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_image_placeholder)
                        .error(R.drawable.ic_image_placeholder)
                        .centerCrop()
                        .into(binding.ivMedia);
            } else if (isVideo) {
                binding.ivMedia.setImageResource(R.drawable.ic_video_placeholder);
            }

            // Video play icon (if videoUrl)
            if (isVideo) {
                binding.ivPlayIcon.setVisibility(View.VISIBLE);
            } else {
                binding.ivPlayIcon.setVisibility(View.GONE);
            }

            // Duration text for videos
            if (isVideo && post.getVideoUrl() != null) {
                // Duration could be parsed from post metadata or left empty
                binding.tvDuration.setVisibility(View.GONE);
            } else {
                binding.tvDuration.setVisibility(View.GONE);
            }

            // Multi-image badge
            if (post.getImages() != null && post.getImages().size() > 1) {
                binding.ivMultiImageBadge.setVisibility(View.VISIBLE);
                binding.tvImageCount.setText(String.valueOf(post.getImages().size()));
                binding.tvImageCount.setVisibility(View.VISIBLE);
            } else {
                binding.ivMultiImageBadge.setVisibility(View.GONE);
                binding.tvImageCount.setVisibility(View.GONE);
            }

            // Click -> PostDetailsActivity
            itemView.setOnClickListener(v -> {
                context.startActivity(new Intent(context, PostDetailsActivity.class)
                        .putExtra(Constants.EXTRA_POST_ID, post.getPostId()));
            });
        }
    }

    // ---- Data Operations ----

    public void addMediaPosts(List<Post> posts) {
        // Filter for posts that have media
        List<Post> filtered = new ArrayList<>();
        for (Post post : posts) {
            if (hasMedia(post)) {
                filtered.add(post);
            }
        }
        int startPos = mediaPostList.size();
        mediaPostList.addAll(filtered);
        notifyItemRangeInserted(startPos, filtered.size());
    }

    private boolean hasMedia(Post post) {
        return (post.getImageUrl() != null && !post.getImageUrl().isEmpty())
                || (post.getVideoUrl() != null && !post.getVideoUrl().isEmpty())
                || (post.getImages() != null && !post.getImages().isEmpty());
    }

    public void setMediaPosts(List<Post> posts) {
        mediaPostList.clear();
        for (Post post : posts) {
            if (hasMedia(post)) {
                mediaPostList.add(post);
            }
        }
        notifyDataSetChanged();
    }

    public void clear() {
        mediaPostList.clear();
        notifyDataSetChanged();
    }

    public Post getPost(int position) {
        if (position >= 0 && position < mediaPostList.size()) {
            return mediaPostList.get(position);
        }
        return null;
    }

    public List<Post> getMediaPostList() {
        return mediaPostList;
    }
}