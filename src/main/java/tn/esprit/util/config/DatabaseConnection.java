package tn.esprit.util.config;

import tn.esprit.util.exception.DataAccessException;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class   DatabaseConnection {

    private static final String PROPERTIES_FILE = "db.properties";
    private static final Properties PROPERTIES = loadProperties();

    private DatabaseConnection() {
    }

    /**
     * Opens a new JDBC connection using values from db.properties.
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                PROPERTIES.getProperty("db.url"),
                PROPERTIES.getProperty("db.username"),
                PROPERTIES.getProperty("db.password")
        );
    }

    /**
     * Loads database settings once from the classpath resource file.
     */
    private static Properties loadProperties() {
        try (InputStream inputStream = DatabaseConnection.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
            if (inputStream == null) {
                throw new DataAccessException("Unable to find " + PROPERTIES_FILE + " on the classpath.");
            }

            Properties properties = new Properties();
            properties.load(inputStream);
            return properties;
        } catch (IOException exception) {
            throw new DataAccessException("Unable to load database configuration.", exception);
        }
    }
}
