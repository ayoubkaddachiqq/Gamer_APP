package tn.esprit.demo.model;

import java.time.Instant;

public class UserGame {
    private int    id;
    private long   userId;
    private int    rawgId;
    private String gameName;
    private String coverUrl;
    private String genre;
    private int    metacriticScore;
    private boolean isFavorite;
    private Instant addedAt;

    public UserGame() {}

    public UserGame(int rawgId, String gameName, String coverUrl,
                    String genre, int metacriticScore) {
        this.rawgId          = rawgId;
        this.gameName        = gameName;
        this.coverUrl        = coverUrl;
        this.genre           = genre;
        this.metacriticScore = metacriticScore;
    }

    public int     getId()               { return id; }
    public void    setId(int id)         { this.id = id; }

    public long    getUserId()           { return userId; }
    public void    setUserId(long userId){ this.userId = userId; }

    public int     getRawgId()           { return rawgId; }
    public void    setRawgId(int rawgId) { this.rawgId = rawgId; }

    public String  getGameName()         { return gameName; }
    public void    setGameName(String n) { this.gameName = n; }

    public String  getCoverUrl()         { return coverUrl; }
    public void    setCoverUrl(String u) { this.coverUrl = u; }

    public String  getGenre()            { return genre; }
    public void    setGenre(String g)    { this.genre = g; }

    public int     getMetacriticScore()          { return metacriticScore; }
    public void    setMetacriticScore(int score) { this.metacriticScore = score; }

    public boolean isFavorite()              { return isFavorite; }
    public void    setFavorite(boolean fav)  { this.isFavorite = fav; }

    public Instant getAddedAt()              { return addedAt; }
    public void    setAddedAt(Instant t)     { this.addedAt = t; }
}
