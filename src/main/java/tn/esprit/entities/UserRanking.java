package tn.esprit.entities;

public class UserRanking {
    private int userId;
    private String username;
    private int score;
    private int posts;
    private int likes;
    private String title;

    public UserRanking(int userId, String username, int score, int posts, int likes, String title) {
        this.userId = userId;
        this.username = username;
        this.score = score;
        this.posts = posts;
        this.likes = likes;
        this.title = title;
    }

    public int getUserId() { return userId; }
    public String getUsername() { return username; }
    public int getScore() { return score; }
    public int getPosts() { return posts; }
    public int getLikes() { return likes; }
    public String getTitle() { return title; }
}
