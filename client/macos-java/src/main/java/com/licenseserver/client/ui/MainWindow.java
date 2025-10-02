package com.licenseserver.client.ui;

import com.licenseserver.client.CertificateManager;
import com.licenseserver.client.DeviceIdentifier;
import javax.swing.*;
import java.awt.*;

/**
 * Main Application Window
 * Displays license status and provides access to enrollment and migration
 */
public class MainWindow extends JFrame {
    private CertificateManager certificateManager;
    private JLabel statusLabel;
    private JButton enrollButton;
    private JButton checkLicenseButton;

    public MainWindow() {
        this.certificateManager = new CertificateManager();
        initializeUI();
        checkInitialStatus();
    }

    private void initializeUI() {
        setTitle("License Client");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Main panel
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Status panel
        JPanel statusPanel = new JPanel();
        statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.Y_AXIS));

        statusLabel = new JLabel("Checking license status...");
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        statusPanel.add(statusLabel);

        // Device info
        try {
            String deviceName = DeviceIdentifier.getDeviceName();
            String platform = DeviceIdentifier.getPlatform();
            JLabel deviceLabel = new JLabel("Device: " + deviceName + " (" + platform + ")");
            deviceLabel.setFont(new Font("Arial", Font.PLAIN, 12));
            statusPanel.add(Box.createVerticalStrut(10));
            statusPanel.add(deviceLabel);
        } catch (Exception e) {
            System.err.println("Failed to get device info: " + e.getMessage());
        }

        mainPanel.add(statusPanel, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 0));

        enrollButton = new JButton("Enroll Device");
        enrollButton.addActionListener(e -> showEnrollmentDialog());

        checkLicenseButton = new JButton("Check License");
        checkLicenseButton.addActionListener(e -> checkLicenseStatus());

        JButton migrateButton = new JButton("Migrate License");
        migrateButton.addActionListener(e -> showMigrationDialog());

        JButton verifyButton = new JButton("Verify License (mTLS)");
        verifyButton.addActionListener(e -> verifyLicense());

        buttonPanel.add(enrollButton);
        buttonPanel.add(checkLicenseButton);
        buttonPanel.add(verifyButton);
        buttonPanel.add(migrateButton);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private void checkInitialStatus() {
        if (certificateManager.hasCertificate()) {
            statusLabel.setText("✓ Device enrolled - License active");
            enrollButton.setEnabled(false);
        } else {
            statusLabel.setText("✗ Device not enrolled");
            checkLicenseButton.setEnabled(false);
        }
    }

    private void showEnrollmentDialog() {
        EnrollmentDialog dialog = new EnrollmentDialog(this);
        dialog.setVisible(true);

        // Refresh status after enrollment
        checkInitialStatus();
    }

    private void showMigrationDialog() {
        MigrationDialog dialog = new MigrationDialog(this);
        dialog.setVisible(true);
    }

    private void checkLicenseStatus() {
        statusLabel.setText("Checking license status...");

        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() throws Exception {
                // Load stored license token (in production, this would be from secure storage)
                String licenseToken = loadStoredLicenseToken();

                if (licenseToken == null || licenseToken.isEmpty()) {
                    return "NO_LICENSE";
                }

                // Validate JWT locally
                com.licenseserver.client.JwtValidator validator =
                    new com.licenseserver.client.JwtValidator();
                com.licenseserver.client.JwtValidator.ValidationResult result =
                    validator.validateLicense(licenseToken);

                if (!result.isValid()) {
                    return "INVALID:" + result.getError();
                }

                // Extract license info from JWT claims
                long expiresAt = result.getExpiresAt();
                long now = System.currentTimeMillis() / 1000;
                long daysRemaining = (expiresAt - now) / (24 * 60 * 60);

                String tier = result.getTier();
                String email = result.getEmail();

                return "VALID:" + daysRemaining + ":" + tier + ":" + email;
            }

            @Override
            protected void done() {
                try {
                    String result = get();

                    if (result.equals("NO_LICENSE")) {
                        statusLabel.setText("✗ No license found - Please contact support");
                    } else if (result.startsWith("INVALID:")) {
                        String error = result.substring(8);
                        statusLabel.setText("✗ License invalid: " + error);
                    } else if (result.startsWith("VALID:")) {
                        String[] parts = result.split(":");
                        long days = Long.parseLong(parts[1]);
                        String tier = parts[2];
                        String email = parts[3];

                        String status = String.format(
                            "✓ License active (%s tier) - Expires in %d days - %s",
                            tier, days, email
                        );
                        statusLabel.setText(status);
                    }
                } catch (Exception e) {
                    System.err.println("License check failed:");
                    e.printStackTrace();
                    statusLabel.setText("✗ License check failed: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private String loadStoredLicenseToken() {
        // TODO: Load from secure storage (macOS Keychain, Windows Credential Store, etc.)
        // For now, read from file in /tmp
        try {
            return java.nio.file.Files.readString(
                java.nio.file.Path.of("/tmp/license-token.jwt")
            ).trim();
        } catch (Exception e) {
            System.err.println("Failed to load license token: " + e.getMessage());
            return null;
        }
    }

    private void verifyLicense() {
        statusLabel.setText("Verifying license with mTLS...");

        SwingWorker<com.licenseserver.client.LicenseService.VerificationResult, Void> worker =
            new SwingWorker<com.licenseserver.client.LicenseService.VerificationResult, Void>() {
            @Override
            protected com.licenseserver.client.LicenseService.VerificationResult doInBackground() throws Exception {
                // Use the P12 file created during enrollment
                String p12File = "/tmp/license-client-modern.p12";
                String p12Password = "temp";

                // Use a test license token (in production, this would be stored securely)
                String licenseToken = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.test";

                return com.licenseserver.client.LicenseService.verifyLicense(
                    licenseToken,
                    p12File,
                    p12Password
                );
            }

            @Override
            protected void done() {
                try {
                    com.licenseserver.client.LicenseService.VerificationResult result = get();

                    if (result.valid) {
                        // Store the JWT license token (in production, use secure storage)
                        if (result.licenseToken != null) {
                            storeLicenseToken(result.licenseToken);
                            System.out.println("Stored license token: " + result.licenseToken.substring(0, 50) + "...");
                        }

                        // Enable the Check License button now that we have a license
                        checkLicenseButton.setEnabled(true);

                        String message = String.format(
                            "✓ mTLS SUCCESS: %s\nClient DN: %s\nTier: %s\nExpires in: %d days\nJWT Token received and stored",
                            result.message, result.clientDN, result.tier, result.expiresInDays
                        );

                        statusLabel.setText("<html>✓ License verified and stored (" + result.tier + " tier)</html>");
                        JOptionPane.showMessageDialog(
                            MainWindow.this,
                            message,
                            "License Verification Result",
                            JOptionPane.INFORMATION_MESSAGE
                        );
                    } else {
                        String message = "✗ Verification failed: " + result.message;
                        statusLabel.setText(message);
                        JOptionPane.showMessageDialog(
                            MainWindow.this,
                            message,
                            "Verification Error",
                            JOptionPane.ERROR_MESSAGE
                        );
                    }
                } catch (Exception e) {
                    System.err.println("License verification failed:");
                    e.printStackTrace();
                    Throwable cause = e.getCause();
                    String errorMessage = cause != null ? cause.getMessage() : e.getMessage();
                    statusLabel.setText("✗ Verification failed: " + errorMessage);
                    JOptionPane.showMessageDialog(
                        MainWindow.this,
                        "License verification failed:\n" + errorMessage,
                        "Verification Error",
                        JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        };
        worker.execute();
    }

    private void storeLicenseToken(String token) {
        // TODO: Store in secure storage (macOS Keychain, Windows Credential Store, etc.)
        // For now, write to a file in /tmp
        try {
            java.nio.file.Files.writeString(
                java.nio.file.Path.of("/tmp/license-token.jwt"),
                token
            );
        } catch (Exception e) {
            System.err.println("Failed to store license token: " + e.getMessage());
        }
    }
}