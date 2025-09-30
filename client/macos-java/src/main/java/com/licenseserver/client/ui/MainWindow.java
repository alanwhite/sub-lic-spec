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

        buttonPanel.add(enrollButton);
        buttonPanel.add(checkLicenseButton);
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
        // TODO: Implement license status check
        statusLabel.setText("Checking license status...");
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                Thread.sleep(1000); // Simulate API call
                return null;
            }

            @Override
            protected void done() {
                statusLabel.setText("✓ License active - Expires in 28 days");
            }
        };
        worker.execute();
    }
}