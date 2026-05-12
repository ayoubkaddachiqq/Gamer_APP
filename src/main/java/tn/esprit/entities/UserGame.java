package tn.esprit.entities;

import java.time.Instant;

public class UserGame {
    private int id;
    private int userId;
    private int rawgId;
    private String gameName;
    private String coverUrl;
    private String genre;
    private int metacriticScore;
    private boolean isFavorite;
    private Instant addedAt;

    public UserGame() {}

    public UserGame(int rawgId, String gameName, String coverUrl, String genre, int metacriticScore) {
        this.rawgId = rawgId;
        this.gameName = gameName;
        this.coverUrl = coverUrl;
        this.genre = genre;
        this.metacriticScore = metacriticScore;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getRawgId() { return rawgId; }
    public void setRawgId(int rawgId) { this.rawgId = rawgId; }

    public String getGameName() { return gameName; }
    public void setGameName(String gameName) { this.gameName = gameName; }

    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

    public int getMetacriticScore() { return metacriticScore; }
    public void setMetacriticScore(int metacriticScore) { this.metacriticScore = metacriticScore; }

    public boolean isFavorite() { return isFavorite; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }

    public Instant getAddedAt() { return addedAt; }
    public void setAddedAt(Instant addedAt) { this.addedAt = addedAt; }
}
