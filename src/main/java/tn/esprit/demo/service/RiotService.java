package tn.esprit.demo.service;

import org.json.JSONArray;
import org.json.JSONObject;
import tn.esprit.demo.dao.RiotDao;
import tn.esprit.demo.model.MatchSummary;
import tn.esprit.demo.model.RiotAccount;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * RiotService
 * ───────────
 * Executes the three-step Riot Games API call chain:
 *
 *   1. GET /riot/account/v1/accounts/by-riot-id/{gameName}/{tagLine}
 *      → puuid, gameName, tagLine
 *
 *   2. GET /lol/summoner/v4/summoners/by-puuid/{puuid}
 *      → summonerId, summonerLevel, profileIconId
 *
 *   3. GET /lol/league/v4/entries/by-summoner/{summonerId}
 *      → tier, rank, leaguePoints, wins, losses (RANKED_SOLO_5x5 only)
 *
 * All methods are synchronous and must be called from a background thread.
 */
public class RiotService {

    // ── Endpoints ──────────────────────────────────────────────────────────────
    private static final String ACCOUNT_URL         =
            "https://europe.api.riotgames.com/riot/account/v1/accounts/by-riot-id/%s/%s";
    private static final String SUMMONER_URL         =
            "https://euw1.api.riotgames.com/lol/summoner/v4/summoners/by-puuid/%s";
    private static final String LEAGUE_URL           =
            "https://euw1.api.riotgames.com/lol/league/v4/entries/by-summoner/%s";
    private static final String LEAGUE_BY_PUUID_URL  =
            "https://euw1.api.riotgames.com/lol/league/v4/entries/by-puuid/%s";
    private static final String MATCH_LIST_URL       =
            "https://europe.api.riotgames.com/lol/match/v5/matches/by-puuid/%s/ids?start=0&count=%d";
    private static final String MATCH_DETAIL_URL     =
            "https://europe.api.riotgames.com/lol/match/v5/matches/%s";

    // ── State ──────────────────────────────────────────────────────────────────
    private final HttpClient http;
    private final RiotDao    riotDao;
    private final String     apiKey;

    // ── Constructor ────────────────────────────────────────────────────────────

    public RiotService(RiotDao riotDao, String apiKey) {
        this.riotDao = riotDao;
        this.apiKey  = apiKey;
        this.http = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Fetches full stats for a Riot ID by executing all 3 API calls in sequence.
     *
     * @param gameName the part before the # (e.g. "Faker")
     * @param tagLine  the part after  the # (e.g. "T1")
     * @return fully populated {@link RiotAccount}
     * @throws RiotException with a user-facing message on any failure
     */
    public RiotAccount fetchAccount(String gameName, String tagLine) throws RiotException {
        if (apiKey == null || apiKey.isBlank()) {
            throw new RiotException("Riot API key is not configured.", RiotException.Kind.CONFIG);
        }

        RiotAccount account = new RiotAccount();
        account.setGameName(gameName.trim());
        account.setTagLine(tagLine.trim());

        // Step 1 – PUUID
        fetchPuuid(account);

        // Step 2 – Summoner ID + level
        fetchSummoner(account);

        // Step 3 – Rank (RANKED_SOLO_5x5)
        fetchRank(account);

        return account;
    }

    /**
     * Persists a fetched account to MySQL for the given user.
     */
    public void saveAccount(long userId, RiotAccount account) {
        riotDao.saveRiotAccount(userId, account);
    }

    /**
     * Loads the stored account for a user from MySQL (no API call).
     */
    public RiotAccount loadAccount(long userId) {
        return riotDao.getRiotAccount(userId);
    }

    /**
     * Removes the stored account for a user.
     */
    public void unlinkAccount(long userId) {
        riotDao.deleteRiotAccount(userId);
    }

    /**
     * Fetches fresh stats from the Riot API and updates the DB.
     */
    public RiotAccount refreshAccount(long userId) throws RiotException {
        RiotAccount stored = riotDao.getRiotAccount(userId);
        if (stored == null) {
            throw new RiotException("No linked account to refresh.", RiotException.Kind.NOT_FOUND);
        }
        RiotAccount fresh = fetchAccount(stored.getGameName(), stored.getTagLine());
        riotDao.saveRiotAccount(userId, fresh);
        return fresh;
    }

    /** Returns a formatted win-rate string like {@code "61.3%"}. */
    public String getWinRate(int wins, int losses) {
        int total = wins + losses;
        if (total == 0) return "0.0%";
        double wr = Math.round((wins * 100.0 / total) * 10.0) / 10.0;
        return wr + "%";
    }

    /**
     * Returns the Data Dragon CDN URL for the given profile icon ID.
     * Fetches the latest patch version automatically; falls back to a
     * pinned version if the versions endpoint is unreachable.
     *
     * Example: https://ddragon.leagueoflegends.com/cdn/15.8.1/img/profileicon/6850.png
     */
    public String buildProfileIconUrl(int profileIconId) {
        String version = "15.9.1"; // fallback
        try {
            JSONArray versions = getArray("https://ddragon.leagueoflegends.com/api/versions.json");
            if (versions.length() > 0) version = versions.getString(0);
        } catch (Exception ignored) {}
        return "https://ddragon.leagueoflegends.com/cdn/" + version
                + "/img/profileicon/" + profileIconId + ".png";
    }

    /**
     * Fetches the last {@code count} ranked/normal matches for the given PUUID.
     * Each match detail is fetched individually. Failures on individual matches
     * are skipped rather than aborting the whole list.
     */
    public List<MatchSummary> fetchRecentMatches(String puuid, int count) throws RiotException {
        String url = String.format(MATCH_LIST_URL, encode(puuid), count);
        JSONArray ids = getArray(url);
        List<MatchSummary> results = new ArrayList<>();
        for (int i = 0; i < ids.length(); i++) {
            String matchId = ids.getString(i);
            try {
                JSONObject match = get(String.format(MATCH_DETAIL_URL, matchId));
                MatchSummary ms = parseMatch(match, puuid);
                if (ms != null) results.add(ms);
            } catch (Exception e) {
                System.err.println("[RiotService] Skipping match " + matchId + ": " + e.getMessage());
            }
        }
        return results;
    }

    private MatchSummary parseMatch(JSONObject match, String puuid) {
        JSONObject info = match.optJSONObject("info");
        if (info == null) return null;
        JSONArray participants = info.optJSONArray("participants");
        if (participants == null) return null;
        for (int i = 0; i < participants.length(); i++) {
            JSONObject p = participants.getJSONObject(i);
            if (!puuid.equals(p.optString("puuid"))) continue;
            MatchSummary ms = new MatchSummary();
            ms.setWin(p.optBoolean("win"));
            ms.setChampionName(p.optString("championName", "Unknown"));
            ms.setKills(p.optInt("kills"));
            ms.setDeaths(p.optInt("deaths"));
            ms.setAssists(p.optInt("assists"));
            ms.setGameDurationSeconds(info.optLong("gameDuration", 0));
            ms.setGameStartTimestamp(info.optLong("gameStartTimestamp", 0));
            ms.setQueueName(queueName(info.optInt("queueId", 0)));
            return ms;
        }
        return null;
    }

    private static String queueName(int id) {
        return switch (id) {
            case 420  -> "Ranked Solo/Duo";
            case 440  -> "Ranked Flex";
            case 450  -> "ARAM";
            case 400  -> "Normal Draft";
            case 430  -> "Normal Blind";
            case 900, 76, 1900 -> "URF";
            case 1020 -> "One for All";
            default   -> "League of Legends";
        };
    }

    // ── Private steps ──────────────────────────────────────────────────────────

    private void fetchPuuid(RiotAccount account) throws RiotException {
        String url = String.format(ACCOUNT_URL,
                encode(account.getGameName()), encode(account.getTagLine()));
        JSONObject json = get(url);
        account.setPuuid(json.getString("puuid"));
        // Riot returns canonical casing — use it
        account.setGameName(json.optString("gameName", account.getGameName()));
        account.setTagLine(json.optString("tagLine",   account.getTagLine()));
    }

    private void fetchSummoner(RiotAccount account) throws RiotException {
        String url     = String.format(SUMMONER_URL, encode(account.getPuuid()));
        String rawBody = rawGet(url);
        JSONObject json;
        try {
            json = new JSONObject(rawBody);
        } catch (Exception e) {
            throw new RiotException(
                "Summoner lookup returned unexpected data: " + truncate(rawBody),
                RiotException.Kind.SERVER_ERROR);
        }

        // Grab level + icon unconditionally (present in all known response shapes)
        account.setSummonerLevel(json.optInt("summonerLevel", 0));
        account.setProfileIconId(json.optInt("profileIconId", 0));

        // "id" = encrypted summoner ID — may be absent in newer Riot API versions.
        // If missing, set to empty string; fetchRank will fall back to the
        // PUUID-based league endpoint automatically.
        String summonerId = json.optString("id", "").trim();
        account.setSummonerId(summonerId);

        if (summonerId.isEmpty()) {
            System.out.println("[RiotService] Summoner 'id' not in response — " +
                    "will use PUUID-based rank endpoint. Body: " + truncate(rawBody));
        }
    }

    private void fetchRank(RiotAccount account) throws RiotException {
        // Use PUUID-based endpoint as primary when summonerId is unavailable,
        // otherwise use the classic summoner-based endpoint.
        boolean usePuuid = account.getSummonerId() == null
                        || account.getSummonerId().isBlank();

        String url = usePuuid
                ? String.format(LEAGUE_BY_PUUID_URL, encode(account.getPuuid()))
                : String.format(LEAGUE_URL,          encode(account.getSummonerId()));

        String rawBody = rawGet(url);
        JSONArray array;
        try {
            array = new JSONArray(rawBody);
        } catch (Exception e) {
            // Response is not a JSON array — could be an error object
            throw new RiotException(
                "Rank lookup returned unexpected data: " + truncate(rawBody),
                RiotException.Kind.SERVER_ERROR);
        }

        // Find RANKED_SOLO_5x5; leave fields blank if player is unranked
        for (int i = 0; i < array.length(); i++) {
            JSONObject entry = array.getJSONObject(i);
            if ("RANKED_SOLO_5x5".equals(entry.optString("queueType"))) {
                account.setTier(entry.optString("tier", ""));
                account.setRank(entry.optString("rank", ""));
                account.setLeaguePoints(entry.optInt("leaguePoints", 0));
                account.setWins(entry.optInt("wins", 0));
                account.setLosses(entry.optInt("losses", 0));
                return;
            }
        }
        // Unranked — leave fields at defaults (tier = null, wins/losses = 0)
    }

    // ── HTTP helpers ───────────────────────────────────────────────────────────

    private JSONObject get(String url) throws RiotException {
        return new JSONObject(rawGet(url));
    }

    private JSONArray getArray(String url) throws RiotException {
        return new JSONArray(rawGet(url));
    }

    private String rawGet(String url) throws RiotException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("X-Riot-Token", apiKey)
                .GET()
                .timeout(Duration.ofSeconds(15))
                .build();
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            return switch (response.statusCode()) {
                case 200 -> response.body();
                case 404 -> throw new RiotException(
                        "Player not found. Check your Riot ID and tag.",
                        RiotException.Kind.NOT_FOUND);
                case 403, 401 -> throw new RiotException(
                        "API key expired or unauthorised. Please try again later.",
                        RiotException.Kind.FORBIDDEN);
                case 429 -> throw new RiotException(
                        "Rate limit exceeded. Please wait a moment and try again.",
                        RiotException.Kind.RATE_LIMIT);
                default  -> throw new RiotException(
                        "Riot API error (HTTP " + response.statusCode() + ").",
                        RiotException.Kind.SERVER_ERROR);
            };
        } catch (RiotException e) {
            throw e;
        } catch (java.net.ConnectException e) {
            throw new RiotException(
                    "Could not reach Riot servers. Check your connection.",
                    RiotException.Kind.NETWORK);
        } catch (Exception e) {
            throw new RiotException(
                    "Could not reach Riot servers. Check your connection.",
                    RiotException.Kind.NETWORK);
        }
    }

    private static String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    /** Truncates a string for safe error message inclusion. */
    private static String truncate(String s) {
        if (s == null) return "(null)";
        return s.length() > 300 ? s.substring(0, 300) + "…" : s;
    }

    // ── Exception ──────────────────────────────────────────────────────────────

    /** Typed exception for all Riot API failures, with a user-facing message. */
    public static class RiotException extends Exception {

        public enum Kind { NOT_FOUND, FORBIDDEN, RATE_LIMIT, NETWORK, SERVER_ERROR, CONFIG }

        private final Kind kind;

        public RiotException(String message, Kind kind) {
            super(message);
            this.kind = kind;
        }

        public Kind getKind() { return kind; }
    }
}
