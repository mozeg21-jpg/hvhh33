package com.news.kimo.models;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class AppConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    private String configId;
    private String appVersion;
    private String minVersion;
    private boolean maintenanceMode;
    private String maintenanceMessage;
    private Map<String, Boolean> features;
    private boolean aiEnabled;
    private long maxPostLength;
    private long maxImageCount;
    private long maxVideoSize;
    private long maxFileSize;

    public AppConfig() {
        this.features = new HashMap<>();
    }

    public AppConfig(String configId, String appVersion, String minVersion,
                       boolean maintenanceMode, String maintenanceMessage,
                       Map<String, Boolean> features, boolean aiEnabled,
                       long maxPostLength, long maxImageCount, long maxVideoSize,
                       long maxFileSize) {
        this.configId = configId;
        this.appVersion = appVersion;
        this.minVersion = minVersion;
        this.maintenanceMode = maintenanceMode;
        this.maintenanceMessage = maintenanceMessage;
        this.features = features != null ? features : new HashMap<String, Boolean>();
        this.aiEnabled = aiEnabled;
        this.maxPostLength = maxPostLength;
        this.maxImageCount = maxImageCount;
        this.maxVideoSize = maxVideoSize;
        this.maxFileSize = maxFileSize;
    }

    public String getConfigId() { return configId; }
    public void setConfigId(String configId) { this.configId = configId; }

    public String getAppVersion() { return appVersion; }
    public void setAppVersion(String appVersion) { this.appVersion = appVersion; }

    public String getMinVersion() { return minVersion; }
    public void setMinVersion(String minVersion) { this.minVersion = minVersion; }

    public boolean isMaintenanceMode() { return maintenanceMode; }
    public void setMaintenanceMode(boolean maintenanceMode) { this.maintenanceMode = maintenanceMode; }

    public String getMaintenanceMessage() { return maintenanceMessage; }
    public void setMaintenanceMessage(String maintenanceMessage) { this.maintenanceMessage = maintenanceMessage; }

    public Map<String, Boolean> getFeatures() { return features; }
    public void setFeatures(Map<String, Boolean> features) { this.features = features; }

    public boolean isAiEnabled() { return aiEnabled; }
    public void setAiEnabled(boolean aiEnabled) { this.aiEnabled = aiEnabled; }

    public long getMaxPostLength() { return maxPostLength; }
    public void setMaxPostLength(long maxPostLength) { this.maxPostLength = maxPostLength; }

    public long getMaxImageCount() { return maxImageCount; }
    public void setMaxImageCount(long maxImageCount) { this.maxImageCount = maxImageCount; }

    public long getMaxVideoSize() { return maxVideoSize; }
    public void setMaxVideoSize(long maxVideoSize) { this.maxVideoSize = maxVideoSize; }

    public long getMaxFileSize() { return maxFileSize; }
    public void setMaxFileSize(long maxFileSize) { this.maxFileSize = maxFileSize; }
}
