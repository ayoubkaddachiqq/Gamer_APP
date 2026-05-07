package tn.esprit.demo.util;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Locale;

/**
 * Checks a plaintext password against the HaveIBeenPwned Pwned Passwords API
 * using the k-Anonymity model — only the first 5 hex characters of the SHA-1
 * hash are sent over the network; the full password (or its full hash) never
 * leaves this machine.
 *
 * <p>Usage:
 * <pre>
 *   HibpClient.PwnResult result = HibpClient.check("MyPassword123");
 *   if (result.isPwned()) {
 *       // result.count() tells how many times it appeared in breaches
 *   }
 * </pre>
 *
 * <p>Network failures are surfaced as a {@link PwnResult} with
 * {@code error = true} so callers can choose to fail-open or fail-closed.
 */
public final class HibpClient {

    private static final String API_URL = "https://api.pwnedpasswords.com/range/";
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    // Shared client — HttpClient is thread-safe and reusable
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .build();

    private HibpClient() {}

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Result of a HIBP Pwned Passwords check.
     *
     * @param pwned  true if the password appeared in at least one known breach
     * @param count  number of times the password appeared (0 if not found)
     * @param error  true if the check could not be completed (network/API error)
     */
    public record PwnResult(boolean pwned, int count, boolean error) {
        /** Convenience: was check completed successfully and password is safe? */
        public boolean isSafe() { return !error && !pwned; }
    }

    /**
     * Performs the k-anonymity range lookup for the given plaintext password.
     * This method is <em>blocking</em> — call it from a background thread.
     *
     * @param password the plaintext password to check
     * @return a {@link PwnResult}; never null
     */
    public static PwnResult check(String password) {
        try {
            String sha1 = sha1Hex(password);
            String prefix = sha1.substring(0, 5);   // sent to API
            String suffix = sha1.substring(5);       // compared locally

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + prefix))
                    .timeout(TIMEOUT)
                    .header("User-Agent", "TeamHub-Desktop/1.0")
                    .header("Add-Padding", "true")   // HIBP padding header hides traffic analysis
                    .GET()
                    .build();

            HttpResponse<String> response =
                    HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() != 200) {
                return new PwnResult(false, 0, true);
            }

            int breachCount = parseCount(response.body(), suffix);
            return new PwnResult(breachCount > 0, breachCount, false);

        } catch (Exception e) {
            // Network timeout, DNS failure, etc. — fail-open
            return new PwnResult(false, 0, true);
        }
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    /**
     * Returns the uppercase SHA-1 hex digest of the UTF-8 encoded password.
     */
    private static String sha1Hex(String input) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder(40);
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    /**
     * Parses the HIBP range response body and returns the breach count for
     * the given suffix, or 0 if the suffix is not present.
     *
     * Response format (one entry per line):
     * {@code SUFFIX_HEX:COUNT}
     * e.g. {@code 003D68EB55068C33ACE09247EE4C639306:3}
     */
    private static int parseCount(String body, String suffix) {
        String upperSuffix = suffix.toUpperCase(Locale.ROOT);
        for (String line : body.split("\r?\n")) {
            line = line.trim();
            if (line.isEmpty()) continue;
            int colon = line.indexOf(':');
            if (colon < 0) continue;
            String lineSuffix = line.substring(0, colon).toUpperCase(Locale.ROOT);
            if (lineSuffix.equals(upperSuffix)) {
                try {
                    // Strip any padding zeros HIBP adds when "Add-Padding: true"
                    String countStr = line.substring(colon + 1).trim();
                    return Integer.parseInt(countStr);
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
        }
        return 0;
    }
}
