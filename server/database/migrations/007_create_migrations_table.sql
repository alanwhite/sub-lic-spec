-- License migrations table
-- Tracks device-to-device license transfers
CREATE TABLE license_migrations (
    id INT AUTO_INCREMENT PRIMARY KEY,
    migration_token VARCHAR(255) UNIQUE NOT NULL,
    client_cert_fingerprint VARCHAR(255) NOT NULL,
    cert_serial_number VARCHAR(255) NOT NULL,
    old_device_id VARCHAR(255) NOT NULL,
    new_device_id VARCHAR(255),
    expires_at DATETIME NOT NULL,
    completed_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_token (migration_token),
    INDEX idx_expires (expires_at),
    INDEX idx_cert_serial (cert_serial_number),
    INDEX idx_old_device (old_device_id),
    INDEX idx_completed (completed_at),

    FOREIGN KEY (cert_serial_number) REFERENCES issued_certificates(serial_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;