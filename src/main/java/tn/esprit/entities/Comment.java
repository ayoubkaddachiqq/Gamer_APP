package tn.esprit.entities;

import java.sql.Timestamp;

public class Comment {
    private int id;
    private int postId;   // Liaison vers le Post
    private int userId;   // Liaison vers l'auteur du commentaire
    private String commentText;
    private Timestamp createdAt;

    // Constructeur pour créer un NOUVEAU commentaire (sans ID)
    public Comment(int postId, int userId, String commentText) {
        this.postId = postId;
        this.userId = userId;
        this.commentText = commentText;
    }

    // Constructeur complet (pour charger depuis la base de données)
    public Comment(int id, int postId, int userId, String commentText, Timestamp createdAt) {
        this.id = id;
        this.postId = postId;
        this.userId = userId;
        this.commentText = commentText;
        this.createdAt = createdAt;
    }

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getPostId() { return postId; }
    public void setPostId(int postId) { this.postId = postId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getCommentText() { return commentText; }
    public void setCommentText(String commentText) { this.commentText = commentText; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    // toString pour faciliter tes tests CRUD dans la console
    @Override
    public String toString() {
        return "Comment{" +
                "id=" + id +
                ", postId=" + postId +
                ", userId=" + userId +
                ", content='" + commentText + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
