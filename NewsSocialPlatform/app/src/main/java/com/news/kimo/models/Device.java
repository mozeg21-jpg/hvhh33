package com.news.kimo.models;

import java.io.Serializable;

public class Device implements Serializable {

    private static final long serialVersionUID = 1L;

    private String deviceId;
    private String uid;
    private String model;
    private String brand;
    private String osVersion;
    private String appVersion;
    private String fcmToken;
    private long createdAt;

    public Device() {
    }

    public Device(String deviceId, String uid, String model, String brand,
                     String osVersion, String appVersion, String fcmToken, long createdAt) {
        this.deviceId = deviceId;
        this.uid = uid;
        this.model = model;
        this.brand = brand;
        this.osVersion = osVersion;
        this.appVersion = appVersion;
        this.fcmToken = fcmToken;
        this.createdAt = createdAt;
    }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getOsVersion() { return osVersion; }
    public void setOsVersion(String osVersion) { this.osVersion = osVersion; }

    public String getAppVersion() { return appVersion; }
    public void setAppVersion(String appVersion) { this.appVersion = appVersion; }

    public String getFcmToken() { return fcmToken; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}