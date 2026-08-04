package com.news.kimo.models;

import java.io.Serializable;

public class Report implements Serializable {

    private static final long serialVersionUID = 1L;

    private String reportId;
    private String type;
    private String reportedId;
    private String reporterUid;
    private String reporterName;
    private String reason;
    private String description;
    private long timestamp;
    private String status;
    private String reviewedBy;
    private long reviewedAt;

    public Report() {
    }

    public Report(String reportId, String type, String reportedId, String reporterUid,
                   String reporterName, String reason, String description, long timestamp,
                   String status, String reviewedBy, long reviewedAt) {
        this.reportId = reportId;
        this.type = type;
        this.reportedId = reportedId;
        this.reporterUid = reporterUid;
        this.reporterName = reporterName;
        this.reason = reason;
        this.description = description;
        this.timestamp = timestamp;
        this.status = status;
        this.reviewedBy = reviewedBy;
        this.reviewedAt = reviewedAt;
    }

    public String getReportId() { return reportId; }
    public void setReportId(String reportId) { this.reportId = reportId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getReportedId() { return reportedId; }
    public void setReportedId(String reportedId) { this.reportedId = reportedId; }

    public String getReporterUid() { return reporterUid; }
    public void setReporterUid(String reporterUid) { this.reporterUid = reporterUid; }

    public String getReporterName() { return reporterName; }
    public void setReporterName(String reporterName) { this.reporterName = reporterName; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }

    public long getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(long reviewedAt) { this.reviewedAt = reviewedAt; }
}