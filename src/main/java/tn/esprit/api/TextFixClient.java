package tn.esprit.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import tn.esprit.api.model.TextFixResult;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class TextFixClient {
    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL = "llama-3.3-70b-versatile";
    private static final Duration TIMEOUT = Duration.ofSeconds(20);
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 2000;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ApiConfig config;

    public TextFixClient() {
        this.httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
        this.objectMapper = new ObjectMapper();
        this.config = ApiConfig.getInstance();
    }

    public TextFixResult fix(String text) {
        if (!config.hasGroqApiKey()) {
            return new TextFixResult(false, "Groq API key not configured. Add groq.api.key to api.properties");
        }
        if (text == null || text.trim().isEmpty()) {
            return new TextFixResult(false, "No text to fix");
        }

        try {
            String escaped = text.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");

            String requestBody = "{\"model\":\"" + MODEL + "\",\"messages\":[" +
                    "{\"role\":\"system\",\"content\":\"Fix grammar, spelling, punctuation and clarity. Return ONLY the corrected text, no explanations.\"}," +
                    "{\"role\":\"user\",\"content\":\"" + escaped + "\"}]}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GROQ_URL))
                    .timeout(TIMEOUT)
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + config.getGroqApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    return parseResponse(response.body());
                }

                if (response.statusCode() == 429 && attempt < MAX_RETRIES) {
                    System.err.println("⚠  Groq rate limited, retrying in " + RETRY_DELAY_MS + "ms (attempt " + attempt + "/" + MAX_RETRIES + ")");
                    try { Thread.sleep(RETRY_DELAY_MS); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
                } else {
                    String body = response.body();
                    if (body != null) {
                        try {
                            JsonNode err = objectMapper.readTree(body);
                            String msg = err.has("error") ? err.get("error").get("message").asText() : body;
                            return new TextFixResult(false, msg);
                        } catch (Exception e) {
                            return new TextFixResult(false, "API error " + response.statusCode());
                        }
                    }
                    return new TextFixResult(false, "API error " + response.statusCode());
                }
            }

            return new TextFixResult(false, "Still rate limited after retries. Try again later.");
        } catch (Exception e) {
            return new TextFixResult(false, "Error: " + e.getMessage());
        }
    }

    private TextFixResult parseResponse(String json) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        JsonNode choices = root.get("choices");
        if (choices != null && choices.isArray() && choices.size() > 0) {
            JsonNode message = choices.get(0).get("message");
            if (message != null && message.has("content") && !message.get("content").isNull()) {
                String fixed = message.get("content").asText().trim();
                return new TextFixResult(fixed);
            }
        }
        return new TextFixResult(false, "Unexpected API response format");
    }
}
