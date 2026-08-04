package com.news.kimo.models;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Setting implements Serializable {

    private static final long serialVersionUID = 1L;

    private String settingId;
    private String uid;
    private String theme;
    private String language;
    private String fontSize;
    private boolean isPrivateProfile;
    private boolean isOnlineVisible;
    private boolean isReadReceiptEnabled;
    private Map<String, Boolean> notificationsEnabled;
    private String privacyLevel;

    public Setting() {
        this.notificationsEnabled = new HashMap<>();
    }

    public Setting(String settingId, String uid, String theme, String language,
                    String fontSize, boolean isPrivateProfile, boolean isOnlineVisible,
                    boolean isReadReceiptEnabled, Map<String, Boolean> notificationsEnabled,
                    String privacyLevel) {
        this.settingId = settingId;
        this.uid = uid;
        this.theme = theme;
        this.language = language;
        this.fontSize = fontSize;
        this.isPrivateProfile = isPrivateProfile;
        this.isOnlineVisible = isOnlineVisible;
        this.isReadReceiptEnabled = isReadReceiptEnabled;
        this.notificationsEnabled = notificationsEnabled != null ? notificationsEnabled : new HashMap<String, Boolean>();
        this.privacyLevel = privacyLevel;
    }

    public String getSettingId() { return settingId; }
    public void setSettingId(String settingId) { this.settingId = settingId; }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getFontSize() { return fontSize; }
    public void setFontSize(String fontSize) { this.fontSize = fontSize; }

    public boolean isPrivateProfile() { return isPrivateProfile; }
    public void setPrivateProfile(boolean privateProfile) { isPrivateProfile = privateProfile; }

    public boolean isOnlineVisible() { return isOnlineVisible; }
    public void setOnlineVisible(boolean onlineVisible) { isOnlineVisible = onlineVisible; }

    public boolean isReadReceiptEnabled() { return isReadReceiptEnabled; }
    public void setReadReceiptEnabled(boolean readReceiptEnabled) { isReadReceiptEnabled = readReceiptEnabled; }

    public Map<String, Boolean> getNotificationsEnabled() { return notificationsEnabled; }
    public void setNotificationsEnabled(Map<String, Boolean> notificationsEnabled) { this.notificationsEnabled = notificationsEnabled; }

    public String getPrivacyLevel() { return privacyLevel; }
    public void setPrivacyLevel(String privacyLevel) { this.privacyLevel = privacyLevel; }
}