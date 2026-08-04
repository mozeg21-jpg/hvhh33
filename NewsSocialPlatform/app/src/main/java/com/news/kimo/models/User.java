package com.news.kimo.models;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    private String uid;
    private String name;
    private String email;
    private String photoUrl;
    private String coverUrl;
    private String bio;
    private String country;
    private String city;
    private String birthDate;
    private String gender;
    private String location;
    private Map<String, String> socialLinks;
    private String website;
    private long postCount;
    private long followersCount;
    private long followingCount;
    private long likesCount;
    private long viewsCount;
    private boolean isVerified;
    private boolean isPrivate;
    private boolean isDisabled;
    private boolean isOnline;
    private String role;
    private String fcmToken;
    private long createdAt;
    private long updatedAt;
    private long lastSeen;

    public User() {
        this.socialLinks = new HashMap<>();
    }

    public User(String uid, String name, String email, String photoUrl, String coverUrl,
                String bio, String country, String city, String birthDate, String gender,
                String location, Map<String, String> socialLinks, String website,
                long postCount, long followersCount, long followingCount, long likesCount,
                long viewsCount, boolean isVerified, boolean isPrivate, boolean isDisabled,
                boolean isOnline, String role, String fcmToken, long createdAt,
                long updatedAt, long lastSeen) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.photoUrl = photoUrl;
        this.coverUrl = coverUrl;
        this.bio = bio;
        this.country = country;
        this.city = city;
        this.birthDate = birthDate;
        this.gender = gender;
        this.location = location;
        this.socialLinks = socialLinks != null ? socialLinks : new HashMap<String, String>();
        this.website = website;
        this.postCount = postCount;
        this.followersCount = followersCount;
        this.followingCount = followingCount;
        this.likesCount = likesCount;
        this.viewsCount = viewsCount;
        this.isVerified = isVerified;
        this.isPrivate = isPrivate;
        this.isDisabled = isDisabled;
        this.isOnline = isOnline;
        this.role = role;
        this.fcmToken = fcmToken;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.lastSeen = lastSeen;
    }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getBirthDate() { return birthDate; }
    public void setBirthDate(String birthDate) { this.birthDate = birthDate; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public Map<String, String> getSocialLinks() { return socialLinks; }
    public void setSocialLinks(Map<String, String> socialLinks) { this.socialLinks = socialLinks; }

    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }

    public long getPostCount() { return postCount; }
    public void setPostCount(long postCount) { this.postCount = postCount; }

    public long getFollowersCount() { return followersCount; }
    public void setFollowersCount(long followersCount) { this.followersCount = followersCount; }

    public long getFollowingCount() { return followingCount; }
    public void setFollowingCount(long followingCount) { this.followingCount = followingCount; }

    public long getLikesCount() { return likesCount; }
    public void setLikesCount(long likesCount) { this.likesCount = likesCount; }

    public long getViewsCount() { return viewsCount; }
    public void setViewsCount(long viewsCount) { this.viewsCount = viewsCount; }

    public boolean isVerified() { return isVerified; }
    public void setVerified(boolean verified) { isVerified = verified; }

    public boolean isPrivate() { return isPrivate; }
    public void setPrivate(boolean aPrivate) { isPrivate = aPrivate; }

    public boolean isDisabled() { return isDisabled; }
    public void setDisabled(boolean disabled) { isDisabled = disabled; }

    public boolean isOnline() { return isOnline; }
    public void setOnline(boolean online) { isOnline = online; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getFcmToken() { return fcmToken; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public long getLastSeen() { return lastSeen; }
    public void setLastSeen(long lastSeen) { this.lastSeen = lastSeen; }
}