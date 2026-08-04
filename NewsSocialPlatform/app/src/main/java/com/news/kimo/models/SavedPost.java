package com.news.kimo.models;

import java.io.Serializable;

public class SavedPost implements Serializable {

    private static final long serialVersionUID = 1L;

    private String savedId;
    private String postId;
    private String uid;
    private String listName;
    private long timestamp;

    public SavedPost() {
    }

    public SavedPost(String savedId, String postId, String uid, String listName, long timestamp) {
        this.savedId = savedId;
        this.postId = postId;
        this.uid = uid;
        this.listName = listName;
        this.timestamp = timestamp;
    }

    public String getSavedId() { return savedId; }
    public void setSavedId(String savedId) { this.savedId = savedId; }

    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getListName() { return listName; }
    public void setListName(String listName) { this.listName = listName; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}