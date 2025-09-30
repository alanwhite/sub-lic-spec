package com.licenseserver.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * Device Identifier
 * Generates stable, hardware-based device identifiers for macOS
 */
public class DeviceIdentifier {

    /**
     * Get unique device identifier for macOS
     * Uses hardware UUID from IOPlatformUUID
     */
    public static String getDeviceId() throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
            "ioreg",
            "-d2",
            "-c",
            "IOPlatformExpertDevice"
        );
        Process process = pb.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

        String line;
        while ((line = reader.readLine()) != null) {
            if (line.contains("IOPlatformUUID")) {
                // Extract UUID from line like: "IOPlatformUUID" = "XXXX-XXXX-XXXX-XXXX"
                String[] parts = line.split("\"");
                if (parts.length >= 4) {
                    String uuid = parts[3];
                    process.waitFor();
                    return hashDeviceId(uuid);
                }
            }
        }

        process.waitFor();
        throw new Exception("Failed to retrieve device UUID");
    }

    /**
     * Hash the device ID to create a consistent identifier
     */
    private static String hashDeviceId(String uuid) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(uuid.getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(hash);
    }

    /**
     * Get platform identifier
     */
    public static String getPlatform() {
        return "macos";
    }

    /**
     * Get device name (computer name)
     */
    public static String getDeviceName() {
        try {
            ProcessBuilder pb = new ProcessBuilder("scutil", "--get", "ComputerName");
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String name = reader.readLine();
            process.waitFor();
            return name != null ? name.trim() : "Unknown Mac";
        } catch (Exception e) {
            return "Unknown Mac";
        }
    }
}