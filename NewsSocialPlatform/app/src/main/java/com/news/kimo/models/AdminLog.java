package com.news.kimo.models;

import java.io.Serializable;

public class AdminLog implements Serializable {

    private static final long serialVersionUID = 1L;

    private String logId;
    private String adminUid;
    private String adminName;
    private String action;
    private String targetType;
    private String targetId;
    private String details;
    private long timestamp;

    public AdminLog() {
    }

    public AdminLog(String logId, String adminUid, String adminName, String action,
                       String targetType, String targetId, String details, long timestamp) {
        this.logId = logId;
        this.adminUid = adminUid;
        this.adminName = adminName;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.details = details;
        this.timestamp = timestamp;
    }

    public String getLogId() { return logId; }
    public void setLogId(String logId) { this.logId = logId; }

    public String getAdminUid() { return adminUid; }
    public void setAdminUid(String adminUid) { this.adminUid = adminUid; }

    public String getAdminName() { return adminName; }
    public void setAdminName(String adminName) { this.adminName = adminName; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }

    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}