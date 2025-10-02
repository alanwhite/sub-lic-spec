package com.licenseserver.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.*;
import java.security.*;
import javax.net.ssl.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.cert.CertPathValidator;
import java.security.cert.PKIXParameters;
import java.security.cert.PKIXRevocationChecker;
import java.security.cert.X509Certificate;

/**
 * License Service
 * Handles license verification using mTLS with production-grade security
 */
public class LicenseService {

    /**
     * Verify license token with server using mTLS
     *
     * @param licenseToken JWT license token
     * @param p12File Path to PKCS12 file with client certificate
     * @param p12Password Password for PKCS12 file
     * @return Verification result
     */
    public static VerificationResult verifyLicense(
        String licenseToken,
        String p12File,
        String p12Password
    ) throws Exception {

        // Load configuration
        LicenseConfig config = new LicenseConfig();

        // Enable SSL debug logging only in development
        if (config.isDebugLogging()) {
            System.setProperty("javax.net.debug", "ssl:handshake");
        }

        // Load PKCS12 keystore with client certificate
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream(p12File)) {
            keyStore.load(fis, p12Password.toCharArray());
        }

        // Create KeyManagerFactory with the keystore
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, p12Password.toCharArray());

        // Load server CA certificate chain for trust
        KeyStore trustStore = KeyStore.getInstance("JKS");
        trustStore.load(null, null); // Initialize empty truststore

        // Load CA certificate chain from configured path
        java.security.cert.CertificateFactory cf = java.security.cert.CertificateFactory.getInstance("X.509");
        try (FileInputStream fis = new FileInputStream(config.getCaCertPath())) {
            java.util.Collection<? extends java.security.cert.Certificate> certs = cf.generateCertificates(fis);
            int i = 0;
            for (java.security.cert.Certificate cert : certs) {
                trustStore.setCertificateEntry("ca-" + i++, cert);
            }
        }

        // Only load self-signed server certificate in development mode
        if (config.allowSelfSignedCerts()) {
            try (FileInputStream fis = new FileInputStream(config.getDevServerCertPath())) {
                java.security.cert.Certificate serverCert = cf.generateCertificate(fis);
                trustStore.setCertificateEntry("server", serverCert);
            }
        }

        // Create TrustManagerFactory with the CA certificates
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);

        // Enable certificate revocation checking in production
        TrustManager[] trustManagers = tmf.getTrustManagers();
        if (config.isCertificateRevocationEnabled() && trustManagers.length > 0 && trustManagers[0] instanceof X509TrustManager) {
            trustManagers = new TrustManager[] { createRevocationCheckingTrustManager((X509TrustManager) trustManagers[0], trustStore) };
        }

        // Create SSLContext with TLS 1.3 only
        SSLContext sslContext = SSLContext.getInstance("TLSv1.3");
        sslContext.init(kmf.getKeyManagers(), trustManagers, new SecureRandom());

        // Configure SSL parameters to enforce TLS 1.3 and hostname verification
        SSLParameters sslParameters = sslContext.getDefaultSSLParameters();
        sslParameters.setProtocols(new String[]{"TLSv1.3"});

        // Enable endpoint identification (hostname verification) in production
        if (config.isHostnameVerificationEnabled()) {
            sslParameters.setEndpointIdentificationAlgorithm("HTTPS");
        }

        // Create HTTP client with mTLS and TLS 1.3 enforcement
        HttpClient client = HttpClient.newBuilder()
            .sslContext(sslContext)
            .sslParameters(sslParameters)
            .build();

        // Build verification request using configured URL
        String verifyUrl = config.getServerUrl() + "/api/license/verify";

        String jsonBody = String.format(
            "{\"license_token\":\"%s\"}",
            escapeJson(licenseToken)
        );

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(verifyUrl))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build();

        System.out.println("Sending mTLS request to: " + verifyUrl);

        // Send request
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new Exception("License verification failed: HTTP " + response.statusCode() + " - " + response.body());
        }

        // Parse response
        Gson gson = new Gson();
        JsonObject jsonResponse = gson.fromJson(response.body(), JsonObject.class);

        boolean valid = jsonResponse.get("valid").getAsBoolean();
        String message = jsonResponse.has("message") ? jsonResponse.get("message").getAsString() : "";
        String clientDN = jsonResponse.has("client_dn") ? jsonResponse.get("client_dn").getAsString() : "";
        String returnedLicenseToken = jsonResponse.has("license_token") ? jsonResponse.get("license_token").getAsString() : null;
        String tier = jsonResponse.has("tier") ? jsonResponse.get("tier").getAsString() : null;
        int expiresInDays = jsonResponse.has("expires_in_days") ? jsonResponse.get("expires_in_days").getAsInt() : 0;

        return new VerificationResult(valid, message, clientDN, returnedLicenseToken, tier, expiresInDays);
    }

    /**
     * Escape string for JSON
     */
    private static String escapeJson(String str) {
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

    /**
     * Create a TrustManager with certificate revocation checking enabled
     */
    private static X509TrustManager createRevocationCheckingTrustManager(
        X509TrustManager baseTrustManager,
        KeyStore trustStore
    ) throws Exception {

        return new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType)
                throws java.security.cert.CertificateException {
                baseTrustManager.checkClientTrusted(chain, authType);
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType)
                throws java.security.cert.CertificateException {

                // First do standard validation
                baseTrustManager.checkServerTrusted(chain, authType);

                // Then check for revocation via OCSP/CRL
                try {
                    PKIXParameters params = new PKIXParameters(trustStore);

                    // Enable revocation checking
                    params.setRevocationEnabled(true);

                    // Get CertPathValidator and enable OCSP
                    CertPathValidator validator = CertPathValidator.getInstance("PKIX");
                    PKIXRevocationChecker revocationChecker = (PKIXRevocationChecker) validator.getRevocationChecker();

                    // Configure OCSP with fallback to CRL
                    revocationChecker.setOptions(java.util.EnumSet.of(
                        PKIXRevocationChecker.Option.PREFER_CRLS,
                        PKIXRevocationChecker.Option.SOFT_FAIL  // Don't fail if OCSP/CRL unavailable
                    ));

                    params.addCertPathChecker(revocationChecker);

                    // Build certificate path
                    java.security.cert.CertificateFactory cf = java.security.cert.CertificateFactory.getInstance("X.509");
                    java.security.cert.CertPath certPath = cf.generateCertPath(java.util.Arrays.asList(chain));

                    // Validate with revocation checking
                    validator.validate(certPath, params);

                } catch (Exception e) {
                    throw new java.security.cert.CertificateException("Certificate revocation check failed", e);
                }
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return baseTrustManager.getAcceptedIssuers();
            }
        };
    }

    /**
     * Result of license verification
     */
    public static class VerificationResult {
        public final boolean valid;
        public final String message;
        public final String clientDN;
        public final String licenseToken;
        public final String tier;
        public final int expiresInDays;

        public VerificationResult(boolean valid, String message, String clientDN, String licenseToken, String tier, int expiresInDays) {
            this.valid = valid;
            this.message = message;
            this.clientDN = clientDN;
            this.licenseToken = licenseToken;
            this.tier = tier;
            this.expiresInDays = expiresInDays;
        }
    }
}
