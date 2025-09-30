<?php

namespace App\Api;

/**
 * Certificate Controller
 * Handles certificate enrollment (TLS + token authentication)
 */
class CertificateController
{
    /**
     * Enroll a new client certificate
     * POST /api/certificate/enroll
     *
     * Request body:
     * - token: Enrollment token
     * - csr: Certificate Signing Request (PEM)
     * - device_name: User-provided device name
     * - platform: Auto-detected platform (windows, macos, linux)
     *
     * Response:
     * - certificate: Issued client certificate
     * - license_token: Initial JWT license token
     * - ca_chain: Certificate chain for validation
     */
    public function enroll(): string
    {
        // TODO: Implement enrollment
        // 1. Validate enrollment token
        // 2. Check device limit
        // 3. Issue certificate from CSR
        // 4. Generate initial license token
        // 5. Store client record
        // 6. Mark token as used
        // 7. Return certificate + license token

        return json_encode([
            'error' => 'Enrollment not yet implemented',
        ]);
    }

    /**
     * Get Certificate Revocation List
     * GET /crl/current.crl
     */
    public function getCRL(): void
    {
        $config = require __DIR__ . '/../../config/config.php';
        $crlPath = $config['ca']['crl_path'];

        if (file_exists($crlPath)) {
            header('Content-Type: application/pkix-crl');
            header('Content-Disposition: attachment; filename="current.crl"');
            readfile($crlPath);
        } else {
            http_response_code(404);
            echo json_encode(['error' => 'CRL not found']);
        }
    }
}