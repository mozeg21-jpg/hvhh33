package com.news.kimo.models;

import java.io.Serializable;

public class NotificationItem implements Serializable {

    private static final long serialVersionUID = 1L;

    private String notificationId;
    private String type;
    private String fromUid;
    private String fromName;
    private String fromPhoto;
    private String toUid;
    private String postId;
    private String commentId;
    private String messageId;
    private String title;
    private String body;
    private long timestamp;
    private boolean isRead;

    public NotificationItem() {
    }

    public NotificationItem(String notificationId, String type, String fromUid, String fromName,
                            String fromPhoto, String toUid, String postId, String commentId,
                            String messageId, String title, String body, long timestamp,
                            boolean isRead) {
        this.notificationId = notificationId;
        this.type = type;
        this.fromUid = fromUid;
        this.fromName = fromName;
        this.fromPhoto = fromPhoto;
        this.toUid = toUid;
        this.postId = postId;
        this.commentId = commentId;
        this.messageId = messageId;
        this.title = title;
        this.body = body;
        this.timestamp = timestamp;
        this.isRead = isRead;
    }

    public String getNotificationId() { return notificationId; }
    public void setNotificationId(String notificationId) { this.notificationId = notificationId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getFromUid() { return fromUid; }
    public void setFromUid(String fromUid) { this.fromUid = fromUid; }

    public String getFromName() { return fromName; }
    public void setFromName(String fromName) { this.fromName = fromName; }

    public String getFromPhoto() { return fromPhoto; }
    public void setFromPhoto(String fromPhoto) { this.fromPhoto = fromPhoto; }

    public String getToUid() { return toUid; }
    public void setToUid(String toUid) { this.toUid = toUid; }

    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }

    public String getCommentId() { return commentId; }
    public void setCommentId(String commentId) { this.commentId = commentId; }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
}