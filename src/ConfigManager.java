package src;

import java.io.*;
import java.util.Properties;

/**
 * 
 * A GUI-based client application for
 * interacting with a marketplace server.
 *
 * @author samridhi
 * @version 07/05/2025
 */

public class ConfigManager {
    private static final String CONFIG_FILE = "marketplace_config.properties";
    private static Properties properties;

    static {
        properties = new Properties();
        loadConfig();
    }

    private static void loadConfig() {
        try (FileInputStream fis = new FileInputStream(CONFIG_FILE)) {
            properties.load(fis);
        } catch (IOException e) {
            saveConfig();
        }
    }

    private static void saveConfig() {
        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
            properties.store(fos, "Marketplace Configuration");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void setRememberedUsername(String username) {
        properties.setProperty("remembered_username", username);
        saveConfig();
    }

    public static String getRememberedUsername() {
        return properties.getProperty("remembered_username", "");
    }

    public static void setSessionToken(String token) {
        properties.setProperty("session_token", token);
        saveConfig();
    }

    public static String getSessionToken() {
        return properties.getProperty("session_token", "");
    }

    public static void setStayLoggedIn(boolean stayLoggedIn) {
        properties.setProperty("stay_logged_in", String.valueOf(stayLoggedIn));
        saveConfig();
    }

    public static boolean getStayLoggedIn() {
        return Boolean.parseBoolean(properties.getProperty("stay_logged_in", "false"));
    }
} 
