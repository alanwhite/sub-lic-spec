<?php

namespace App\Services;

use Firebase\JWT\JWT;
use Firebase\JWT\Key;

/**
 * License Token Service
 * Handles JWT license token generation and validation
 */
class LicenseTokenService
{
    private array $config;
    private $privateKey;  // OpenSSLAsymmetricKey resource
    private string $publicKey;

    public function __construct()
    {
        $this->config = require __DIR__ . '/../../config/config.php';
        $this->loadKeys();
    }

    private function loadKeys(): void
    {
        $keyPath = $this->config['license']['signing_key_path'];
        $keyPassword = $this->config['license']['signing_key_password'];

        $privateKeyContent = file_get_contents($keyPath);
        $this->privateKey = openssl_pkey_get_private($privateKeyContent, $keyPassword);

        if (!$this->privateKey) {
            throw new \Exception('Failed to load license signing key: ' . openssl_error_string());
        }

        $pubPath = $this->config['license']['signing_pub_path'];
        $this->publicKey = file_get_contents($pubPath);
    }

    /**
     * Generate a license token
     *
     * @param array $payload Token payload (subscription, device, etc.)
     * @return string JWT token
     */
    public function generateToken(array $payload): string
    {
        $payload['iat'] = time();
        $payload['exp'] = $payload['exp'] ?? (time() + 86400 * 30); // 30 days default

        return JWT::encode($payload, $this->privateKey, 'RS256');
    }

    /**
     * Validate and decode a license token
     *
     * @param string $token JWT token
     * @return array Decoded payload
     * @throws \Exception if token is invalid
     */
    public function validateToken(string $token): array
    {
        try {
            $decoded = JWT::decode($token, new Key($this->publicKey, 'RS256'));
            return (array) $decoded;
        } catch (\Exception $e) {
            throw new \Exception('Invalid license token: ' . $e->getMessage());
        }
    }
}