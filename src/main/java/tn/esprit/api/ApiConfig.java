package tn.esprit.api;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ApiConfig {
    private static final String PROPERTIES_FILE = "api.properties";
    private static ApiConfig instance;
    private Properties properties;

    private ApiConfig() {
        properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
            if (input != null) {
                properties.load(input);
            } else {
                System.err.println("⚠  " + PROPERTIES_FILE + " not found in classpath. API features will use fallback data.");
            }
        } catch (IOException e) {
            System.err.println("⚠  Error loading " + PROPERTIES_FILE + ": " + e.getMessage());
        }
    }

    public static ApiConfig getInstance() {
        if (instance == null) {
            instance = new ApiConfig();
        }
        return instance;
    }

    public String getRawgApiKey() {
        return properties.getProperty("rawg.api.key", "");
    }

    public boolean hasRawgApiKey() {
        return !getRawgApiKey().isEmpty() && !getRawgApiKey().equals("YOUR_RAWG_API_KEY_HERE");
    }
}
