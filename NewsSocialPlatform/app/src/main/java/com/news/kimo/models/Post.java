package com.news.kimo.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Post implements Serializable {

    private static final long serialVersionUID = 1L;

    private String postId;
    private String uid;
    private String userName;
    private String userPhoto;
    private String text;
    private String imageUrl;
    private List<String> images;
    private String videoUrl;
    private String fileUrl;
    private String fileName;
    private String linkUrl;
    private List<Map<String, Object>> pollOptions;
    private Map<String, Long> pollVotes;
    private String codeContent;
    private String codeLanguage;
    private String quoteText;
    private String quoteAuthor;
    private boolean isScheduled;
    private long scheduledAt;
    private boolean isPinned;
    private boolean isArchived;
    private long timestamp;
    private long likesCount;
    private long commentsCount;
    private long sharesCount;
    private long viewsCount;
    private Map<String, Long> reactions;
    private List<String> tags;
    private List<String> mentions;
    private String aiSummary;

    public Post() {
        this.images = new ArrayList<>();
        this.pollOptions = new ArrayList<>();
        this.pollVotes = new HashMap<>();
        this.reactions = new HashMap<>();
        this.tags = new ArrayList<>();
        this.mentions = new ArrayList<>();
    }

    public Post(String postId, String uid, String userName, String userPhoto, String text,
                String imageUrl, List<String> images, String videoUrl, String fileUrl,
                String fileName, String linkUrl, List<Map<String, Object>> pollOptions,
                Map<String, Long> pollVotes, String codeContent, String codeLanguage,
                String quoteText, String quoteAuthor, boolean isScheduled, long scheduledAt,
                boolean isPinned, boolean isArchived, long timestamp, long likesCount,
                long commentsCount, long sharesCount, long viewsCount,
                Map<String, Long> reactions, List<String> tags, List<String> mentions,
                String aiSummary) {
        this.postId = postId;
        this.uid = uid;
        this.userName = userName;
        this.userPhoto = userPhoto;
        this.text = text;
        this.imageUrl = imageUrl;
        this.images = images != null ? images : new ArrayList<String>();
        this.videoUrl = videoUrl;
        this.fileUrl = fileUrl;
        this.fileName = fileName;
        this.linkUrl = linkUrl;
        this.pollOptions = pollOptions != null ? pollOptions : new ArrayList<Map<String, Object>>();
        this.pollVotes = pollVotes != null ? pollVotes : new HashMap<String, Long>();
        this.codeContent = codeContent;
        this.codeLanguage = codeLanguage;
        this.quoteText = quoteText;
        this.quoteAuthor = quoteAuthor;
        this.isScheduled = isScheduled;
        this.scheduledAt = scheduledAt;
        this.isPinned = isPinned;
        this.isArchived = isArchived;
        this.timestamp = timestamp;
        this.likesCount = likesCount;
        this.commentsCount = commentsCount;
        this.sharesCount = sharesCount;
        this.viewsCount = viewsCount;
        this.reactions = reactions != null ? reactions : new HashMap<String, Long>();
        this.tags = tags != null ? tags : new ArrayList<String>();
        this.mentions = mentions != null ? mentions : new ArrayList<String>();
        this.aiSummary = aiSummary;
    }

    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserPhoto() { return userPhoto; }
    public void setUserPhoto(String userPhoto) { this.userPhoto = userPhoto; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }

    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getLinkUrl() { return linkUrl; }
    public void setLinkUrl(String linkUrl) { this.linkUrl = linkUrl; }

    public List<Map<String, Object>> getPollOptions() { return pollOptions; }
    public void setPollOptions(List<Map<String, Object>> pollOptions) { this.pollOptions = pollOptions; }

    public Map<String, Long> getPollVotes() { return pollVotes; }
    public void setPollVotes(Map<String, Long> pollVotes) { this.pollVotes = pollVotes; }

    public String getCodeContent() { return codeContent; }
    public void setCodeContent(String codeContent) { this.codeContent = codeContent; }

    public String getCodeLanguage() { return codeLanguage; }
    public void setCodeLanguage(String codeLanguage) { this.codeLanguage = codeLanguage; }

    public String getQuoteText() { return quoteText; }
    public void setQuoteText(String quoteText) { this.quoteText = quoteText; }

    public String getQuoteAuthor() { return quoteAuthor; }
    public void setQuoteAuthor(String quoteAuthor) { this.quoteAuthor = quoteAuthor; }

    public boolean isScheduled() { return isScheduled; }
    public void setScheduled(boolean scheduled) { isScheduled = scheduled; }

    public long getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(long scheduledAt) { this.scheduledAt = scheduledAt; }

    public boolean isPinned() { return isPinned; }
    public void setPinned(boolean pinned) { isPinned = pinned; }

    public boolean isArchived() { return isArchived; }
    public void setArchived(boolean archived) { isArchived = archived; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public long getLikesCount() { return likesCount; }
    public void setLikesCount(long likesCount) { this.likesCount = likesCount; }

    public long getCommentsCount() { return commentsCount; }
    public void setCommentsCount(long commentsCount) { this.commentsCount = commentsCount; }

    public long getSharesCount() { return sharesCount; }
    public void setSharesCount(long sharesCount) { this.sharesCount = sharesCount; }

    public long getViewsCount() { return viewsCount; }
    public void setViewsCount(long viewsCount) { this.viewsCount = viewsCount; }

    public Map<String, Long> getReactions() { return reactions; }
    public void setReactions(Map<String, Long> reactions) { this.reactions = reactions; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public List<String> getMentions() { return mentions; }
    public void setMentions(List<String> mentions) { this.mentions = mentions; }

    public String getAiSummary() { return aiSummary; }
    public void setAiSummary(String aiSummary) { this.aiSummary = aiSummary; }
}