package com.news.kimo.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Group implements Serializable {

    private static final long serialVersionUID = 1L;

    private String groupId;
    private String name;
    private String photoUrl;
    private String description;
    private String createdBy;
    private List<String> admins;
    private List<String> members;
    private long createdAt;
    private long memberCount;

    public Group() {
        this.admins = new ArrayList<>();
        this.members = new ArrayList<>();
    }

    public Group(String groupId, String name, String photoUrl, String description,
                  String createdBy, List<String> admins, List<String> members,
                  long createdAt, long memberCount) {
        this.groupId = groupId;
        this.name = name;
        this.photoUrl = photoUrl;
        this.description = description;
        this.createdBy = createdBy;
        this.admins = admins != null ? admins : new ArrayList<String>();
        this.members = members != null ? members : new ArrayList<String>();
        this.createdAt = createdAt;
        this.memberCount = memberCount;
    }

    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public List<String> getAdmins() { return admins; }
    public void setAdmins(List<String> admins) { this.admins = admins; }

    public List<String> getMembers() { return members; }
    public void setMembers(List<String> members) { this.members = members; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getMemberCount() { return memberCount; }
    public void setMemberCount(long memberCount) { this.memberCount = memberCount; }
}