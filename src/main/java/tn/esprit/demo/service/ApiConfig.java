package tn.esprit.demo.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads API credentials from /api.properties on the classpath.
 */
public class ApiConfig {

    private static final String FILE = "/api.properties";

    private final String rawgApiKey;
    private final String rawgBaseUrl;
    private final String imgbbApiKey;
    private final String lightxApiKey;
    private final String riotApiKey;

    private ApiConfig(String rawgApiKey, String rawgBaseUrl,
                      String imgbbApiKey, String lightxApiKey,
                      String riotApiKey) {
        this.rawgApiKey   = rawgApiKey;
        this.rawgBaseUrl  = rawgBaseUrl;
        this.imgbbApiKey  = imgbbApiKey;
        this.lightxApiKey = lightxApiKey;
        this.riotApiKey   = riotApiKey;
    }

    public static ApiConfig load() {
        Properties p = new Properties();
        try (InputStream is = ApiConfig.class.getResourceAsStream(FILE)) {
            if (is == null) throw new IllegalStateException("Missing api.properties in classpath");
            p.load(is);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load api.properties", e);
        }
        String rawgKey   = p.getProperty("rawg.api.key",   "").trim();
        String rawgUrl   = p.getProperty("rawg.base.url",  "https://api.rawg.io/api").trim();
        String imgbbKey  = p.getProperty("imgbb.api.key",  "").trim();
        String lightxKey = p.getProperty("lightx.api.key", "").trim();
        String riotKey   = p.getProperty("riot.api.key",   "").trim();
        if (rawgKey.isEmpty()) throw new IllegalStateException("rawg.api.key is not set in api.properties");
        return new ApiConfig(rawgKey, rawgUrl, imgbbKey, lightxKey, riotKey);
    }

    public String getRawgApiKey()   { return rawgApiKey; }
    public String getRawgBaseUrl()  { return rawgBaseUrl; }
    public String getImgbbApiKey()  { return imgbbApiKey; }
    public String getLightxApiKey() { return lightxApiKey; }
    public String getRiotApiKey()   { return riotApiKey; }
}
