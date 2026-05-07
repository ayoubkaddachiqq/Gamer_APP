package tn.esprit.demo.service;

import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * HTTP client that communicates with the Python Face Auth microservice
 * running at http://localhost:8765.
 *
 * All calls are synchronous (blocking) — they are intended to be invoked
 * from a JavaFX background thread so the UI is never frozen.
 */
public class FaceAuthClient {

    private static final String BASE_URL = "http://127.0.0.1:8765";
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final HttpClient http;

    public FaceAuthClient() {
        this.http = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)   // uvicorn doesn't support HTTP/2 upgrade
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    // ─── Result record ──────────────────────────────────────────────────────

    /** Result returned by {@link #recognize()}. */
    public record RecognizeResult(boolean matched, String email, double confidence, String message) {}

    // ─── Public API ─────────────────────────────────────────────────────────

    /**
     * Checks whether the Python service is reachable.
     *
     * @return {@code true} if the service responded with HTTP 200
     */
    public boolean ping() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/health"))
                    .GET()
                    .timeout(Duration.ofSeconds(3))
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            return resp.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Triggers face recognition via the Python service.
     * The service opens the webcam on its side, captures a frame,
     * and returns the closest match.
     *
     * @return a {@link RecognizeResult} — check {@code matched} before using {@code email}
     * @throws FaceAuthException if the service is unreachable or returns an error
     */
    public RecognizeResult recognize() throws FaceAuthException {
        String body = post("/recognize", "{}");
        JSONObject json = new JSONObject(body);
        return new RecognizeResult(
                json.getBoolean("matched"),
                json.optString("email", null),
                json.optDouble("confidence", 0.0),
                json.optString("message", "")
        );
    }

    /**
     * Enrolls a user's face in the Python service.
     * The service opens the webcam, captures a frame, and saves the embedding.
     *
     * @param userId the database user ID
     * @param email  the user's email address
     * @throws FaceAuthException if the service is unreachable or returns an error
     */
    public void enroll(long userId, String email) throws FaceAuthException {
        JSONObject payload = new JSONObject();
        payload.put("user_id", userId);
        payload.put("email", email);
        post("/enroll", payload.toString());
    }

    // ─── Internal ───────────────────────────────────────────────────────────

    private String post(String path, String jsonBody) throws FaceAuthException {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + path))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(TIMEOUT)
                    .build();

            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() == 200 || resp.statusCode() == 201) {
                return resp.body();
            }

            // Parse FastAPI error detail if available
            String detail = resp.body();
            try {
                detail = new JSONObject(resp.body()).optString("detail", resp.body());
            } catch (Exception ignored) {}

            throw new FaceAuthException("Face service error (" + resp.statusCode() + "): " + detail);

        } catch (FaceAuthException e) {
            throw e;
        } catch (java.net.ConnectException e) {
            throw new FaceAuthException("Face ID service is not running. Please start face_service/start.bat first.");
        } catch (Exception e) {
            throw new FaceAuthException("Unexpected error communicating with Face ID service: " + e.getMessage());
        }
    }

    // ─── Exception ──────────────────────────────────────────────────────────

    /** Thrown when the face auth service cannot be reached or returns an error. */
    public static class FaceAuthException extends Exception {
        public FaceAuthException(String message) {
            super(message);
        }
    }
}
