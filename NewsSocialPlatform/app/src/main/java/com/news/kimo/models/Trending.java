package com.news.kimo.models;

import java.io.Serializable;

public class Trending implements Serializable {

    private static final long serialVersionUID = 1L;

    private String trendingId;
    private String type;
    private String itemId;
    private String title;
    private String subtitle;
    private String imageUrl;
    private long count;
    private long timestamp;

    public Trending() {
    }

    public Trending(String trendingId, String type, String itemId, String title,
                     String subtitle, String imageUrl, long count, long timestamp) {
        this.trendingId = trendingId;
        this.type = type;
        this.itemId = itemId;
        this.title = title;
        this.subtitle = subtitle;
        this.imageUrl = imageUrl;
        this.count = count;
        this.timestamp = timestamp;
    }

    public String getTrendingId() { return trendingId; }
    public void setTrendingId(String trendingId) { this.trendingId = trendingId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public long getCount() { return count; }
    public void setCount(long count) { this.count = count; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}