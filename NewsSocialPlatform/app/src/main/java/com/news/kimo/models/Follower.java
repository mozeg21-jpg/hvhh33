package com.news.kimo.models;

import java.io.Serializable;

public class Follower implements Serializable {

    private static final long serialVersionUID = 1L;

    private String followerUid;
    private String followingUid;
    private long timestamp;
    private String status;

    public Follower() {
    }

    public Follower(String followerUid, String followingUid, long timestamp, String status) {
        this.followerUid = followerUid;
        this.followingUid = followingUid;
        this.timestamp = timestamp;
        this.status = status;
    }

    public String getFollowerUid() { return followerUid; }
    public void setFollowerUid(String followerUid) { this.followerUid = followerUid; }

    public String getFollowingUid() { return followingUid; }
    public void setFollowingUid(String followingUid) { this.followingUid = followingUid; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}