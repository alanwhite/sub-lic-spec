package com.licenseserver.client;

import com.licenseserver.client.ui.MainWindow;
import javax.swing.SwingUtilities;

/**
 * License Client - Main Entry Point
 * macOS Java application for subscription licensing with certificate authentication
 */
public class Main {
    public static void main(String[] args) {
        // Set macOS-specific look and feel
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "License Client");

        // Launch UI on Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            try {
                MainWindow window = new MainWindow();
                window.setVisible(true);
            } catch (Exception e) {
                System.err.println("Failed to start application: " + e.getMessage());
                e.printStackTrace();
                System.exit(1);
            }
        });
    }
}