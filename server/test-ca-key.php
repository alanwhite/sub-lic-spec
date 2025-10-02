<?php
require_once __DIR__ . '/vendor/autoload.php';

$config = require __DIR__ . '/config/config.php';

echo "CA Key Path: " . $config['ca']['intermediate_key_path'] . "\n";
echo "CA Password: " . $config['ca']['intermediate_key_password'] . "\n";

$keyContent = file_get_contents($config['ca']['intermediate_key_path']);
echo "Key content loaded: " . (strlen($keyContent) > 0 ? 'YES' : 'NO') . "\n";

$key = openssl_pkey_get_private($keyContent, $config['ca']['intermediate_key_password']);
if ($key) {
    echo "SUCCESS: Key loaded successfully!\n";
    $details = openssl_pkey_get_details($key);
    echo "Key type: " . $details['type'] . "\n";
    echo "Key bits: " . $details['bits'] . "\n";
} else {
    echo "FAILED: " . openssl_error_string() . "\n";

    // Try with no password
    $key = openssl_pkey_get_private($keyContent);
    if ($key) {
        echo "Key loaded without password - key might not be encrypted!\n";
    }
}
