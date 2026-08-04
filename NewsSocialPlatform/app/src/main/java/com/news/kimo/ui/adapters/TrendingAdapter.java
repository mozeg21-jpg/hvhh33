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
import com.news.kimo.databinding.ItemTrendingBinding;
import com.news.kimo.models.Trending;
import com.news.kimo.ui.activities.HashtagActivity;
import com.news.kimo.ui.activities.PostDetailsActivity;
import com.news.kimo.ui.activities.ProfileActivity;
import com.news.kimo.utils.Constants;
import com.news.kimo.utils.DateUtils;

import java.util.ArrayList;
import java.util.List;

public class TrendingAdapter extends RecyclerView.Adapter<TrendingAdapter.TrendingViewHolder> {

    private final Context context;
    private final List<Trending> trendingList;

    public TrendingAdapter(Context context, List<Trending> trendingList) {
        this.context = context;
        this.trendingList = trendingList != null ? trendingList : new ArrayList<>();
    }

    @NonNull
    @Override
    public TrendingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemTrendingBinding binding = ItemTrendingBinding.inflate(inflater, parent, false);
        return new TrendingViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull TrendingViewHolder holder, int position) {
        holder.bind(trendingList.get(position), position);
    }

    @Override
    public int getItemCount() {
        return trendingList.size();
    }

    @Override
    public long getItemId(int position) {
        if (position < trendingList.size() && trendingList.get(position).getTrendingId() != null) {
            return trendingList.get(position).getTrendingId().hashCode();
        }
        return RecyclerView.NO_ID;
    }

    // ---- ViewHolder ----

    class TrendingViewHolder extends RecyclerView.ViewHolder {
        private final ItemTrendingBinding binding;

        TrendingViewHolder(ItemTrendingBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Trending trending, int position) {
            // Rank number (circle)
            binding.tvRankNumber.setText(String.valueOf(position + 1));

            // Thumbnail
            if (trending.getImageUrl() != null && !trending.getImageUrl().isEmpty()) {
                Glide.with(context)
                        .load(trending.getImageUrl())
                        .apply(new RequestOptions()
                                .placeholder(R.drawable.ic_image_placeholder)
                                .error(R.drawable.ic_image_placeholder))
                        .into(binding.ivThumbnail);
            } else {
                binding.ivThumbnail.setImageResource(R.drawable.ic_trending_placeholder);
            }

            // Title
            binding.tvTitle.setText(trending.getTitle());

            // Subtitle
            if (trending.getSubtitle() != null && !trending.getSubtitle().isEmpty()) {
                binding.tvSubtitle.setVisibility(View.VISIBLE);
                binding.tvSubtitle.setText(trending.getSubtitle());
            } else {
                binding.tvSubtitle.setVisibility(View.GONE);
            }

            // Count
            String countStr = formatCount(trending.getCount());
            binding.tvCount.setText(countStr);

            // Time
            binding.tvTime.setText(DateUtils.formatRelativeTimeArabic(trending.getTimestamp()));

            // Click -> navigate based on type
            itemView.setOnClickListener(v -> navigateToTarget(trending));
        }

        private String formatCount(long count) {
            if (count <= 0) return "0";
            if (count >= 1_000_000) return String.format("%.1fM", count / 1_000_000.0);
            if (count >= 1_000) return String.format("%.1fK", count / 1_000.0);
            return String.valueOf(count);
        }

        private void navigateToTarget(Trending trending) {
            String type = trending.getType();
            if (type == null) return;

            Intent intent = null;
            switch (type) {
                case "post":
                    if (trending.getItemId() != null) {
                        intent = new Intent(context, PostDetailsActivity.class)
                                .putExtra(Constants.EXTRA_POST_ID, trending.getItemId());
                    }
                    break;
                case "user":
                    if (trending.getItemId() != null) {
                        intent = new Intent(context, ProfileActivity.class)
                                .putExtra(Constants.EXTRA_USER_ID, trending.getItemId());
                    }
                    break;
                case "hashtag":
                    intent = new Intent(context, HashtagActivity.class)
                            .putExtra("hashtag", trending.getTitle());
                    break;
                default:
                    break;
            }
            if (intent != null) {
                context.startActivity(intent);
            }
        }
    }

    // ---- Data Operations ----

    public void addTrending(Trending trending) {
        trendingList.add(trending);
        notifyItemInserted(trendingList.size() - 1);
    }

    public void addTrendingList(List<Trending> newList) {
        int startPos = trendingList.size();
        trendingList.addAll(newList);
        notifyItemRangeInserted(startPos, newList.size());
    }

    public void setTrendingList(List<Trending> newList) {
        trendingList.clear();
        trendingList.addAll(newList);
        notifyDataSetChanged();
    }

    public Trending getTrending(int position) {
        if (position >= 0 && position < trendingList.size()) {
            return trendingList.get(position);
        }
        return null;
    }

    public List<Trending> getTrendingList() {
        return trendingList;
    }
}