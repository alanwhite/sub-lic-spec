package com.licenseserver.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.*;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import javax.net.ssl.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Certificate Service
 * Handles CSR generation and certificate management
 */
public class CertificateService {

    /**
     * Generate a Certificate Signing Request (CSR)
     *
     * @param commonName Common name for the certificate (device name)
     * @param deviceId Hardware device identifier
     * @return CSR in PEM format
     */
    public static CsrResult generateCSR(String commonName, String deviceId) throws Exception {
        // Generate RSA key pair (2048-bit)
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048, new SecureRandom());
        KeyPair keyPair = keyGen.generateKeyPair();

        // Build distinguished name
        String dn = String.format("CN=%s, OU=Device %s", commonName, deviceId);

        // Generate CSR using openssl command (simpler than bouncycastle)
        // Save private key temporarily
        File tempKeyFile = File.createTempFile("private_key_", ".pem");
        File tempCsrFile = File.createTempFile("csr_", ".pem");
        tempKeyFile.deleteOnExit();
        tempCsrFile.deleteOnExit();

        // Write private key in PKCS8 format
        String privateKeyPem = convertPrivateKeyToPEM(keyPair.getPrivate());
        try (FileWriter fw = new FileWriter(tempKeyFile)) {
            fw.write(privateKeyPem);
        }

        // Generate CSR using openssl
        ProcessBuilder pb = new ProcessBuilder(
            "openssl", "req", "-new",
            "-key", tempKeyFile.getAbsolutePath(),
            "-out", tempCsrFile.getAbsolutePath(),
            "-subj", "/" + dn.replace(", ", "/")
        );
        Process process = pb.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            BufferedReader errorReader = new BufferedReader(
                new InputStreamReader(process.getErrorStream())
            );
            StringBuilder errorOutput = new StringBuilder();
            String line;
            while ((line = errorReader.readLine()) != null) {
                errorOutput.append(line).append("\n");
            }
            throw new Exception("Failed to generate CSR: " + errorOutput.toString());
        }

        // Read CSR
        StringBuilder csrBuilder = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(tempCsrFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                csrBuilder.append(line).append("\n");
            }
        }

        return new CsrResult(csrBuilder.toString(), privateKeyPem);
    }

    /**
     * Convert private key to PEM format
     */
    private static String convertPrivateKeyToPEM(PrivateKey privateKey) {
        StringBuilder pem = new StringBuilder();
        pem.append("-----BEGIN PRIVATE KEY-----\n");

        String base64 = Base64.getEncoder().encodeToString(privateKey.getEncoded());
        // Split into 64-character lines
        for (int i = 0; i < base64.length(); i += 64) {
            pem.append(base64.substring(i, Math.min(i + 64, base64.length())));
            pem.append("\n");
        }

        pem.append("-----END PRIVATE KEY-----\n");
        return pem.toString();
    }

    /**
     * Result of CSR generation
     */
    public static class CsrResult {
        public final String csr;
        public final String privateKey;

        public CsrResult(String csr, String privateKey) {
            this.csr = csr;
            this.privateKey = privateKey;
        }
    }

    /**
     * Enroll device with server
     *
     * @param token Enrollment token
     * @param deviceName Device name
     * @param deviceId Hardware device ID
     * @param platform Platform (macos)
     * @return Enrollment result with certificate and license token
     */
    public static EnrollmentResult enroll(
        String token,
        String deviceName,
        String deviceId,
        String platform
    ) throws Exception {

        // 1. Generate CSR
        CsrResult csrResult = generateCSR(deviceName, deviceId);

        // 2. Build enrollment request
        String serverUrl = AppConfig.getLicenseServerUrl();
        String enrollmentUrl = serverUrl + "/api/v1/certificate/enroll";

        String jsonBody = String.format(
            "{\"token\":\"%s\",\"csr\":\"%s\",\"device_name\":\"%s\",\"device_id\":\"%s\",\"platform\":\"%s\"}",
            token,
            escapeJson(csrResult.csr),
            escapeJson(deviceName),
            deviceId,
            platform
        );

        // 3. Create HTTPS client (accepting self-signed certs for development)
        HttpClient client = createTrustAllClient();

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(enrollmentUrl))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build();

        // 4. Send request
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new Exception("Enrollment failed: HTTP " + response.statusCode() + " - " + response.body());
        }

        // 5. Parse response using Gson
        String responseBody = response.body();
        System.out.println("Response body length: " + responseBody.length());

        Gson gson = new Gson();
        JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);

        String certificate = jsonResponse.get("certificate").getAsString();
        String licenseToken = jsonResponse.get("license_token").getAsString();
        String caChain = jsonResponse.has("ca_chain") ? jsonResponse.get("ca_chain").getAsString() : null;
        String expiresAt = jsonResponse.get("expires_at").getAsString();

        System.out.println("Parsed certificate: YES (" + certificate.length() + " chars)");
        System.out.println("Parsed license_token: YES (" + licenseToken.length() + " chars)");

        return new EnrollmentResult(
            certificate,
            csrResult.privateKey,
            licenseToken,
            caChain,
            expiresAt
        );
    }

    /**
     * Create HTTP client that trusts all certificates (for development)
     */
    private static HttpClient createTrustAllClient() throws Exception {
        TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
                public void checkClientTrusted(
                    java.security.cert.X509Certificate[] certs, String authType) {
                }
                public void checkServerTrusted(
                    java.security.cert.X509Certificate[] certs, String authType) {
                }
            }
        };

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustAllCerts, new SecureRandom());

        return HttpClient.newBuilder()
            .sslContext(sslContext)
            .build();
    }

    /**
     * Simple JSON value extraction
     */
    private static String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) return null;

        int colonIndex = json.indexOf(":", keyIndex);
        if (colonIndex == -1) return null;

        int quoteStart = json.indexOf("\"", colonIndex);
        if (quoteStart == -1) return null;

        int quoteEnd = quoteStart + 1;
        while (quoteEnd < json.length()) {
            if (json.charAt(quoteEnd) == '"') {
                // Check if it's escaped by counting preceding backslashes
                int backslashCount = 0;
                int pos = quoteEnd - 1;
                while (pos >= quoteStart && json.charAt(pos) == '\\') {
                    backslashCount++;
                    pos--;
                }
                // If even number of backslashes (including 0), quote is not escaped
                if (backslashCount % 2 == 0) {
                    break;
                }
            }
            quoteEnd++;
        }

        String value = json.substring(quoteStart + 1, quoteEnd);
        return unescapeJson(value);
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
     * Unescape JSON string
     */
    private static String unescapeJson(String str) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '\\' && i + 1 < str.length()) {
                char next = str.charAt(i + 1);
                switch (next) {
                    case 'n': result.append('\n'); i++; break;
                    case 'r': result.append('\r'); i++; break;
                    case 't': result.append('\t'); i++; break;
                    case '"': result.append('"'); i++; break;
                    case '\\': result.append('\\'); i++; break;
                    default: result.append(c); break;
                }
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    /**
     * Result of enrollment
     */
    public static class EnrollmentResult {
        public final String certificate;
        public final String privateKey;
        public final String licenseToken;
        public final String caChain;
        public final String expiresAt;

        public EnrollmentResult(
            String certificate,
            String privateKey,
            String licenseToken,
            String caChain,
            String expiresAt
        ) {
            this.certificate = certificate;
            this.privateKey = privateKey;
            this.licenseToken = licenseToken;
            this.caChain = caChain;
            this.expiresAt = expiresAt;
        }
    }
}
