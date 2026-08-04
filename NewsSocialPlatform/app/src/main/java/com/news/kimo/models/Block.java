package com.news.kimo.models;

import java.io.Serializable;

public class Block implements Serializable {

    private static final long serialVersionUID = 1L;

    private String blockId;
    private String blockerUid;
    private String blockedUid;
    private long timestamp;

    public Block() {
    }

    public Block(String blockId, String blockerUid, String blockedUid, long timestamp) {
        this.blockId = blockId;
        this.blockerUid = blockerUid;
        this.blockedUid = blockedUid;
        this.timestamp = timestamp;
    }

    public String getBlockId() { return blockId; }
    public void setBlockId(String blockId) { this.blockId = blockId; }

    public String getBlockerUid() { return blockerUid; }
    public void setBlockerUid(String blockerUid) { this.blockerUid = blockerUid; }

    public String getBlockedUid() { return blockedUid; }
    public void setBlockedUid(String blockedUid) { this.blockedUid = blockedUid; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}