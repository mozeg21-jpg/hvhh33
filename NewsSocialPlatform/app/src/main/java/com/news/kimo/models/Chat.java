package com.news.kimo.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Chat implements Serializable {

    private static final long serialVersionUID = 1L;

    private String chatId;
    private String type;
    private String name;
    private String photoUrl;
    private List<String> participants;
    private String lastMessage;
    private long lastMessageTime;
    private String lastMessageSenderName;
    private long unreadCount;
    private String createdBy;
    private long createdAt;
    private boolean isMuted;

    public Chat() {
        this.participants = new ArrayList<>();
    }

    public Chat(String chatId, String type, String name, String photoUrl,
                 List<String> participants, String lastMessage, long lastMessageTime,
                 String lastMessageSenderName, long unreadCount, String createdBy,
                 long createdAt, boolean isMuted) {
        this.chatId = chatId;
        this.type = type;
        this.name = name;
        this.photoUrl = photoUrl;
        this.participants = participants != null ? participants : new ArrayList<String>();
        this.lastMessage = lastMessage;
        this.lastMessageTime = lastMessageTime;
        this.lastMessageSenderName = lastMessageSenderName;
        this.unreadCount = unreadCount;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.isMuted = isMuted;
    }

    public String getChatId() { return chatId; }
    public void setChatId(String chatId) { this.chatId = chatId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public List<String> getParticipants() { return participants; }
    public void setParticipants(List<String> participants) { this.participants = participants; }

    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    public long getLastMessageTime() { return lastMessageTime; }
    public void setLastMessageTime(long lastMessageTime) { this.lastMessageTime = lastMessageTime; }

    public String getLastMessageSenderName() { return lastMessageSenderName; }
    public void setLastMessageSenderName(String lastMessageSenderName) { this.lastMessageSenderName = lastMessageSenderName; }

    public long getUnreadCount() { return unreadCount; }
    public void setUnreadCount(long unreadCount) { this.unreadCount = unreadCount; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public boolean isMuted() { return isMuted; }
    public void setMuted(boolean muted) { isMuted = muted; }
}