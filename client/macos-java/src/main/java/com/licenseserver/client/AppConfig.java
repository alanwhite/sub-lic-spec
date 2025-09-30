package com.licenseserver.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Application Configuration
 * Loads configuration from resources and provides access to settings
 */
public class AppConfig {
    private static final Properties properties = new Properties();

    static {
        try (InputStream input = AppConfig.class.getResourceAsStream("/config.properties")) {
            if (input == null) {
                throw new RuntimeException("Unable to find config.properties");
            }
            properties.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load configuration", e);
        }
    }

    public static String getLicenseServerUrl() {
        return properties.getProperty("license.server.url", "https://license-server.local:8443");
    }

    public static String getLicenseServerMtlsUrl() {
        return properties.getProperty("license.server.mtls.url", "https://license-server.local:9443");
    }

    public static String getCaCertPath() {
        return "/ca-chain.pem";
    }

    public static String getLicensePublicKeyPath() {
        return "/license-server.pub";
    }

    public static String getAppVersion() {
        return properties.getProperty("app.version", "1.0.0");
    }

    public static int getLicenseCheckIntervalHours() {
        return Integer.parseInt(properties.getProperty("license.check.interval.hours", "24"));
    }
}