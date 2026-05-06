package tn.esprit.entities;

import java.sql.Timestamp;

public class ImagePost {
    private int id;
    private int postId;
    private String imagePath;
    private Timestamp createdAt;

    public ImagePost() {}

    public ImagePost(int postId, String imagePath) {
        this.postId = postId;
        this.imagePath = imagePath;
    }

    public ImagePost(int id, int postId, String imagePath, Timestamp createdAt) {
        this.id = id;
        this.postId = postId;
        this.imagePath = imagePath;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getPostId() { return postId; }
    public void setPostId(int postId) { this.postId = postId; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
