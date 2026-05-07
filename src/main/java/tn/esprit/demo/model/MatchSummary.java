package tn.esprit.demo.model;

public class MatchSummary {
    private boolean win;
    private String  championName;
    private int     kills, deaths, assists;
    private String  queueName;
    private long    gameStartTimestamp; // epoch ms
    private long    gameDurationSeconds;

    public String getKda()  { return kills + " / " + deaths + " / " + assists; }

    public String getRelativeTime() {
        long diff = System.currentTimeMillis() - gameStartTimestamp;
        long min  = diff / 60_000L;
        long hr   = diff / 3_600_000L;
        long day  = diff / 86_400_000L;
        if (min  <  60) return min  + "m ago";
        if (hr   <  24) return hr   + "h ago";
        if (day  ==  1) return "Yesterday";
        return day + " days ago";
    }

    public String getDuration() {
        return String.format("%d:%02d", gameDurationSeconds / 60, gameDurationSeconds % 60);
    }

    public boolean isWin()                       { return win; }
    public void    setWin(boolean v)              { win = v; }
    public String  getChampionName()              { return championName; }
    public void    setChampionName(String v)      { championName = v; }
    public int     getKills()                     { return kills; }
    public void    setKills(int v)                { kills = v; }
    public int     getDeaths()                    { return deaths; }
    public void    setDeaths(int v)               { deaths = v; }
    public int     getAssists()                   { return assists; }
    public void    setAssists(int v)              { assists = v; }
    public String  getQueueName()                 { return queueName; }
    public void    setQueueName(String v)         { queueName = v; }
    public long    getGameStartTimestamp()        { return gameStartTimestamp; }
    public void    setGameStartTimestamp(long v)  { gameStartTimestamp = v; }
    public long    getGameDurationSeconds()       { return gameDurationSeconds; }
    public void    setGameDurationSeconds(long v) { gameDurationSeconds = v; }
}
