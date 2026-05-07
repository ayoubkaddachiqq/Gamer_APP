package tn.esprit.demo.db;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class DbConfig {
    private static final String PROPERTIES_FILE = "/db.properties";

    private final String url;
    private final String user;
    private final String password;
    private final int poolSize;

    private DbConfig(String url, String user, String password, int poolSize) {
        this.url = url;
        this.user = user;
        this.password = password;
        this.poolSize = poolSize;
    }

    public static DbConfig load() {
        Properties properties = new Properties();
        try (InputStream input = DbConfig.class.getResourceAsStream(PROPERTIES_FILE)) {
            if (input == null) {
                throw new IllegalStateException("Missing db.properties in resources");
            }
            properties.load(input);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load db.properties", e);
        }

        String url = require(properties, "db.url");
        String user = require(properties, "db.user");
        String password = properties.getProperty("db.password", "");
        int poolSize = Integer.parseInt(properties.getProperty("db.poolSize", "5"));

        return new DbConfig(url, user, password, poolSize);
    }

    private static String require(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing property: " + key);
        }
        return value.trim();
    }

    public String getUrl() {
        return url;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public int getPoolSize() {
        return poolSize;
    }
}
