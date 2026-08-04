package com.news.kimo.models;

import java.io.Serializable;

public class Media implements Serializable {

    private static final long serialVersionUID = 1L;

    private String mediaId;
    private String postId;
    private String uid;
    private String type;
    private String url;
    private String thumbnailUrl;
    private long width;
    private long height;
    private long size;
    private String fileName;
    private String mimeType;
    private long timestamp;

    public Media() {
    }

    public Media(String mediaId, String postId, String uid, String type, String url,
                  String thumbnailUrl, long width, long height, long size, String fileName,
                  String mimeType, long timestamp) {
        this.mediaId = mediaId;
        this.postId = postId;
        this.uid = uid;
        this.type = type;
        this.url = url;
        this.thumbnailUrl = thumbnailUrl;
        this.width = width;
        this.height = height;
        this.size = size;
        this.fileName = fileName;
        this.mimeType = mimeType;
        this.timestamp = timestamp;
    }

    public String getMediaId() { return mediaId; }
    public void setMediaId(String mediaId) { this.mediaId = mediaId; }

    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    public long getWidth() { return width; }
    public void setWidth(long width) { this.width = width; }

    public long getHeight() { return height; }
    public void setHeight(long height) { this.height = height; }

    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}