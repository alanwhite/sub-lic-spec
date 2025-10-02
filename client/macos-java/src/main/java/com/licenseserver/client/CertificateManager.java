package com.licenseserver.client;

import java.io.*;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

/**
 * Certificate Manager
 * Manages client certificates in macOS Keychain
 */
public class CertificateManager {
    private static final String KEYCHAIN_SERVICE = "LicenseClient";

    /**
     * Check if client certificate exists in macOS Keychain
     */
    public boolean hasCertificate() {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "security",
                "find-identity",
                "-v",
                "-p",
                "codesigning"
            );
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("License Client Certificate")) {
                    return true;
                }
            }
            process.waitFor();
            return false;
        } catch (Exception e) {
            System.err.println("Error checking for certificate: " + e.getMessage());
            return false;
        }
    }

    /**
     * Install certificate and private key into macOS Keychain
     * Uses native Java APIs to parse PEM and create PKCS12
     */
    public void installCertificate(String certificatePem, String privateKeyPem) throws Exception {
        File p12File = new File("/tmp/license-client.p12");
        // Don't delete automatically for debugging
        //p12File.deleteOnExit();

        try {
            System.out.println("Parsing certificate and private key using Java native APIs");
            System.out.println("Certificate PEM (first 200 chars): " + certificatePem.substring(0, Math.min(200, certificatePem.length())));
            System.out.println("Certificate contains actual newlines: " + certificatePem.contains("\n"));
            System.out.println("Certificate contains escaped newlines: " + certificatePem.contains("\\n"));
            System.out.println("Certificate contains backslashes: " + certificatePem.contains("\\"));

            // Parse certificate using Java CertificateFactory
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            ByteArrayInputStream certStream = new ByteArrayInputStream(certificatePem.getBytes());
            Certificate certificate = certFactory.generateCertificate(certStream);
            System.out.println("Certificate parsed successfully: " + certificate.getType());

            // Parse private key from PKCS8 PEM format
            PrivateKey privateKey = parsePrivateKey(privateKeyPem);
            System.out.println("Private key parsed successfully: " + privateKey.getAlgorithm());

            // Create PKCS12 keystore with a temporary password
            // Use legacy encryption for macOS compatibility
            // Set system properties for PKCS12 KeyStore to use legacy encryption
            System.setProperty("keystore.pkcs12.legacy", "true");
            System.setProperty("keystore.pkcs12.certProtectionAlgorithm", "PBEWithSHA1AndDESede");
            System.setProperty("keystore.pkcs12.keyProtectionAlgorithm", "PBEWithSHA1AndDESede");

            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            char[] password = "temp".toCharArray();
            keyStore.load(null, null); // Initialize empty keystore

            // Add certificate and private key to keystore
            Certificate[] certChain = new Certificate[]{certificate};
            keyStore.setKeyEntry("License Client Certificate", privateKey, password, certChain);
            System.out.println("PKCS12 keystore created successfully");

            // Write keystore to file with legacy encryption
            try (FileOutputStream fos = new FileOutputStream(p12File)) {
                keyStore.store(fos, password);
            }
            System.out.println("PKCS12 file written: " + p12File.getAbsolutePath());

            // Import to macOS Keychain
            String keychainPath = System.getProperty("user.home") + "/Library/Keychains/login.keychain-db";
            ProcessBuilder pb = new ProcessBuilder(
                "security", "import",
                p12File.getAbsolutePath(),
                "-k", keychainPath,
                "-T", "/usr/bin/codesign",
                "-T", "/usr/bin/security",
                "-P", "temp"
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Capture output
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
            )) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new Exception("Failed to import certificate to Keychain: " + output.toString());
            }

            System.out.println("Certificate successfully installed in macOS Keychain");

        } finally {
            // Don't clean up for debugging
            // p12File.delete();
        }
    }

    /**
     * Parse PKCS8 PEM private key
     */
    private PrivateKey parsePrivateKey(String privateKeyPem) throws Exception {
        // Remove PEM headers/footers and decode Base64
        String privateKeyContent = privateKeyPem
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replaceAll("\\s", "");

        byte[] keyBytes = Base64.getDecoder().decode(privateKeyContent);

        // Parse as PKCS8
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(keySpec);
    }

    /**
     * Get certificate fingerprint
     */
    public String getCertificateFingerprint() throws Exception {
        // TODO: Implement fingerprint extraction
        // 1. Export certificate from Keychain
        // 2. Calculate SHA-256 fingerprint
        // 3. Return hex string

        throw new UnsupportedOperationException("Certificate fingerprint extraction not yet implemented");
    }

    /**
     * Export certificate for mTLS connections
     */
    public byte[] exportCertificate() throws Exception {
        // TODO: Implement certificate export
        throw new UnsupportedOperationException("Certificate export not yet implemented");
    }

    /**
     * Revoke and remove certificate from Keychain
     */
    public void removeCertificate() throws Exception {
        // TODO: Implement certificate removal
        throw new UnsupportedOperationException("Certificate removal not yet implemented");
    }
}