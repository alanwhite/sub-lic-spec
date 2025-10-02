package com.licenseserver.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * License Client Configuration
 * Supports both development and production environments
 */
public class LicenseConfig {

    private final Properties props;
    private final boolean isDevelopment;

    public LicenseConfig() throws IOException {
        props = new Properties();

        // Try to load config file, fall back to defaults
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("license-client.properties")) {
            if (is != null) {
                props.load(is);
            }
        }

        // Check if running in development mode
        isDevelopment = "development".equalsIgnoreCase(
            props.getProperty("environment", "production")
        );
    }

    public boolean isDevelopment() {
        return isDevelopment;
    }

    public String getServerUrl() {
        return props.getProperty("server.url", "https://license-server.example.com:9443");
    }

    public String getServerHostname() {
        return props.getProperty("server.hostname", "license-server.example.com");
    }

    public boolean isDebugLogging() {
        // Only enable debug logging in development
        return isDevelopment && "true".equalsIgnoreCase(
            props.getProperty("debug.ssl", "false")
        );
    }

    public boolean isCertificateRevocationEnabled() {
        // Enable OCSP/CRL checking in production by default
        return "true".equalsIgnoreCase(
            props.getProperty("security.revocation.enabled", isDevelopment ? "false" : "true")
        );
    }

    public boolean isHostnameVerificationEnabled() {
        // Always enable in production, optional in dev
        return "true".equalsIgnoreCase(
            props.getProperty("security.hostname.verification", isDevelopment ? "false" : "true")
        );
    }

    public boolean allowSelfSignedCerts() {
        // Only in development
        return isDevelopment && "true".equalsIgnoreCase(
            props.getProperty("dev.allow.selfsigned", "false")
        );
    }

    public String getCaCertPath() {
        return props.getProperty("security.ca.cert.path", "/tmp/ca-chain.crt");
    }

    public String getDevServerCertPath() {
        // Only used in development when allowSelfSignedCerts is true
        return props.getProperty("dev.server.cert.path", "/tmp/server.crt");
    }
}
