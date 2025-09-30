package com.licenseserver.client.ui;

import com.licenseserver.client.DeviceIdentifier;
import javax.swing.*;
import java.awt.*;

/**
 * Enrollment Dialog
 * Allows user to enroll device using enrollment token
 */
public class EnrollmentDialog extends JDialog {
    private JTextField tokenField;
    private JTextField deviceNameField;
    private JButton enrollButton;
    private JLabel statusLabel;

    public EnrollmentDialog(JFrame parent) {
        super(parent, "Enroll Device", true);
        initializeUI();
    }

    private void initializeUI() {
        setSize(500, 300);
        setLocationRelativeTo(getParent());

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Instructions
        JLabel instructionLabel = new JLabel("<html>Enter the enrollment token from your account portal:</html>");
        instructionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainPanel.add(instructionLabel);
        mainPanel.add(Box.createVerticalStrut(15));

        // Token input
        JPanel tokenPanel = new JPanel(new BorderLayout(5, 5));
        tokenPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        tokenPanel.add(new JLabel("Enrollment Token:"), BorderLayout.WEST);
        tokenField = new JTextField();
        tokenPanel.add(tokenField, BorderLayout.CENTER);
        mainPanel.add(tokenPanel);
        mainPanel.add(Box.createVerticalStrut(10));

        // Device name input
        JPanel devicePanel = new JPanel(new BorderLayout(5, 5));
        devicePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        devicePanel.add(new JLabel("Device Name:"), BorderLayout.WEST);
        deviceNameField = new JTextField(DeviceIdentifier.getDeviceName());
        devicePanel.add(deviceNameField, BorderLayout.CENTER);
        mainPanel.add(devicePanel);
        mainPanel.add(Box.createVerticalStrut(10));

        // Status label
        statusLabel = new JLabel(" ");
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusLabel.setForeground(Color.RED);
        mainPanel.add(statusLabel);
        mainPanel.add(Box.createVerticalStrut(10));

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        enrollButton = new JButton("Enroll");
        enrollButton.addActionListener(e -> performEnrollment());

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dispose());

        buttonPanel.add(cancelButton);
        buttonPanel.add(enrollButton);
        mainPanel.add(buttonPanel);

        setContentPane(mainPanel);
    }

    private void performEnrollment() {
        String token = tokenField.getText().trim();
        String deviceName = deviceNameField.getText().trim();

        if (token.isEmpty()) {
            statusLabel.setText("Please enter an enrollment token");
            return;
        }

        if (deviceName.isEmpty()) {
            statusLabel.setText("Please enter a device name");
            return;
        }

        enrollButton.setEnabled(false);
        statusLabel.setText("Enrolling...");
        statusLabel.setForeground(Color.BLUE);

        // TODO: Implement actual enrollment
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                // 1. Generate CSR
                // 2. Send enrollment request with token, CSR, device name, platform
                // 3. Receive certificate and license token
                // 4. Install certificate in Keychain
                // 5. Store license token encrypted
                Thread.sleep(2000); // Simulate enrollment
                return null;
            }

            @Override
            protected void done() {
                statusLabel.setText("✓ Enrollment successful!");
                statusLabel.setForeground(Color.GREEN);
                Timer timer = new Timer(1500, e -> dispose());
                timer.setRepeats(false);
                timer.start();
            }
        };
        worker.execute();
    }
}