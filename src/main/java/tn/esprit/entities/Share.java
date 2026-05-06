package tn.esprit.entities;

import java.sql.Timestamp;

public class Share {
    private int id;
    private int originalPostId;
    private int userId;
    private String shareText;
    private Timestamp createdAt;

    public Share() {}

    public Share(int originalPostId, int userId, String shareText) {
        this.originalPostId = originalPostId;
        this.userId = userId;
        this.shareText = shareText;
    }

    public Share(int id, int originalPostId, int userId, String shareText, Timestamp createdAt) {
        this.id = id;
        this.originalPostId = originalPostId;
        this.userId = userId;
        this.shareText = shareText;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getOriginalPostId() { return originalPostId; }
    public void setOriginalPostId(int originalPostId) { this.originalPostId = originalPostId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getShareText() { return shareText; }
    public void setShareText(String shareText) { this.shareText = shareText; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
