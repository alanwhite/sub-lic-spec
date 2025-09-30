-- Clients table
-- Tracks enrolled devices with identification information
CREATE TABLE clients (
    id INT AUTO_INCREMENT PRIMARY KEY,
    client_cert_fingerprint VARCHAR(255) UNIQUE NOT NULL,
    cert_serial_number VARCHAR(255) NOT NULL,
    user_id INT NOT NULL,
    subscriber_email VARCHAR(255) NOT NULL,
    subscriber_name VARCHAR(255) NOT NULL,
    organization VARCHAR(255),
    device_name VARCHAR(255),  -- NEW: User-provided device name
    platform ENUM('windows', 'macos', 'linux', 'other') NOT NULL,  -- NEW: Auto-detected platform
    enrollment_token VARCHAR(255),
    subscription_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_seen TIMESTAMP NULL,

    INDEX idx_fingerprint (client_cert_fingerprint),
    INDEX idx_cert_serial (cert_serial_number),
    INDEX idx_user_id (user_id),
    INDEX idx_subscription_id (subscription_id),
    INDEX idx_last_seen (last_seen),
    INDEX idx_user_platform (user_id, platform),

    FOREIGN KEY (enrollment_token) REFERENCES enrollment_tokens(token),
    FOREIGN KEY (cert_serial_number) REFERENCES issued_certificates(serial_number),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (subscription_id) REFERENCES subscriptions(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;