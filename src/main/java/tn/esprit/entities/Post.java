package tn.esprit.entities;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class Post {
    private int id;
    private int userId;
    private String username;
    private String content;
    private String gameTag;
    private Timestamp createdAt;
    private List<String> imagePaths;



    public Post(){}

    public Post(int userId, String content, String gameTag) {
        this.userId = userId;
        this.content = content;
        this.gameTag = gameTag;
        this.imagePaths = new ArrayList<>();
    }

    public Post(int userId, String username, String content, String gameTag) {
        this.userId = userId;
        this.username = username;
        this.content = content;
        this.gameTag = gameTag;
        this.imagePaths = new ArrayList<>();
    }


    public Post(int id, int userId, String content, String gameTag, Timestamp createdAt) {
        this.id = id;
        this.userId = userId;
        this.content = content;
        this.gameTag = gameTag;
        this.createdAt = createdAt;
        this.imagePaths = new ArrayList<>();
    }

    public Post(int id, int userId, String username, String content, String gameTag, Timestamp createdAt) {
        this.id = id;
        this.userId = userId;
        this.username = username;
        this.content = content;
        this.gameTag = gameTag;
        this.createdAt = createdAt;
        this.imagePaths = new ArrayList<>();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getGameTag() { return gameTag; }
    public void setGameTag(String gameTag) { this.gameTag = gameTag; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public List<String> getImagePaths() { return imagePaths; }
    public void setImagePaths(List<String> imagePaths) { this.imagePaths = imagePaths; }
    public void addImagePath(String path) { this.imagePaths.add(path); }

    @Override
    public String toString() {
        return "Post{" +
                "id=" + id +
                ", userId=" + userId +
                ", username='" + username + '\'' +
                ", content='" + content + '\'' +
                ", gameTag='" + gameTag + '\'' +
                ", imagePaths=" + imagePaths +
                ", createdAt=" + createdAt +
                '}';
    }
}
