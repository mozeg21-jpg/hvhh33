package com.news.kimo.models;

import java.io.Serializable;

public class Reaction implements Serializable {

    private static final long serialVersionUID = 1L;

    private String postId;
    private String uid;
    private String type;
    private long timestamp;

    public Reaction() {
    }

    public Reaction(String postId, String uid, String type, long timestamp) {
        this.postId = postId;
        this.uid = uid;
        this.type = type;
        this.timestamp = timestamp;
    }

    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}