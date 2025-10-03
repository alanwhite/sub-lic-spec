<?php

namespace App\Api;

use App\Services\JwtService;

/**
 * License Controller
 * Handles license operations (mTLS required)
 */
class LicenseController
{
    private JwtService $jwtService;

    public function __construct()
    {
        $this->jwtService = new JwtService();
    }
    /**
     * Activate license for device
     * POST /api/v1/license/activate
     */
    public function activate(): string
    {
        // TODO: Implement license activation
        // 1. Validate mTLS certificate
        // 2. Validate subscription status
        // 3. Generate license token
        // 4. Store license record
        // 5. Return license token

        return json_encode(['error' => 'License activation not yet implemented']);
    }

    /**
     * Renew license
     * POST /api/v1/license/renew
     */
    public function renew(): string
    {
        // TODO: Implement license renewal
        // 1. Validate mTLS certificate
        // 2. Check subscription status
        // 3. Check payment status
        // 4. Generate new license token
        // 5. Return new license token

        return json_encode(['error' => 'License renewal not yet implemented']);
    }

    /**
     * Get license status
     * GET /api/v1/license/status
     */
    public function getStatus(): string
    {
        // TODO: Implement status check
        // 1. Validate mTLS certificate
        // 2. Get subscription details
        // 3. Return status, expiry, grace period info

        return json_encode(['error' => 'License status not yet implemented']);
    }

    /**
     * Verify license token and issue new JWT (test endpoint)
     * POST /api/v1/license/verify
     */
    public function verify(): string
    {
        header('Content-Type: application/json');

        // Get client certificate info from Apache
        $clientCert = $_SERVER['SSL_CLIENT_CERT'] ?? null;
        $clientDN = $_SERVER['SSL_CLIENT_S_DN'] ?? null;
        $verifyResult = $_SERVER['SSL_CLIENT_VERIFY'] ?? null;

        if ($verifyResult !== 'SUCCESS') {
            http_response_code(401);
            return json_encode([
                'valid' => false,
                'error' => 'Client certificate verification failed'
            ]);
        }

        // Get license token from request body (optional for testing)
        $input = json_decode(file_get_contents('php://input'), true);
        $existingToken = $input['license_token'] ?? null;

        // If token provided, verify it
        if ($existingToken && $existingToken !== 'eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.test') {
            $payload = $this->jwtService->verifyToken($existingToken);
            if (!$payload) {
                http_response_code(400);
                return json_encode([
                    'valid' => false,
                    'error' => 'Invalid or expired license token'
                ]);
            }
        }

        // Generate a new JWT license token for testing
        // In production, this would check subscription status, payment, etc.
        $certFingerprint = JwtService::getCertificateFingerprint();

        // For demo purposes, extract email from DN or use a test value
        $email = 'test@example.com';  // In production, look up from database
        $tier = 'premium';             // In production, check subscription
        $expiresInDays = 30;          // In production, based on subscription type

        $newLicenseToken = $this->jwtService->createLicenseToken(
            $email,
            $tier,
            $certFingerprint,
            $expiresInDays
        );

        return json_encode([
            'valid' => true,
            'message' => 'mTLS connection successful',
            'client_dn' => $clientDN,
            'certificate_verified' => true,
            'license_token' => $newLicenseToken,
            'expires_in_days' => $expiresInDays,
            'tier' => $tier
        ]);
    }
}