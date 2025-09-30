-- Enrollment tokens table
-- Single-use tokens for certificate enrollment
CREATE TABLE enrollment_tokens (
    id INT AUTO_INCREMENT PRIMARY KEY,
    token VARCHAR(255) UNIQUE NOT NULL,
    user_id INT NOT NULL,
    subscriber_email VARCHAR(255) NOT NULL,
    subscriber_name VARCHAR(255) NOT NULL,
    organization VARCHAR(255),
    subscription_type ENUM('monthly', 'annual') NOT NULL,
    subscription_id INT NOT NULL,
    expires_at DATETIME NOT NULL,
    max_uses INT DEFAULT 1,
    used_count INT DEFAULT 0,
    used_at TIMESTAMP NULL,
    certificate_fingerprint VARCHAR(255),
    revoked_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_token (token),
    INDEX idx_user_id (user_id),
    INDEX idx_expires (expires_at),
    INDEX idx_subscription_id (subscription_id),
    INDEX idx_used (used_count, max_uses),

    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (subscription_id) REFERENCES subscriptions(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;