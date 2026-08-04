package com.news.kimo.models;

import java.io.Serializable;

public class Analytics implements Serializable {

    private static final long serialVersionUID = 1L;

    private String analyticsId;
    private String date;
    private long totalUsers;
    private long activeUsers;
    private long totalPosts;
    private long totalMessages;
    private long totalComments;
    private long totalViews;
    private double avgEngagement;
    private long totalReports;
    private long storageUsed;
    private long networkUsage;

    public Analytics() {
    }

    public Analytics(String analyticsId, String date, long totalUsers, long activeUsers,
                       long totalPosts, long totalMessages, long totalComments, long totalViews,
                       double avgEngagement, long totalReports, long storageUsed,
                       long networkUsage) {
        this.analyticsId = analyticsId;
        this.date = date;
        this.totalUsers = totalUsers;
        this.activeUsers = activeUsers;
        this.totalPosts = totalPosts;
        this.totalMessages = totalMessages;
        this.totalComments = totalComments;
        this.totalViews = totalViews;
        this.avgEngagement = avgEngagement;
        this.totalReports = totalReports;
        this.storageUsed = storageUsed;
        this.networkUsage = networkUsage;
    }

    public String getAnalyticsId() { return analyticsId; }
    public void setAnalyticsId(String analyticsId) { this.analyticsId = analyticsId; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public long getTotalUsers() { return totalUsers; }
    public void setTotalUsers(long totalUsers) { this.totalUsers = totalUsers; }

    public long getActiveUsers() { return activeUsers; }
    public void setActiveUsers(long activeUsers) { this.activeUsers = activeUsers; }

    public long getTotalPosts() { return totalPosts; }
    public void setTotalPosts(long totalPosts) { this.totalPosts = totalPosts; }

    public long getTotalMessages() { return totalMessages; }
    public void setTotalMessages(long totalMessages) { this.totalMessages = totalMessages; }

    public long getTotalComments() { return totalComments; }
    public void setTotalComments(long totalComments) { this.totalComments = totalComments; }

    public long getTotalViews() { return totalViews; }
    public void setTotalViews(long totalViews) { this.totalViews = totalViews; }

    public double getAvgEngagement() { return avgEngagement; }
    public void setAvgEngagement(double avgEngagement) { this.avgEngagement = avgEngagement; }

    public long getTotalReports() { return totalReports; }
    public void setTotalReports(long totalReports) { this.totalReports = totalReports; }

    public long getStorageUsed() { return storageUsed; }
    public void setStorageUsed(long storageUsed) { this.storageUsed = storageUsed; }

    public long getNetworkUsage() { return networkUsage; }
    public void setNetworkUsage(long networkUsage) { this.networkUsage = networkUsage; }
}
