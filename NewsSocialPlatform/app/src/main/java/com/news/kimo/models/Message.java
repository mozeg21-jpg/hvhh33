package com.news.kimo.models;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Message implements Serializable {

    private static final long serialVersionUID = 1L;

    private String messageId;
    private String chatId;
    private String senderId;
    private String senderName;
    private String senderPhoto;
    private String text;
    private String imageUrl;
    private String videoUrl;
    private String fileUrl;
    private String fileName;
    private String audioUrl;
    private long duration;
    private Map<String, Double> location;
    private String gifUrl;
    private String replyToMessageId;
    private String replyToText;
    private long timestamp;
    private boolean isRead;
    private boolean isEdited;
    private boolean isDeleted;

    public Message() {
        this.location = new HashMap<>();
    }

    public Message(String messageId, String chatId, String senderId, String senderName,
                   String senderPhoto, String text, String imageUrl, String videoUrl,
                   String fileUrl, String fileName, String audioUrl, long duration,
                   Map<String, Double> location, String gifUrl, String replyToMessageId,
                   String replyToText, long timestamp, boolean isRead, boolean isEdited,
                   boolean isDeleted) {
        this.messageId = messageId;
        this.chatId = chatId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.senderPhoto = senderPhoto;
        this.text = text;
        this.imageUrl = imageUrl;
        this.videoUrl = videoUrl;
        this.fileUrl = fileUrl;
        this.fileName = fileName;
        this.audioUrl = audioUrl;
        this.duration = duration;
        this.location = location != null ? location : new HashMap<String, Double>();
        this.gifUrl = gifUrl;
        this.replyToMessageId = replyToMessageId;
        this.replyToText = replyToText;
        this.timestamp = timestamp;
        this.isRead = isRead;
        this.isEdited = isEdited;
        this.isDeleted = isDeleted;
    }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getChatId() { return chatId; }
    public void setChatId(String chatId) { this.chatId = chatId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getSenderPhoto() { return senderPhoto; }
    public void setSenderPhoto(String senderPhoto) { this.senderPhoto = senderPhoto; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getAudioUrl() { return audioUrl; }
    public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }

    public long getDuration() { return duration; }
    public void setDuration(long duration) { this.duration = duration; }

    public Map<String, Double> getLocation() { return location; }
    public void setLocation(Map<String, Double> location) { this.location = location; }

    public String getGifUrl() { return gifUrl; }
    public void setGifUrl(String gifUrl) { this.gifUrl = gifUrl; }

    public String getReplyToMessageId() { return replyToMessageId; }
    public void setReplyToMessageId(String replyToMessageId) { this.replyToMessageId = replyToMessageId; }

    public String getReplyToText() { return replyToText; }
    public void setReplyToText(String replyToText) { this.replyToText = replyToText; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public boolean isEdited() { return isEdited; }
    public void setEdited(boolean edited) { isEdited = edited; }

    public boolean isDeleted() { return isDeleted; }
    public void setDeleted(boolean deleted) { isDeleted = deleted; }
}