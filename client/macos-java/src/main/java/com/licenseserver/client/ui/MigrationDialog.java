package com.licenseserver.client.ui;

import javax.swing.*;
import java.awt.*;

/**
 * Migration Dialog
 * Allows user to migrate license to a new device
 */
public class MigrationDialog extends JDialog {
    private JButton generateTokenButton;
    private JTextField migrationTokenField;
    private JLabel statusLabel;

    public MigrationDialog(JFrame parent) {
        super(parent, "Migrate License", true);
        initializeUI();
    }

    private void initializeUI() {
        setSize(500, 300);
        setLocationRelativeTo(getParent());

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Instructions
        JLabel instructionLabel = new JLabel(
            "<html><b>Step 1:</b> Generate a migration token on this device<br>" +
            "<b>Step 2:</b> Install the app on your new device<br>" +
            "<b>Step 3:</b> Use the migration token on the new device</html>"
        );
        instructionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainPanel.add(instructionLabel);
        mainPanel.add(Box.createVerticalStrut(20));

        // Generate token button
        generateTokenButton = new JButton("Generate Migration Token");
        generateTokenButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        generateTokenButton.addActionListener(e -> generateMigrationToken());
        mainPanel.add(generateTokenButton);
        mainPanel.add(Box.createVerticalStrut(15));

        // Migration token display
        JPanel tokenPanel = new JPanel(new BorderLayout(5, 5));
        tokenPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        tokenPanel.add(new JLabel("Migration Token:"), BorderLayout.WEST);
        migrationTokenField = new JTextField();
        migrationTokenField.setEditable(false);
        tokenPanel.add(migrationTokenField, BorderLayout.CENTER);
        JButton copyButton = new JButton("Copy");
        copyButton.addActionListener(e -> copyTokenToClipboard());
        tokenPanel.add(copyButton, BorderLayout.EAST);
        mainPanel.add(tokenPanel);
        mainPanel.add(Box.createVerticalStrut(10));

        // Status label
        statusLabel = new JLabel(" ");
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainPanel.add(statusLabel);
        mainPanel.add(Box.createVerticalStrut(15));

        // Note
        JLabel noteLabel = new JLabel(
            "<html><i>Note: Migration tokens are valid for 24 hours and can only be used once.</i></html>"
        );
        noteLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        noteLabel.setForeground(Color.GRAY);
        mainPanel.add(noteLabel);

        // Close button
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());
        buttonPanel.add(closeButton);
        mainPanel.add(Box.createVerticalGlue());
        mainPanel.add(buttonPanel);

        setContentPane(mainPanel);
    }

    private void generateMigrationToken() {
        generateTokenButton.setEnabled(false);
        statusLabel.setText("Generating migration token...");

        // TODO: Implement actual migration token generation
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() throws Exception {
                // 1. Connect via mTLS
                // 2. Request migration token
                // 3. Return token
                Thread.sleep(1000); // Simulate API call
                return "MIGRATE-" + System.currentTimeMillis();
            }

            @Override
            protected void done() {
                try {
                    String token = get();
                    migrationTokenField.setText(token);
                    statusLabel.setText("✓ Migration token generated successfully");
                    statusLabel.setForeground(Color.GREEN);
                } catch (Exception e) {
                    statusLabel.setText("✗ Failed to generate migration token");
                    statusLabel.setForeground(Color.RED);
                    generateTokenButton.setEnabled(true);
                }
            }
        };
        worker.execute();
    }

    private void copyTokenToClipboard() {
        String token = migrationTokenField.getText();
        if (!token.isEmpty()) {
            Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new java.awt.datatransfer.StringSelection(token), null);
            statusLabel.setText("✓ Token copied to clipboard");
            statusLabel.setForeground(Color.BLUE);
        }
    }
}