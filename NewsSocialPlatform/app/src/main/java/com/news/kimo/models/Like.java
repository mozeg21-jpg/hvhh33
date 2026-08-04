package com.news.kimo.models;

import java.io.Serializable;

public class Like implements Serializable {

    private static final long serialVersionUID = 1L;

    private String postId;
    private String uid;
    private long timestamp;
    private String type;

    public Like() {
    }

    public Like(String postId, String uid, long timestamp, String type) {
        this.postId = postId;
        this.uid = uid;
        this.timestamp = timestamp;
        this.type = type;
    }

    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}