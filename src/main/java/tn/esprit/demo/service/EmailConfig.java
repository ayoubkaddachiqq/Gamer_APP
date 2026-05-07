package tn.esprit.demo.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class EmailConfig {
    private static final String PROPERTIES_FILE = "/email.properties";

    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final String from;

    private EmailConfig(String host, int port, String username, String password, String from) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.from = from;
    }

    public static EmailConfig load() {
        Properties properties = new Properties();
        try (InputStream input = EmailConfig.class.getResourceAsStream(PROPERTIES_FILE)) {
            if (input == null) {
                throw new IllegalStateException("Missing email.properties in resources");
            }
            properties.load(input);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load email.properties", e);
        }

        String host = require(properties, "mail.host");
        int port = Integer.parseInt(require(properties, "mail.port"));
        String username = require(properties, "mail.username");
        String password = require(properties, "mail.password");
        String from = require(properties, "mail.from");

        return new EmailConfig(host, port, username, password, from);
    }

    private static String require(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing property: " + key);
        }
        return value.trim();
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getFrom() {
        return from;
    }
}
