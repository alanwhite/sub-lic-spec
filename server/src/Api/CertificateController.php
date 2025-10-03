<?php

namespace App\Api;

use App\Database\Database;
use App\Services\PrivateCAService;
use App\Services\LicenseTokenService;

/**
 * Certificate Controller
 * Handles certificate enrollment (TLS + token authentication)
 */
class CertificateController
{
    private Database $db;
    private PrivateCAService $caService;
    private LicenseTokenService $licenseService;

    public function __construct()
    {
        $this->db = Database::getInstance();
        $this->caService = new PrivateCAService();
        $this->licenseService = new LicenseTokenService();
    }

    /**
     * Enroll a new client certificate
     * POST /api/v1/certificate/enroll
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
        try {
            // Parse JSON request body
            $input = json_decode(file_get_contents('php://input'), true);
            if (!$input) {
                http_response_code(400);
                return json_encode(['error' => 'Invalid JSON request']);
            }

            $token = trim($input['token'] ?? '', " \t\n\r\0\x0B.");
            $csr = $input['csr'] ?? '';
            $deviceName = trim($input['device_name'] ?? '');
            $platform = $input['platform'] ?? '';
            $deviceId = $input['device_id'] ?? '';

            // Validate required fields
            if (empty($token) || empty($csr) || empty($deviceName) || empty($platform) || empty($deviceId)) {
                http_response_code(400);
                return json_encode(['error' => 'Missing required fields']);
            }

            // 1. Validate enrollment token
            error_log("Validating token: " . substr($token, 0, 20) . "... (length: " . strlen($token) . ")");
            error_log("Full token: " . $token);
            $stmt = $this->db->query(
                "SELECT id, user_id, subscription_id, used_count, max_uses, expires_at
                 FROM enrollment_tokens
                 WHERE token = ? AND used_count < max_uses AND revoked_at IS NULL",
                [$token]
            );
            $tokenRecord = $stmt->fetch(\PDO::FETCH_ASSOC);

            if (!$tokenRecord) {
                error_log("Token validation failed - not found or used");
                http_response_code(401);
                return json_encode(['error' => 'Invalid or used enrollment token']);
            }
            error_log("Token valid, ID: " . $tokenRecord['id']);

            if (strtotime($tokenRecord['expires_at']) < time()) {
                http_response_code(401);
                return json_encode(['error' => 'Enrollment token expired']);
            }

            // 2. Check device limit - count unique device_ids
            $stmt = $this->db->query(
                "SELECT device_limit FROM subscriptions WHERE id = ?",
                [$tokenRecord['subscription_id']]
            );
            $subscription = $stmt->fetch(\PDO::FETCH_ASSOC);

            // Check if this device_id is already enrolled
            $stmt = $this->db->query(
                "SELECT COUNT(*) as count FROM clients
                 WHERE subscription_id = ? AND device_id = ?",
                [$tokenRecord['subscription_id'], $deviceId]
            );
            $existingDevice = $stmt->fetch(\PDO::FETCH_ASSOC)['count'];

            // If device not already enrolled, check if we have room for a new device
            if ($existingDevice == 0) {
                // Count unique device_ids (not total enrollments)
                $stmt = $this->db->query(
                    "SELECT COUNT(DISTINCT device_id) as count FROM clients
                     WHERE subscription_id = ?",
                    [$tokenRecord['subscription_id']]
                );
                $uniqueDeviceCount = $stmt->fetch(\PDO::FETCH_ASSOC)['count'];

                if ($uniqueDeviceCount >= $subscription['device_limit']) {
                    http_response_code(403);
                    return json_encode(['error' => 'Device limit reached for this subscription. Please revoke a device in the customer portal.']);
                }
            }
            // If device already enrolled, allow re-enrollment (certificate renewal)

            // 3. Issue certificate from CSR
            error_log("Received CSR (first 100 chars): " . substr($csr, 0, 100));
            error_log("CSR length: " . strlen($csr));
            $certData = $this->caService->issueCertificate($csr, $deviceName, 2);

            // Get user info for client record
            $stmt = $this->db->query(
                "SELECT email, full_name FROM users WHERE id = ?",
                [$tokenRecord['user_id']]
            );
            $user = $stmt->fetch(\PDO::FETCH_ASSOC);

            // 4. Store certificate record
            $this->db->query(
                "INSERT INTO issued_certificates (serial_number, subject, fingerprint, user_id, issued_at, expires_at, status)
                 VALUES (?, ?, ?, ?, NOW(), DATE_ADD(NOW(), INTERVAL 2 YEAR), 'active')",
                [
                    $certData['serial'],
                    'CN=' . $deviceName,
                    $certData['fingerprint'],
                    $tokenRecord['user_id']
                ]
            );

            // 5. Store client record
            $this->db->query(
                "INSERT INTO clients (client_cert_fingerprint, cert_serial_number, user_id, subscriber_email, subscriber_name, device_name, platform, device_id, enrollment_token, subscription_id, last_seen)
                 VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())",
                [
                    $certData['fingerprint'],
                    $certData['serial'],
                    $tokenRecord['user_id'],
                    $user['email'],
                    $user['full_name'],
                    $deviceName,
                    $platform,
                    $deviceId,
                    $token,
                    $tokenRecord['subscription_id']
                ]
            );
            $clientId = $this->db->getConnection()->lastInsertId();

            // 6. Generate initial license token
            $licensePayload = [
                'sub' => $tokenRecord['subscription_id'],
                'cert_fp' => $certData['fingerprint'],
                'device_id' => $deviceId,
                'tier' => 'premium', // TODO: Get from subscription
                'exp' => time() + (30 * 24 * 60 * 60), // 30 days
                'iat' => time()
            ];
            $licenseToken = $this->licenseService->generateToken($licensePayload);

            // 7. Store license token
            $this->db->query(
                "INSERT INTO licenses (subscription_id, client_cert_fingerprint, cert_serial_number, device_id, token, is_active)
                 VALUES (?, ?, ?, ?, ?, TRUE)",
                [
                    $tokenRecord['subscription_id'],
                    $certData['fingerprint'],
                    $certData['serial'],
                    $deviceId,
                    $licenseToken
                ]
            );

            // 8. Mark enrollment token as used
            $this->db->query(
                "UPDATE enrollment_tokens SET used_count = used_count + 1, used_at = NOW() WHERE id = ?",
                [$tokenRecord['id']]
            );

            // 9. Load CA chain
            $config = require __DIR__ . '/../../config/config.php';
            $caChain = file_get_contents($config['ca']['ca_chain']);

            // Return certificate + license token
            return json_encode([
                'certificate' => $certData['certificate'],
                'license_token' => $licenseToken,
                'ca_chain' => $caChain,
                'expires_at' => date('Y-m-d H:i:s', $licensePayload['exp'])
            ]);

        } catch (\Exception $e) {
            error_log('Enrollment error: ' . $e->getMessage());
            http_response_code(500);
            return json_encode(['error' => 'Enrollment failed: ' . $e->getMessage()]);
        }
    }

    /**
     * Get Certificate Revocation List
     * GET /crl/v1/current.crl
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