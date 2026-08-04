package com.news.kimo.models;

import java.io.Serializable;

public class Comment implements Serializable {

    private static final long serialVersionUID = 1L;

    private String commentId;
    private String postId;
    private String uid;
    private String userName;
    private String userPhoto;
    private String text;
    private String imageUrl;
    private long timestamp;
    private long likesCount;
    private long replyCount;
    private String parentId;

    public Comment() {
    }

    public Comment(String commentId, String postId, String uid, String userName, String userPhoto,
                   String text, String imageUrl, long timestamp, long likesCount, long replyCount,
                   String parentId) {
        this.commentId = commentId;
        this.postId = postId;
        this.uid = uid;
        this.userName = userName;
        this.userPhoto = userPhoto;
        this.text = text;
        this.imageUrl = imageUrl;
        this.timestamp = timestamp;
        this.likesCount = likesCount;
        this.replyCount = replyCount;
        this.parentId = parentId;
    }

    public String getCommentId() { return commentId; }
    public void setCommentId(String commentId) { this.commentId = commentId; }

    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserPhoto() { return userPhoto; }
    public void setUserPhoto(String userPhoto) { this.userPhoto = userPhoto; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public long getLikesCount() { return likesCount; }
    public void setLikesCount(long likesCount) { this.likesCount = likesCount; }

    public long getReplyCount() { return replyCount; }
    public void setReplyCount(long replyCount) { this.replyCount = replyCount; }

    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }
}