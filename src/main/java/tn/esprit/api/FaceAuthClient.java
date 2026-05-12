package tn.esprit.api;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class FaceAuthClient {

    private static final String BASE_URL = "http://localhost:8765";
    private final HttpClient client = HttpClient.newHttpClient();

    public boolean enroll(int userId, String imagePath) {
        try {
            String json = String.format("{\"user_id\": %d, \"image_path\": \"%s\"}", userId, imagePath);
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/enroll"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            return resp.statusCode() == 200;
        } catch (Exception e) {
            System.err.println("[FaceAuth] Enroll error: " + e.getMessage());
            return false;
        }
    }

    public boolean verify(int userId, String imagePath) {
        try {
            String json = String.format("{\"user_id\": %d, \"image_path\": \"%s\"}", userId, imagePath);
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/verify"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            return resp.statusCode() == 200;
        } catch (Exception e) {
            System.err.println("[FaceAuth] Verify error: " + e.getMessage());
            return false;
        }
    }
}
