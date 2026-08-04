package com.news.kimo.models;

import java.io.Serializable;

public class Mute implements Serializable {

    private static final long serialVersionUID = 1L;

    private String muteId;
    private String muterUid;
    private String mutedUid;
    private String chatId;
    private long timestamp;

    public Mute() {
    }

    public Mute(String muteId, String muterUid, String mutedUid, String chatId, long timestamp) {
        this.muteId = muteId;
        this.muterUid = muterUid;
        this.mutedUid = mutedUid;
        this.chatId = chatId;
        this.timestamp = timestamp;
    }

    public String getMuteId() { return muteId; }
    public void setMuteId(String muteId) { this.muteId = muteId; }

    public String getMuterUid() { return muterUid; }
    public void setMuterUid(String muterUid) { this.muterUid = muterUid; }

    public String getMutedUid() { return mutedUid; }
    public void setMutedUid(String mutedUid) { this.mutedUid = mutedUid; }

    public String getChatId() { return chatId; }
    public void setChatId(String chatId) { this.chatId = chatId; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}