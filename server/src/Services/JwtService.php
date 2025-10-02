<?php

namespace App\Services;

/**
 * JWT Service
 * Creates and validates JWT license tokens
 */
class JwtService
{
    private $privateKeyPath;
    private $publicKeyPath;
    private $privateKey;
    private $publicKey;

    public function __construct()
    {
        $this->privateKeyPath = '/etc/license-server/jwt-private.pem';
        $this->publicKeyPath = '/etc/license-server/jwt-public.pem';

        // Load keys
        $this->privateKey = openssl_pkey_get_private(file_get_contents($this->privateKeyPath));
        $this->publicKey = openssl_pkey_get_public(file_get_contents($this->publicKeyPath));

        if (!$this->privateKey || !$this->publicKey) {
            throw new \Exception("Failed to load JWT signing keys");
        }
    }

    /**
     * Create a JWT license token
     *
     * @param string $email User email
     * @param string $tier License tier (free, premium, enterprise)
     * @param string $certFingerprint Certificate fingerprint for binding
     * @param int $expiresInDays Days until expiration
     * @return string JWT token
     */
    public function createLicenseToken(
        string $email,
        string $tier,
        string $certFingerprint,
        int $expiresInDays = 30
    ): string {
        $now = time();
        $exp = $now + ($expiresInDays * 24 * 60 * 60);

        // JWT Header
        $header = [
            'typ' => 'JWT',
            'alg' => 'RS256'
        ];

        // JWT Payload/Claims
        $payload = [
            'iss' => 'license-server.example.com',  // Issuer
            'sub' => $email,                         // Subject (user)
            'iat' => $now,                           // Issued at
            'exp' => $exp,                           // Expiration
            'email' => $email,
            'tier' => $tier,
            'cert_fingerprint' => $certFingerprint,  // Bind to certificate
            'jti' => bin2hex(random_bytes(16))       // Unique token ID
        ];

        // Encode header and payload
        $headerEncoded = $this->base64UrlEncode(json_encode($header));
        $payloadEncoded = $this->base64UrlEncode(json_encode($payload));

        // Create signature
        $signatureInput = $headerEncoded . '.' . $payloadEncoded;
        openssl_sign($signatureInput, $signature, $this->privateKey, OPENSSL_ALGO_SHA256);
        $signatureEncoded = $this->base64UrlEncode($signature);

        // Return complete JWT
        return $headerEncoded . '.' . $payloadEncoded . '.' . $signatureEncoded;
    }

    /**
     * Verify a JWT token
     *
     * @param string $jwt JWT token
     * @return array|null Decoded payload if valid, null if invalid
     */
    public function verifyToken(string $jwt): ?array
    {
        $parts = explode('.', $jwt);
        if (count($parts) !== 3) {
            return null;
        }

        list($headerEncoded, $payloadEncoded, $signatureEncoded) = $parts;

        // Verify signature
        $signatureInput = $headerEncoded . '.' . $payloadEncoded;
        $signature = $this->base64UrlDecode($signatureEncoded);

        $verified = openssl_verify(
            $signatureInput,
            $signature,
            $this->publicKey,
            OPENSSL_ALGO_SHA256
        );

        if ($verified !== 1) {
            return null;
        }

        // Decode payload
        $payload = json_decode($this->base64UrlDecode($payloadEncoded), true);

        // Check expiration
        if (isset($payload['exp']) && time() > $payload['exp']) {
            return null; // Token expired
        }

        return $payload;
    }

    /**
     * Get certificate fingerprint from client certificate
     *
     * @return string|null SHA256 fingerprint or null if not available
     */
    public static function getCertificateFingerprint(): ?string
    {
        $certPem = $_SERVER['SSL_CLIENT_CERT'] ?? null;
        if (!$certPem) {
            return null;
        }

        // Parse certificate
        $cert = openssl_x509_read($certPem);
        if (!$cert) {
            return null;
        }

        // Export to DER format for fingerprinting
        openssl_x509_export($cert, $certPem);

        // Calculate SHA256 fingerprint
        return hash('sha256', $certPem);
    }

    /**
     * Base64 URL encode
     */
    private function base64UrlEncode(string $data): string
    {
        return rtrim(strtr(base64_encode($data), '+/', '-_'), '=');
    }

    /**
     * Base64 URL decode
     */
    private function base64UrlDecode(string $data): string
    {
        return base64_decode(strtr($data, '-_', '+/'));
    }
}
