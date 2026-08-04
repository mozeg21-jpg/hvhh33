package com.news.kimo.models;

import java.io.Serializable;

public class Verification implements Serializable {

    private static final long serialVersionUID = 1L;

    private String verificationId;
    private String uid;
    private String idType;
    private String idImageUrl;
    private String status;
    private long submittedAt;
    private String reviewedBy;
    private long reviewedAt;
    private String rejectionReason;

    public Verification() {
    }

    public Verification(String verificationId, String uid, String idType, String idImageUrl,
                           String status, long submittedAt, String reviewedBy, long reviewedAt,
                           String rejectionReason) {
        this.verificationId = verificationId;
        this.uid = uid;
        this.idType = idType;
        this.idImageUrl = idImageUrl;
        this.status = status;
        this.submittedAt = submittedAt;
        this.reviewedBy = reviewedBy;
        this.reviewedAt = reviewedAt;
        this.rejectionReason = rejectionReason;
    }

    public String getVerificationId() { return verificationId; }
    public void setVerificationId(String verificationId) { this.verificationId = verificationId; }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getIdType() { return idType; }
    public void setIdType(String idType) { this.idType = idType; }

    public String getIdImageUrl() { return idImageUrl; }
    public void setIdImageUrl(String idImageUrl) { this.idImageUrl = idImageUrl; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(long submittedAt) { this.submittedAt = submittedAt; }

    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }

    public long getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(long reviewedAt) { this.reviewedAt = reviewedAt; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
}