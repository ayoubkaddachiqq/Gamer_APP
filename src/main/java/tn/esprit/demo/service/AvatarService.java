package tn.esprit.demo.service;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Base64;

/**
 * AvatarService
 * ─────────────
 * Handles the two-step AI avatar generation pipeline:
 *
 *  1. uploadToImgbb()  — encodes a local file to Base64 and posts it to imgbb,
 *                        returning a public CDN URL.
 *  2. cartoonify()     — posts that URL to LightX's /cartoon endpoint,
 *                        polls /order-status until the job completes,
 *                        and returns the cartoonified image URL.
 *  3. generateAvatar() — convenience wrapper: upload → cartoonify → return URL.
 *
 * All network calls are synchronous (blocking) and must be invoked from a
 * background thread (JavaFX Task) — never on the Application Thread.
 */
public class AvatarService {

    // ── Constants ──────────────────────────────────────────────────────────────
    private static final long   MAX_FILE_BYTES  = 5 * 1024 * 1024L;           // 5 MB
    private static final String IMGBB_ENDPOINT  = "https://api.imgbb.com/1/upload";
    private static final String LIGHTX_CARTOON  = "https://api.lightxeditor.com/external/api/v1/cartoon";
    private static final String LIGHTX_STATUS   = "https://api.lightxeditor.com/external/api/v1/order-status";
    private static final String TEXT_PROMPT     = "esports gamer avatar, dark cyberpunk style, gold accents";
    private static final int    POLL_INTERVAL_MS = 2_000;
    private static final int    POLL_MAX_TRIES   = 20;    // 20 × 2 s = 40 s max

    // ── State ──────────────────────────────────────────────────────────────────
    private final HttpClient http;
    private final String     imgbbKey;
    private final String     lightxKey;

    // ── Constructor ────────────────────────────────────────────────────────────

    public AvatarService(String imgbbKey, String lightxKey) {
        this.imgbbKey  = imgbbKey;
        this.lightxKey = lightxKey;
        this.http = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Full pipeline: upload photo → cartoonify → return final CDN URL.
     *
     * @param imageFile local image file selected by the user
     * @return publicly accessible URL of the AI-generated avatar
     * @throws AvatarException on any failure (with a user-friendly message)
     */
    public String generateAvatar(File imageFile) throws AvatarException {
        String hostedUrl = uploadToImgbb(imageFile);
        return cartoonify(hostedUrl);
    }

    /**
     * Uploads a local image file to imgbb and returns its public CDN URL.
     *
     * @param imageFile the file to upload (must be ≤ 5 MB)
     * @return the public https:// URL from imgbb's {@code data.url} field
     * @throws AvatarException on validation failure, network error, or API error
     */
    public String uploadToImgbb(File imageFile) throws AvatarException {
        // ── 1. Validate ───────────────────────────────────────────────────────
        if (imageFile == null || !imageFile.exists()) {
            throw new AvatarException("No file selected.");
        }
        if (imageFile.length() > MAX_FILE_BYTES) {
            throw new AvatarException("Image too large. Please pick a file under 5 MB.");
        }
        if (imgbbKey == null || imgbbKey.isBlank()) {
            throw new AvatarException("imgbb API key is not configured.");
        }

        // ── 2. Encode to Base64 ───────────────────────────────────────────────
        byte[] fileBytes;
        try {
            fileBytes = Files.readAllBytes(imageFile.toPath());
        } catch (IOException e) {
            throw new AvatarException("Could not read image file: " + e.getMessage());
        }
        String base64Image = Base64.getEncoder().encodeToString(fileBytes);

        // ── 3. Build URL-encoded form body ────────────────────────────────────
        String formBody = "image=" + URLEncoder.encode(base64Image, StandardCharsets.UTF_8);

        // ── 4. POST to imgbb ─────────────────────────────────────────────────
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(IMGBB_ENDPOINT + "?key=" + imgbbKey))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formBody))
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = sendRequest(request, "Image upload failed. Check your connection.");

        // ── 5. Parse response ─────────────────────────────────────────────────
        try {
            JSONObject json = new JSONObject(response.body());
            if (!json.optBoolean("success", false)) {
                String err = json.optString("error", "Unknown imgbb error");
                throw new AvatarException("Image upload failed: " + err);
            }
            JSONObject data = json.getJSONObject("data");
            // Use display_url — this is the direct CDN image link that external
            // APIs (like LightX) can actually download. data.url is an imgbb
            // viewer page URL which returns HTML, not the raw image.
            String url = data.optString("display_url", null);
            if (url == null || url.isBlank()) {
                // Fallback to url if display_url missing
                url = data.optString("url", null);
            }
            if (url == null || url.isBlank()) {
                throw new AvatarException("Image upload failed. imgbb returned an empty URL.");
            }
            return url;
        } catch (AvatarException e) {
            throw e;
        } catch (Exception e) {
            throw new AvatarException("Image upload failed. Unexpected response from server.");
        }
    }

    /**
     * Sends a hosted image URL to LightX /cartoon and polls until the cartoonified
     * result is ready.
     *
     * @param imageUrl the public CDN URL of the source image
     * @return the cartoonified image URL
     * @throws AvatarException on network error, API error, or timeout
     */
    public String cartoonify(String imageUrl) throws AvatarException {
        if (lightxKey == null || lightxKey.isBlank()) {
            throw new AvatarException("LightX API key is not configured.");
        }

        // ── 1. POST /cartoon ──────────────────────────────────────────────────
        JSONObject body = new JSONObject();
        body.put("imageUrl",   imageUrl);
        body.put("textPrompt", TEXT_PROMPT);

        HttpRequest cartoonRequest = HttpRequest.newBuilder()
                .uri(URI.create(LIGHTX_CARTOON))
                .header("Content-Type", "application/json")
                .header("x-api-key", lightxKey)
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> cartoonResponse = sendRequest(cartoonRequest,
                "Avatar generation failed. Check your connection and try again.");

        // ── 2. Parse initial response — look for orderId or direct URL ────────
        JSONObject cartoonJson;
        try {
            cartoonJson = new JSONObject(cartoonResponse.body());
        } catch (Exception e) {
            throw new AvatarException("Avatar generation failed. Unexpected response from LightX.");
        }

        // LightX may return the result directly or via async order polling
        String directUrl = extractOutputUrl(cartoonJson);
        if (directUrl != null) return directUrl;

        // ── 3. Extract orderId for polling ────────────────────────────────────
        String orderId = extractOrderId(cartoonJson);
        if (orderId == null || orderId.isBlank()) {
            throw new AvatarException("Avatar generation failed. LightX returned no order ID.");
        }

        // ── 4. Poll until complete ─────────────────────────────────────────────
        return pollForResult(orderId);
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    /**
     * Polls the LightX order-status endpoint every {@code POLL_INTERVAL_MS} ms
     * up to {@code POLL_MAX_TRIES} times.
     */
    private String pollForResult(String orderId) throws AvatarException {
        String statusUrl = LIGHTX_STATUS + "?orderId=" + orderId;

        for (int attempt = 0; attempt < POLL_MAX_TRIES; attempt++) {
            try {
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AvatarException("Avatar generation was interrupted.");
            }

            HttpRequest statusRequest = HttpRequest.newBuilder()
                    .uri(URI.create(statusUrl))
                    .header("x-api-key", lightxKey)
                    .GET()
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> statusResponse = sendRequest(statusRequest,
                    "Avatar generation failed. Could not retrieve status.");

            JSONObject statusJson;
            try {
                statusJson = new JSONObject(statusResponse.body());
            } catch (Exception e) {
                continue; // malformed response — retry
            }

            String status = extractStatus(statusJson);
            String outputUrl = extractOutputUrl(statusJson);

            if (outputUrl != null && !outputUrl.isBlank()) {
                return outputUrl;
            }
            if ("failed".equalsIgnoreCase(status) || "error".equalsIgnoreCase(status)) {
                throw new AvatarException("Avatar generation failed. LightX reported an error.");
            }
            // status == "pending" / "processing" — keep polling
        }

        throw new AvatarException("Avatar generation timed out. Please try again.");
    }

    /**
     * Sends an HTTP request and throws {@link AvatarException} on network or
     * non-2xx HTTP errors. Includes the raw response body in the exception
     * message so failures surface as actionable UI text, not just HTTP codes.
     */
    private HttpResponse<String> sendRequest(HttpRequest request, String userMessage)
            throws AvatarException {
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                // Extract detail from JSON error body if possible
                String detail = extractErrorDetail(response.body());
                throw new AvatarException(userMessage + " (HTTP " + response.statusCode()
                        + (detail != null ? ": " + detail : "") + ")");
            }
            return response;
        } catch (AvatarException e) {
            throw e;
        } catch (java.net.ConnectException e) {
            throw new AvatarException(userMessage + " Could not connect to server.");
        } catch (Exception e) {
            throw new AvatarException(userMessage + " [" + e.getMessage() + "]");
        }
    }

    /** Tries to pull a human-readable message from a JSON error response body. */
    private String extractErrorDetail(String body) {
        if (body == null || body.isBlank()) return null;
        try {
            JSONObject j = new JSONObject(body);
            if (j.has("message"))    return j.getString("message");
            if (j.has("error"))      return j.get("error").toString();
            if (j.has("detail"))     return j.getString("detail");
            if (j.has("statusMessage")) return j.getString("statusMessage");
        } catch (Exception ignored) {}
        // Return raw body (truncated) so we always have something useful
        return body.length() > 200 ? body.substring(0, 200) + "…" : body;
    }

    /**
     * Tries to extract the output image URL from various LightX response shapes.
     * LightX may nest it differently depending on API version.
     */
    private String extractOutputUrl(JSONObject json) {
        try {
            // Shape: { "body": { "imageUrl": "..." } }
            if (json.has("body")) {
                JSONObject bodyObj = json.getJSONObject("body");
                if (bodyObj.has("imageUrl")) return bodyObj.getString("imageUrl");
                if (bodyObj.has("output"))   return bodyObj.getString("output");
            }
            // Shape: { "data": { "imageUrl": "..." } }
            if (json.has("data")) {
                Object dataRaw = json.get("data");
                if (dataRaw instanceof JSONObject dataObj) {
                    if (dataObj.has("imageUrl")) return dataObj.getString("imageUrl");
                    if (dataObj.has("output"))   return dataObj.getString("output");
                }
            }
            // Shape: { "output": "..." }
            if (json.has("output"))   return json.getString("output");
            if (json.has("imageUrl")) return json.getString("imageUrl");
        } catch (Exception ignored) {}
        return null;
    }

    /** Extracts orderId from various known LightX response shapes. */
    private String extractOrderId(JSONObject json) {
        try {
            if (json.has("body")) {
                JSONObject bodyObj = json.getJSONObject("body");
                if (bodyObj.has("orderId")) return bodyObj.getString("orderId");
                if (bodyObj.has("order_id")) return bodyObj.getString("order_id");
            }
            if (json.has("orderId"))   return json.getString("orderId");
            if (json.has("order_id"))  return json.getString("order_id");
            if (json.has("data")) {
                Object raw = json.get("data");
                if (raw instanceof JSONObject d) {
                    if (d.has("orderId"))  return d.getString("orderId");
                    if (d.has("order_id")) return d.getString("order_id");
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    /** Extracts status string from LightX order-status response. */
    private String extractStatus(JSONObject json) {
        try {
            if (json.has("body")) {
                JSONObject b = json.getJSONObject("body");
                if (b.has("status")) return b.getString("status");
            }
            if (json.has("status")) return json.getString("status");
            if (json.has("data")) {
                Object raw = json.get("data");
                if (raw instanceof JSONObject d && d.has("status")) return d.getString("status");
            }
        } catch (Exception ignored) {}
        return "pending";
    }

    // ── Exception ──────────────────────────────────────────────────────────────

    /** Thrown for any avatar generation failure, with a user-facing message. */
    public static class AvatarException extends Exception {
        public AvatarException(String message) { super(message); }
    }
}
