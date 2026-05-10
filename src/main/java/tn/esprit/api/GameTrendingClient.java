package tn.esprit.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import tn.esprit.api.model.TrendingGame;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class GameTrendingClient {
    private static final String RAWG_BASE_URL = "https://api.rawg.io/api/games";
    private static final int PAGE_SIZE = 5;
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ApiConfig config;

    public GameTrendingClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();
        this.objectMapper = new ObjectMapper();
        this.config = ApiConfig.getInstance();
    }

    public List<TrendingGame> fetch() throws Exception {
        if (!config.hasRawgApiKey()) {
            System.err.println("⚠  RAWG API key not configured. Using fallback trending games.");
            return getFallback();
        }

        String url = RAWG_BASE_URL + "?key=" + config.getRawgApiKey()
                + "&ordering=-rating&page_size=" + PAGE_SIZE;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(TIMEOUT)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            System.err.println("⚠  RAWG API returned status " + response.statusCode() + ". Using fallback.");
            return getFallback();
        }

        return parseResponse(response.body());
    }

    private List<TrendingGame> parseResponse(String json) throws Exception {
        List<TrendingGame> games = new ArrayList<>();
        JsonNode root = objectMapper.readTree(json);
        JsonNode results = root.get("results");

        if (results == null || !results.isArray()) {
            return getFallback();
        }

        for (JsonNode node : results) {
            String name = node.has("name") ? node.get("name").asText() : "Unknown";
            double rating = node.has("rating") ? node.get("rating").asDouble() : 0.0;
            String imageUrl = node.has("background_image") && !node.get("background_image").isNull()
                    ? node.get("background_image").asText() : null;
            games.add(new TrendingGame(name, rating, imageUrl));
        }

        return games;
    }

    public List<String> searchGames(String query, int limit) {
        if (!config.hasRawgApiKey() || query == null || query.trim().isEmpty()) {
            return List.of();
        }

        try {
            String url = RAWG_BASE_URL + "?key=" + config.getRawgApiKey()
                    + "&search=" + java.net.URLEncoder.encode(query.trim(), "UTF-8")
                    + "&page_size=" + Math.min(limit, 10);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(TIMEOUT)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                return List.of();
            }

            List<String> names = new ArrayList<>();
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode results = root.get("results");

            if (results != null && results.isArray()) {
                for (JsonNode node : results) {
                    if (node.has("name")) {
                        names.add(node.get("name").asText());
                    }
                }
            }
            return names;
        } catch (Exception e) {
            System.err.println("⚠  RAWG search failed: " + e.getMessage());
            return List.of();
        }
    }

    public byte[] downloadImage(String imageUrl) throws Exception {
        if (imageUrl == null || imageUrl.isEmpty()) return null;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(imageUrl))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        return response.statusCode() == 200 ? response.body() : null;
    }

    private List<TrendingGame> getFallback() {
        List<TrendingGame> fallback = new ArrayList<>();
        fallback.add(new TrendingGame("League of Legends", 4.5, null));
        fallback.add(new TrendingGame("Valorant", 4.3, null));
        fallback.add(new TrendingGame("CS2", 4.4, null));
        fallback.add(new TrendingGame("Dota 2", 4.2, null));
        fallback.add(new TrendingGame("Fortnite", 4.1, null));
        return fallback;
    }
}
