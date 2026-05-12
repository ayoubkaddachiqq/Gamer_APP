package tn.esprit.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import tn.esprit.api.model.ToxicityResult;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class ToxicityClient {
    private static final String HF_URL = "https://router.huggingface.co/hf-inference/models/unitary/toxic-bert";
    private static final double TOXICITY_THRESHOLD = 0.7;
    private static final Duration TIMEOUT = Duration.ofSeconds(60);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ApiConfig config;

    public ToxicityClient() {
        this.httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
        this.objectMapper = new ObjectMapper();
        this.config = ApiConfig.getInstance();
    }

    public void warmUp() {
        if (!isConfigured()) return;
        new Thread(() -> {
            try {
                System.out.println("Warming up toxicity model...");
                check("hello world");
                System.out.println("Toxicity model ready.");
            } catch (Exception e) {
                System.err.println("Toxicity warm-up failed (will warm on first use): " + e.getMessage());
            }
        }).start();
    }

    public boolean isConfigured() {
        return config.hasHuggingFaceToken();
    }

    public ToxicityResult check(String text) {
        if (!isConfigured()) return null;
        if (text == null || text.trim().isEmpty()) return null;

        try {
            String escaped = text.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r");
            String requestBody = "{\"inputs\":\"" + escaped + "\"}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(HF_URL))
                    .timeout(TIMEOUT)
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + config.getHuggingFaceToken())
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.println("⚠  HuggingFace API error: " + response.statusCode());
                return null;
            }

            return parseResponse(response.body());
        } catch (Exception e) {
            System.err.println("⚠  Toxicity check failed: " + e.getMessage());
            return null;
        }
    }

    private ToxicityResult parseResponse(String json) throws Exception {
        JsonNode root = objectMapper.readTree(json);

        JsonNode predictions;
        if (root.isArray() && root.size() > 0 && root.get(0).isArray()) {
            predictions = root.get(0);
        } else if (root.isArray()) {
            predictions = root;
        } else {
            return null;
        }

        String worstLabel = "";
        double worstScore = 0;

        for (JsonNode pred : predictions) {
            String label = pred.get("label").asText().toLowerCase();
            double score = pred.get("score").asDouble();
            if (score > worstScore) {
                worstScore = score;
                worstLabel = label;
            }
        }

        return new ToxicityResult(worstLabel, worstScore, worstScore >= TOXICITY_THRESHOLD);
    }
}
