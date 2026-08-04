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
import com.news.kimo.R;
import com.news.kimo.databinding.ItemSearchUserBinding;
import com.news.kimo.databinding.ItemSearchPostBinding;
import com.news.kimo.databinding.ItemSearchHashtagBinding;
import com.news.kimo.models.Post;
import com.news.kimo.models.User;
import com.news.kimo.ui.activities.HashtagActivity;
import com.news.kimo.ui.activities.PostDetailsActivity;
import com.news.kimo.ui.activities.ProfileActivity;
import com.news.kimo.utils.Constants;
import com.news.kimo.utils.DateUtils;
import com.news.kimo.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SearchAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int TYPE_USER = 0;
    public static final int TYPE_POST = 1;
    public static final int TYPE_HASHTAG = 2;

    private final Context context;
    private final List<SearchItem> searchItems;

    public SearchAdapter(Context context) {
        this.context = context;
        this.searchItems = new ArrayList<>();
    }

    // ---- Wrapper for heterogeneous items ----

    public static class SearchItem {
        public static final String TYPE_USER = "user";
        public static final String TYPE_POST = "post";
        public static final String TYPE_HASHTAG = "hashtag";

        public final String type;
        public Object data;
        public String hashtagName;
        public long postCount;

        public SearchItem(String type, Object data) {
            this.type = type;
            this.data = data;
        }

        public SearchItem(String type, String hashtagName, long postCount) {
            this.type = type;
            this.hashtagName = hashtagName;
            this.postCount = postCount;
        }
    }

    @Override
    public int getItemViewType(int position) {
        SearchItem item = searchItems.get(position);
        if (TYPE_HASHTAG == item.type.hashCode()) return TYPE_HASHTAG;
        switch (item.type) {
            case SearchItem.TYPE_USER: return TYPE_USER;
            case SearchItem.TYPE_POST: return TYPE_POST;
            case SearchItem.TYPE_HASHTAG: return TYPE_HASHTAG;
            default: return TYPE_USER;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case TYPE_USER:
                ItemSearchUserBinding userBinding = ItemSearchUserBinding.inflate(inflater, parent, false);
                return new UserSearchViewHolder(userBinding);
            case TYPE_POST:
                ItemSearchPostBinding postBinding = ItemSearchPostBinding.inflate(inflater, parent, false);
                return new PostSearchViewHolder(postBinding);
            case TYPE_HASHTAG:
                ItemSearchHashtagBinding hashtagBinding = ItemSearchHashtagBinding.inflate(inflater, parent, false);
                return new HashtagSearchViewHolder(hashtagBinding);
            default:
                ItemSearchUserBinding defBinding = ItemSearchUserBinding.inflate(inflater, parent, false);
                return new UserSearchViewHolder(defBinding);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        SearchItem item = searchItems.get(position);
        switch (holder.getItemViewType()) {
            case TYPE_USER:
                ((UserSearchViewHolder) holder).bind((User) item.data);
                break;
            case TYPE_POST:
                ((PostSearchViewHolder) holder).bind((Post) item.data);
                break;
            case TYPE_HASHTAG:
                ((HashtagSearchViewHolder) holder).bind(item);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return searchItems.size();
    }

    // ============================================================
    // User ViewHolder
    // ============================================================

    class UserSearchViewHolder extends RecyclerView.ViewHolder {
        private final ItemSearchUserBinding binding;

        UserSearchViewHolder(ItemSearchUserBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(User user) {
            Glide.with(context)
                    .load(user.getPhotoUrl())
                    .apply(RequestOptions.circleCropTransform()
                            .placeholder(R.drawable.ic_default_avatar)
                            .error(R.drawable.ic_default_avatar))
                    .into(binding.ivUserAvatar);

            binding.ivVerifiedBadge.setVisibility(user.isVerified() ? View.VISIBLE : View.GONE);
            binding.tvUserName.setText(user.getName());
            binding.tvUserBio.setText(StringUtils.truncateText(user.getBio(), 60));

            itemView.setOnClickListener(v -> {
                context.startActivity(new Intent(context, ProfileActivity.class)
                        .putExtra(Constants.EXTRA_USER_ID, user.getUid()));
            });
        }
    }

    // ============================================================
    // Post ViewHolder (Simplified Post Card)
    // ============================================================

    class PostSearchViewHolder extends RecyclerView.ViewHolder {
        private final ItemSearchPostBinding binding;

        PostSearchViewHolder(ItemSearchPostBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Post post) {
            binding.tvUserName.setText(post.getUserName());
            binding.tvPostTime.setText(DateUtils.formatRelativeTimeArabic(post.getTimestamp()));
            binding.tvPostText.setText(StringUtils.truncateText(post.getText(), 100));

            if (post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
                binding.ivPostImage.setVisibility(View.VISIBLE);
                Glide.with(context)
                        .load(post.getImageUrl())
                        .placeholder(R.drawable.ic_image_placeholder)
                        .into(binding.ivPostImage);
            } else {
                binding.ivPostImage.setVisibility(View.GONE);
            }

            binding.tvLikeCount.setText(formatCount(post.getLikesCount()));
            binding.tvCommentCount.setText(formatCount(post.getCommentsCount()));

            itemView.setOnClickListener(v -> {
                context.startActivity(new Intent(context, PostDetailsActivity.class)
                        .putExtra(Constants.EXTRA_POST_ID, post.getPostId()));
            });
        }

        private String formatCount(long count) {
            if (count <= 0) return "0";
            if (count >= 1_000_000) return String.format("%.1fM", count / 1_000_000.0);
            if (count >= 1_000) return String.format("%.1fK", count / 1_000.0);
            return String.valueOf(count);
        }
    }

    // ============================================================
    // Hashtag ViewHolder
    // ============================================================

    class HashtagSearchViewHolder extends RecyclerView.ViewHolder {
        private final ItemSearchHashtagBinding binding;

        HashtagSearchViewHolder(ItemSearchHashtagBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(SearchItem item) {
            binding.tvHashtagName.setText("#" + item.hashtagName);
            binding.tvPostCount.setText(context.getString(R.string.posts_count, item.postCount));

            itemView.setOnClickListener(v -> {
                context.startActivity(new Intent(context, HashtagActivity.class)
                        .putExtra("hashtag", item.hashtagName));
            });
        }
    }

    // ============================================================
    // Data Operations
    // ============================================================

    public void addUsers(List<User> users) {
        for (User user : users) {
            searchItems.add(new SearchItem(SearchItem.TYPE_USER, user));
        }
        notifyDataSetChanged();
    }

    public void addPosts(List<Post> posts) {
        for (Post post : posts) {
            searchItems.add(new SearchItem(SearchItem.TYPE_POST, post));
        }
        notifyDataSetChanged();
    }

    public void addHashtags(Map<String, Long> hashtags) {
        for (Map.Entry<String, Long> entry : hashtags.entrySet()) {
            searchItems.add(new SearchItem(SearchItem.TYPE_HASHTAG, entry.getKey(), entry.getValue()));
        }
        notifyDataSetChanged();
    }

    public void setSearchItems(List<SearchItem> items) {
        searchItems.clear();
        searchItems.addAll(items);
        notifyDataSetChanged();
    }

    public void clear() {
        searchItems.clear();
        notifyDataSetChanged();
    }
}