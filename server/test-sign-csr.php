<?php
require_once __DIR__ . '/vendor/autoload.php';

$config = require __DIR__ . '/config/config.php';

echo "=== Testing CSR Signing ===\n\n";

// Load CA cert and key
$caCertPath = $config['ca']['intermediate_cert_path'];
$caKeyPath = $config['ca']['intermediate_key_path'];
$caPassword = $config['ca']['intermediate_key_password'];

echo "Loading CA certificate from: $caCertPath\n";
$caCert = file_get_contents($caCertPath);
if (!$caCert) {
    die("Failed to read CA cert\n");
}
echo "CA cert loaded: " . strlen($caCert) . " bytes\n";

echo "Loading CA key from: $caKeyPath\n";
$caKeyContent = file_get_contents($caKeyPath);
if (!$caKeyContent) {
    die("Failed to read CA key\n");
}
echo "CA key loaded: " . strlen($caKeyContent) . " bytes\n";

echo "Decrypting CA key with password...\n";
$caKey = openssl_pkey_get_private($caKeyContent, $caPassword);
if (!$caKey) {
    die("Failed to load CA key: " . openssl_error_string() . "\n");
}
echo "CA key decrypted successfully\n\n";

// Generate a test CSR
echo "Generating test private key and CSR...\n";
$dn = array("CN" => "Test Device");
$privkey = openssl_pkey_new(array(
    "private_key_bits" => 2048,
    "private_key_type" => OPENSSL_KEYTYPE_RSA,
));
$csr = openssl_csr_new($dn, $privkey);
openssl_csr_export($csr, $csrOut);
echo "Test CSR generated:\n";
echo substr($csrOut, 0, 100) . "...\n\n";

// Try to sign it
echo "Attempting to sign CSR with intermediate CA...\n";
$cert = openssl_csr_sign(
    $csr,
    $caCert,
    $caKey,
    365,
    array('digest_alg' => 'sha256'),
    123456
);

if (!$cert) {
    echo "FAILED to sign CSR!\n";
    while ($error = openssl_error_string()) {
        echo "OpenSSL Error: $error\n";
    }
} else {
    echo "SUCCESS! Certificate signed\n";
    openssl_x509_export($cert, $certOut);
    echo substr($certOut, 0, 100) . "...\n";
}
