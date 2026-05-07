package tn.esprit.demo.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Persists the admin-configured avatar folder path in a writable file
 * next to the running jar (or in the working directory during development).
 *
 * The file is intentionally kept OUTSIDE the classpath so it survives
 * recompilation and is writable at runtime.
 */
public class AvatarFolderConfig {

    private static final String CONFIG_FILE = "teamhub-app.properties";
    private static final String KEY = "avatar.folder.path";

    private static final File configFile = new File(CONFIG_FILE);

    /** Returns the admin-configured folder, or null if not set. */
    public static String getFolder() {
        if (!configFile.exists()) return null;
        Properties p = new Properties();
        try (FileInputStream fis = new FileInputStream(configFile)) {
            p.load(fis);
        } catch (IOException e) {
            return null;
        }
        String val = p.getProperty(KEY, "").trim();
        return val.isEmpty() ? null : val;
    }

    /** Persists the given folder path. Pass null or blank to clear. */
    public static void setFolder(String folderPath) {
        Properties p = new Properties();
        // load existing properties first so we don't wipe other keys
        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                p.load(fis);
            } catch (IOException ignored) { }
        }
        if (folderPath == null || folderPath.isBlank()) {
            p.remove(KEY);
        } else {
            p.setProperty(KEY, folderPath.trim());
        }
        try (FileOutputStream fos = new FileOutputStream(configFile)) {
            p.store(fos, "TeamHub application settings");
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save avatar folder config", e);
        }
    }

    /**
     * Returns all image files in the configured folder,
     * filtered to common image extensions.
     * Returns an empty array if the folder is not set or invalid.
     */
    public static File[] listAvatarFiles() {
        String folder = getFolder();
        if (folder == null) return new File[0];
        File dir = new File(folder);
        if (!dir.isDirectory()) return new File[0];
        File[] files = dir.listFiles(f -> {
            String name = f.getName().toLowerCase();
            return f.isFile() &&
                   (name.endsWith(".png") || name.endsWith(".jpg") ||
                    name.endsWith(".jpeg") || name.endsWith(".gif") ||
                    name.endsWith(".webp"));
        });
        return files == null ? new File[0] : files;
    }
}
