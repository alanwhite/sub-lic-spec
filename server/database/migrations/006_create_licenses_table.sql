-- Licenses table
-- Stores active license tokens for devices
CREATE TABLE licenses (
    id INT AUTO_INCREMENT PRIMARY KEY,
    subscription_id INT NOT NULL,
    client_cert_fingerprint VARCHAR(255) NOT NULL,
    cert_serial_number VARCHAR(255) NOT NULL,
    device_id VARCHAR(255) NOT NULL,
    token TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deactivated_at TIMESTAMP NULL,

    INDEX idx_subscription_id (subscription_id),
    INDEX idx_cert_fingerprint (client_cert_fingerprint),
    INDEX idx_cert_serial (cert_serial_number),
    INDEX idx_device_id (device_id),
    INDEX idx_device_active (device_id, is_active),
    INDEX idx_cert_device (client_cert_fingerprint, device_id),

    FOREIGN KEY (subscription_id) REFERENCES subscriptions(id),
    FOREIGN KEY (cert_serial_number) REFERENCES issued_certificates(serial_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;