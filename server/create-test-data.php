<?php
require_once __DIR__ . '/vendor/autoload.php';

use App\Database\Database;

$db = Database::getInstance();

try {
    // 1. Create test user
    $email = 'test@example.com';
    $passwordHash = password_hash('testpassword', PASSWORD_DEFAULT);

    $db->query(
        "INSERT INTO users (email, full_name, password_hash, status, created_at)
         VALUES (?, ?, ?, 'active', NOW())
         ON DUPLICATE KEY UPDATE id=LAST_INSERT_ID(id)",
        [$email, 'Test User', $passwordHash]
    );
    $userId = $db->getConnection()->lastInsertId();
    echo "Created user: $userId ($email)\n";

    // 2. Create test subscription
    $db->query(
        "INSERT INTO subscriptions (user_id, subscription_type, device_limit, start_date, end_date, payment_status, created_at)
         VALUES (?, 'monthly', 5, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 'active', NOW())",
        [$userId]
    );
    $subscriptionId = $db->getConnection()->lastInsertId();
    echo "Created subscription: $subscriptionId (monthly, 5 devices)\n";

    // 3. Create enrollment token
    $token = bin2hex(random_bytes(32)); // 64-character hex token
    $db->query(
        "INSERT INTO enrollment_tokens (user_id, subscription_id, token, subscriber_email, subscriber_name, subscription_type, expires_at, created_at)
         VALUES (?, ?, ?, ?, ?, 'monthly', DATE_ADD(NOW(), INTERVAL 24 HOUR), NOW())",
        [$userId, $subscriptionId, $token, $email, 'Test User']
    );
    echo "Created enrollment token: $token\n";
    echo "\nYou can use this token to enroll a device in the Java client!\n";
    echo "Token expires in 24 hours.\n";

} catch (Exception $e) {
    echo "Error: " . $e->getMessage() . "\n";
    exit(1);
}
