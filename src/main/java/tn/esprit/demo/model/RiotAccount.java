package tn.esprit.demo.model;

/**
 * Represents a linked Riot Games / League of Legends account.
 * Populated from the three sequential Riot API calls:
 *   1. /riot/account/v1/accounts/by-riot-id  → puuid, gameName, tagLine
 *   2. /lol/summoner/v4/summoners/by-puuid   → summonerId, summonerLevel, profileIconId
 *   3. /lol/league/v4/entries/by-summoner    → tier, rank, leaguePoints, wins, losses
 */
public class RiotAccount {

    private String gameName;
    private String tagLine;
    private String puuid;
    private String summonerId;
    private int    summonerLevel;
    private int    profileIconId;
    private String tier;          // e.g. "GOLD"
    private String rank;          // e.g. "II"
    private int    leaguePoints;
    private int    wins;
    private int    losses;

    public RiotAccount() {}

    // ── Computed ───────────────────────────────────────────────────────────────

    /**
     * Win rate as a percentage rounded to 1 decimal place.
     * Returns 0.0 if no games played.
     */
    public double getWinRate() {
        int total = wins + losses;
        if (total == 0) return 0.0;
        double raw = (wins * 100.0) / total;
        return Math.round(raw * 10.0) / 10.0;
    }

    /** Formatted rank string, e.g. "Gold II". Returns "Unranked" if no rank data. */
    public String getFormattedRank() {
        if (tier == null || tier.isBlank()) return "Unranked";
        String t = tier.charAt(0) + tier.substring(1).toLowerCase();
        if (rank == null || rank.isBlank()) return t;
        return t + " " + rank;
    }

    /** Riot ID in standard display format, e.g. "Faker#T1". */
    public String getRiotId() {
        return gameName + "#" + tagLine;
    }

    // ── Getters / Setters ──────────────────────────────────────────────────────

    public String getGameName()        { return gameName; }
    public void   setGameName(String v) { this.gameName = v; }

    public String getTagLine()         { return tagLine; }
    public void   setTagLine(String v)  { this.tagLine = v; }

    public String getPuuid()           { return puuid; }
    public void   setPuuid(String v)    { this.puuid = v; }

    public String getSummonerId()           { return summonerId; }
    public void   setSummonerId(String v)    { this.summonerId = v; }

    public int    getSummonerLevel()        { return summonerLevel; }
    public void   setSummonerLevel(int v)   { this.summonerLevel = v; }

    public int    getProfileIconId()        { return profileIconId; }
    public void   setProfileIconId(int v)   { this.profileIconId = v; }

    public String getTier()            { return tier; }
    public void   setTier(String v)     { this.tier = v; }

    public String getRank()            { return rank; }
    public void   setRank(String v)     { this.rank = v; }

    public int    getLeaguePoints()     { return leaguePoints; }
    public void   setLeaguePoints(int v){ this.leaguePoints = v; }

    public int    getWins()             { return wins; }
    public void   setWins(int v)        { this.wins = v; }

    public int    getLosses()           { return losses; }
    public void   setLosses(int v)      { this.losses = v; }
}
