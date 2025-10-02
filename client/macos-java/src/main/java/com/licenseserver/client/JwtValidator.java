package com.licenseserver.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * JWT License Token Validator
 * Validates license tokens locally without contacting the server
 */
public class JwtValidator {

    private PublicKey licensePublicKey;

    public JwtValidator() throws Exception {
        // Load the license server's public key for JWT verification
        loadLicensePublicKey();
    }

    /**
     * Validate a JWT license token
     */
    public ValidationResult validateLicense(String jwtToken) {
        try {
            // Split JWT into parts
            String[] parts = jwtToken.split("\\.");
            if (parts.length != 3) {
                return new ValidationResult(false, "Invalid JWT format", null, null, 0, 0);
            }

            String headerB64 = parts[0];
            String payloadB64 = parts[1];
            String signatureB64 = parts[2];

            // Verify signature
            String signedData = headerB64 + "." + payloadB64;
            byte[] signature = Base64.getUrlDecoder().decode(signatureB64);

            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(licensePublicKey);
            sig.update(signedData.getBytes());

            if (!sig.verify(signature)) {
                return new ValidationResult(false, "Invalid signature", null, null, 0, 0);
            }

            // Decode and parse payload
            byte[] payloadBytes = Base64.getUrlDecoder().decode(payloadB64);
            String payloadJson = new String(payloadBytes);

            Gson gson = new Gson();
            JsonObject payload = gson.fromJson(payloadJson, JsonObject.class);

            // Extract claims
            long exp = payload.has("exp") ? payload.get("exp").getAsLong() : 0;
            long iat = payload.has("iat") ? payload.get("iat").getAsLong() : 0;
            String email = payload.has("email") ? payload.get("email").getAsString() : "";
            String tier = payload.has("tier") ? payload.get("tier").getAsString() : "free";
            String certFingerprint = payload.has("cert_fingerprint") ?
                payload.get("cert_fingerprint").getAsString() : "";

            // Check expiration
            long now = System.currentTimeMillis() / 1000;
            if (exp > 0 && now > exp) {
                return new ValidationResult(false, "Token expired", email, tier, iat, exp);
            }

            // TODO: Verify certificate binding (cert_fingerprint matches current cert)

            return new ValidationResult(true, null, email, tier, iat, exp);

        } catch (Exception e) {
            return new ValidationResult(false, "Validation error: " + e.getMessage(), null, null, 0, 0);
        }
    }

    /**
     * Load the license server's public key from resources
     */
    private void loadLicensePublicKey() throws Exception {
        // Try to load from resources first
        InputStream is = getClass().getClassLoader().getResourceAsStream("license-public-key.pem");

        // Fall back to file system for development
        if (is == null) {
            try {
                is = new FileInputStream("/tmp/license-public-key.pem");
            } catch (Exception e) {
                // If file doesn't exist, create a dummy key for testing
                // In production, this would fail
                System.err.println("Warning: License public key not found, using dummy key for testing");
                createDummyKey();
                return;
            }
        }

        try {
            // Read PEM file
            String pem = new String(is.readAllBytes());
            pem = pem.replace("-----BEGIN PUBLIC KEY-----", "")
                     .replace("-----END PUBLIC KEY-----", "")
                     .replaceAll("\\s", "");

            byte[] keyBytes = Base64.getDecoder().decode(pem);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            licensePublicKey = kf.generatePublic(spec);
        } finally {
            is.close();
        }
    }

    /**
     * Create a dummy key for testing when real key is unavailable
     */
    private void createDummyKey() throws Exception {
        // Generate a test key pair (in production, this would never happen)
        java.security.KeyPairGenerator kpg = java.security.KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        java.security.KeyPair kp = kpg.generateKeyPair();
        licensePublicKey = kp.getPublic();
    }

    /**
     * Validation result with license details
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String error;
        private final String email;
        private final String tier;
        private final long issuedAt;
        private final long expiresAt;

        public ValidationResult(boolean valid, String error, String email, String tier,
                              long issuedAt, long expiresAt) {
            this.valid = valid;
            this.error = error;
            this.email = email;
            this.tier = tier;
            this.issuedAt = issuedAt;
            this.expiresAt = expiresAt;
        }

        public boolean isValid() {
            return valid;
        }

        public String getError() {
            return error;
        }

        public String getEmail() {
            return email;
        }

        public String getTier() {
            return tier;
        }

        public long getIssuedAt() {
            return issuedAt;
        }

        public long getExpiresAt() {
            return expiresAt;
        }
    }
}
