package com.news.kimo.models;

import java.io.Serializable;

public class Reply implements Serializable {

    private static final long serialVersionUID = 1L;

    private String replyId;
    private String commentId;
    private String postId;
    private String uid;
    private String userName;
    private String userPhoto;
    private String text;
    private long timestamp;
    private long likesCount;

    public Reply() {
    }

    public Reply(String replyId, String commentId, String postId, String uid, String userName,
                 String userPhoto, String text, long timestamp, long likesCount) {
        this.replyId = replyId;
        this.commentId = commentId;
        this.postId = postId;
        this.uid = uid;
        this.userName = userName;
        this.userPhoto = userPhoto;
        this.text = text;
        this.timestamp = timestamp;
        this.likesCount = likesCount;
    }

    public String getReplyId() { return replyId; }
    public void setReplyId(String replyId) { this.replyId = replyId; }

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

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public long getLikesCount() { return likesCount; }
    public void setLikesCount(long likesCount) { this.likesCount = likesCount; }
}