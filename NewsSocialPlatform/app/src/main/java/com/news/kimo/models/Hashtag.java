package com.news.kimo.models;

import java.io.Serializable;

public class Hashtag implements Serializable {

    private static final long serialVersionUID = 1L;

    private String tagId;
    private String name;
    private long postCount;
    private long createdAt;

    public Hashtag() {
    }

    public Hashtag(String tagId, String name, long postCount, long createdAt) {
        this.tagId = tagId;
        this.name = name;
        this.postCount = postCount;
        this.createdAt = createdAt;
    }

    public String getTagId() { return tagId; }
    public void setTagId(String tagId) { this.tagId = tagId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public long getPostCount() { return postCount; }
    public void setPostCount(long postCount) { this.postCount = postCount; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}