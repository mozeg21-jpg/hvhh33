package com.news.kimo.models;

import java.io.Serializable;

public class Session implements Serializable {

    private static final long serialVersionUID = 1L;

    private String sessionId;
    private String uid;
    private String deviceName;
    private String deviceType;
    private String ip;
    private String fcmToken;
    private long createdAt;
    private long lastActive;

    public Session() {
    }

    public Session(String sessionId, String uid, String deviceName, String deviceType,
                       String ip, String fcmToken, long createdAt, long lastActive) {
        this.sessionId = sessionId;
        this.uid = uid;
        this.deviceName = deviceName;
        this.deviceType = deviceType;
        this.ip = ip;
        this.fcmToken = fcmToken;
        this.createdAt = createdAt;
        this.lastActive = lastActive;
    }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }

    public String getDeviceType() { return deviceType; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }

    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }

    public String getFcmToken() { return fcmToken; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getLastActive() { return lastActive; }
    public void setLastActive(long lastActive) { this.lastActive = lastActive; }
}