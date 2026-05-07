package tn.esprit.demo.service;

import org.json.JSONArray;
import org.json.JSONObject;
import tn.esprit.demo.dao.GameDao;
import tn.esprit.demo.model.UserGame;

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
 * Business logic layer for the RAWG Game Database integration.
 *
 * All RAWG API calls use java.net.http.HttpClient (no external HTTP library).
 * JSON is parsed with org.json.
 */
public class GameService {

    private static final Duration TIMEOUT = Duration.ofSeconds(8);
    private static final int MAX_FAVORITES = 4;

    private final GameDao gameDao;
    private final String  apiKey;
    private final String  baseUrl;
    private final HttpClient http;

    public GameService(GameDao gameDao, ApiConfig apiConfig) {
        this.gameDao = gameDao;
        this.apiKey  = apiConfig.getRawgApiKey();
        this.baseUrl = apiConfig.getRawgBaseUrl();
        this.http    = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();
    }

    // ── RAWG Search ───────────────────────────────────────────────────────────

    /**
     * Searches the RAWG API and returns up to 10 results as {@link UserGame} objects.
     * Throws {@link IllegalStateException} on network or API error so the controller
     * can display an inline error message.
     */
    public List<UserGame> searchGames(String query) {
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = baseUrl + "/games?search=" + encoded
                + "&key=" + apiKey
                + "&page_size=10"
                + "&search_precise=false";

        String body = get(url);
        return parseSearchResults(body);
    }

    private List<UserGame> parseSearchResults(String body) {
        List<UserGame> results = new ArrayList<>();
        JSONObject root = new JSONObject(body);
        JSONArray items = root.optJSONArray("results");
        if (items == null) return results;

        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.getJSONObject(i);
            UserGame g = new UserGame();
            g.setRawgId(item.optInt("id", 0));
            g.setGameName(item.optString("name", "Unknown"));
            g.setCoverUrl(item.optString("background_image", null));
            g.setMetacriticScore(item.optInt("metacritic", 0));

            // First genre from the genres array
            JSONArray genres = item.optJSONArray("genres");
            if (genres != null && genres.length() > 0) {
                g.setGenre(genres.getJSONObject(0).optString("name", ""));
            }
            results.add(g);
        }
        return results;
    }

    // ── Library operations ────────────────────────────────────────────────────

    public void addGameToLibrary(long userId, UserGame game) {
        gameDao.addGame(userId, game);
    }

    public void removeGameFromLibrary(long userId, int rawgId) {
        gameDao.removeGame(userId, rawgId);
    }

    public List<UserGame> getUserLibrary(long userId) {
        return gameDao.getUserGames(userId);
    }

    public List<UserGame> getFavorites(long userId) {
        return gameDao.getFavoriteGames(userId);
    }

    /**
     * Toggles the favorite flag. Enforces a maximum of {@value MAX_FAVORITES} favorites.
     *
     * @throws IllegalStateException with a user-facing message when the cap is reached.
     */
    public void toggleFavorite(long userId, int rawgId, boolean isFavorite) {
        if (isFavorite) {
            int current = gameDao.countFavorites(userId);
            if (current >= MAX_FAVORITES) {
                throw new IllegalStateException(
                        "You can only have 4 favorites. Unmark another game first.");
            }
        }
        gameDao.setFavorite(userId, rawgId, isFavorite);
    }

    public boolean isGameSaved(long userId, int rawgId) {
        return gameDao.isGameSaved(userId, rawgId);
    }

    public int countFavorites(long userId) {
        return gameDao.countFavorites(userId);
    }

    // ── HTTP helper ───────────────────────────────────────────────────────────

    private String get(String url) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(TIMEOUT)
                    .header("User-Agent", "TeamHub-Desktop/1.0")
                    .GET()
                    .build();
            HttpResponse<String> resp =
                    http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() != 200) {
                throw new IllegalStateException(
                        "RAWG API returned status " + resp.statusCode());
            }
            return resp.body();
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Could not reach game database. Check your connection.", e);
        }
    }
}
