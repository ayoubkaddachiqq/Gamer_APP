package tn.esprit.entities;

import java.time.Instant;

public class RiotAccount {
    private int id;
    private int userId;
    private String gameName;
    private String tagLine;
    private String puuid;
    private String summonerId;
    private int summonerLevel;
    private int profileIconId;
    private String tier;
    private String rankDivision;
    private int leaguePoints;
    private int wins;
    private int losses;
    private Instant lastUpdatedAt;
    private Instant createdAt;
    private Instant updatedAt;

    public RiotAccount() {}

    public String getRiotId() {
        return gameName + "#" + tagLine;
    }

    public String getFormattedRank() {
        if (tier == null || tier.isBlank()) return "Unranked";
        String t = tier.charAt(0) + tier.substring(1).toLowerCase();
        if (rankDivision == null || rankDivision.isBlank()) return t;
        return t + " " + rankDivision;
    }

    public double getWinRate() {
        int total = wins + losses;
        if (total == 0) return 0.0;
        return Math.round((wins * 1000.0) / total) / 10.0;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getGameName() { return gameName; }
    public void setGameName(String gameName) { this.gameName = gameName; }

    public String getTagLine() { return tagLine; }
    public void setTagLine(String tagLine) { this.tagLine = tagLine; }

    public String getPuuid() { return puuid; }
    public void setPuuid(String puuid) { this.puuid = puuid; }

    public String getSummonerId() { return summonerId; }
    public void setSummonerId(String summonerId) { this.summonerId = summonerId; }

    public int getSummonerLevel() { return summonerLevel; }
    public void setSummonerLevel(int summonerLevel) { this.summonerLevel = summonerLevel; }

    public int getProfileIconId() { return profileIconId; }
    public void setProfileIconId(int profileIconId) { this.profileIconId = profileIconId; }

    public String getTier() { return tier; }
    public void setTier(String tier) { this.tier = tier; }

    public String getRankDivision() { return rankDivision; }
    public void setRankDivision(String rankDivision) { this.rankDivision = rankDivision; }

    public int getLeaguePoints() { return leaguePoints; }
    public void setLeaguePoints(int leaguePoints) { this.leaguePoints = leaguePoints; }

    public int getWins() { return wins; }
    public void setWins(int wins) { this.wins = wins; }

    public int getLosses() { return losses; }
    public void setLosses(int losses) { this.losses = losses; }

    public Instant getLastUpdatedAt() { return lastUpdatedAt; }
    public void setLastUpdatedAt(Instant lastUpdatedAt) { this.lastUpdatedAt = lastUpdatedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
