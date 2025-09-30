-- Subscriptions table
-- Stores subscription information with device limits
CREATE TABLE subscriptions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    subscription_type ENUM('monthly', 'annual') NOT NULL,
    device_limit INT NOT NULL DEFAULT 1,  -- NEW: Device limit per subscription
    start_date DATETIME NOT NULL,
    end_date DATETIME NOT NULL,
    payment_status ENUM('active', 'pending', 'expired', 'cancelled') DEFAULT 'pending',
    payment_provider VARCHAR(50),
    payment_provider_id VARCHAR(255),
    auto_renew BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_user_id (user_id),
    INDEX idx_end_date (end_date),
    INDEX idx_payment_status (payment_status),
    INDEX idx_user_status (user_id, payment_status),

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;