-- Issued certificates table
-- Tracks all client certificates issued by the private CA
CREATE TABLE issued_certificates (
    id INT AUTO_INCREMENT PRIMARY KEY,
    serial_number VARCHAR(255) UNIQUE NOT NULL,
    subject VARCHAR(500) NOT NULL,
    fingerprint VARCHAR(255) UNIQUE NOT NULL,
    user_id INT NOT NULL,
    issued_at DATETIME NOT NULL,
    expires_at DATETIME NOT NULL,
    status ENUM('active', 'revoked', 'expired') DEFAULT 'active',
    revoked_at TIMESTAMP NULL,
    revocation_reason ENUM('account_deletion', 'key_compromise', 'superseded', 'user_revoked') NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_serial (serial_number),
    INDEX idx_fingerprint (fingerprint),
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_status_expires (status, expires_at),
    INDEX idx_expires_at (expires_at),

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;