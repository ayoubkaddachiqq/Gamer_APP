package tn.esprit.entities;

import java.sql.Timestamp;

public class Post {
    private int id;
    private int userId;
    private String content;
    private String gameTag;
    private Timestamp createdAt;



    public Post(){}


    public Post(int id, int userId, String content, String gameTag, Timestamp createdAt) {
        this.id = id;
        this.userId = userId;
        this.content = content;
        this.gameTag = gameTag;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getGameTag() { return gameTag; }
    public void setGameTag(String gameTag) { this.gameTag = gameTag; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "Post{" +
                "id=" + id +
                ", userId=" + userId +
                ", content='" + content + '\'' +
                ", gameTag='" + gameTag + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
