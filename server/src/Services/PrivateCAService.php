<?php

namespace App\Services;

/**
 * Private CA Service
 * Handles certificate issuance using the intermediate CA
 */
class PrivateCAService
{
    private array $config;

    public function __construct()
    {
        $this->config = require __DIR__ . '/../../config/config.php';
    }

    /**
     * Issue a client certificate from CSR
     *
     * @param string $csr Certificate Signing Request (PEM format)
     * @param string $commonName Common Name for certificate
     * @param int $validityYears Validity period in years
     * @return array ['certificate' => string, 'serial' => string, 'fingerprint' => string]
     */
    public function issueCertificate(string $csr, string $commonName, int $validityYears = 2): array
    {
        $caKeyPath = $this->config['ca']['intermediate_key_path'];
        $caCertPath = $this->config['ca']['intermediate_cert_path'];
        $caPassword = $this->config['ca']['intermediate_key_password'];

        // Create temporary files for CSR
        $csrFile = tempnam(sys_get_temp_dir(), 'csr_');
        $certFile = tempnam(sys_get_temp_dir(), 'cert_');
        file_put_contents($csrFile, $csr);

        try {
            // Write CSR to temp file
            file_put_contents($csrFile, $csr);
            error_log("CSR written to $csrFile for signing");

            // Calculate validity period
            $validityDays = $validityYears * 365;

            // Generate certificate using command-line openssl
            $serial = sprintf('%X', random_int(100000, 999999));
            error_log("Attempting to sign CSR with serial $serial using openssl command");

            // Create temp file for output cert
            $certOutFile = tempnam(sys_get_temp_dir(), 'cert_out_');

            // Create extensions config file for client auth
            $extFile = tempnam(sys_get_temp_dir(), 'ext_');
            $extConfig = <<<EOT
keyUsage = critical, digitalSignature, keyEncipherment
extendedKeyUsage = clientAuth
basicConstraints = critical, CA:FALSE
subjectKeyIdentifier = hash
EOT;
            file_put_contents($extFile, $extConfig);

            // Use openssl command to sign the CSR with extensions
            $cmd = sprintf(
                'openssl x509 -req -in %s -CA %s -CAkey %s -passin pass:%s -out %s -days %d -sha256 -set_serial 0x%s -extfile %s 2>&1',
                escapeshellarg($csrFile),
                escapeshellarg($caCertPath),
                escapeshellarg($caKeyPath),
                escapeshellarg($caPassword),
                escapeshellarg($certOutFile),
                $validityDays,
                $serial,
                escapeshellarg($extFile)
            );

            error_log("Running: openssl x509 -req with client auth extensions...");
            exec($cmd, $output, $returnCode);

            // Clean up extensions file
            @unlink($extFile);

            if ($returnCode !== 0) {
                $errorMsg = implode("\n", $output);
                error_log("OpenSSL command failed: " . $errorMsg);
                throw new \Exception('Failed to sign certificate: ' . $errorMsg);
            }

            // Read the generated certificate
            $certPem = file_get_contents($certOutFile);
            if (!$certPem) {
                throw new \Exception('Failed to read generated certificate');
            }

            error_log("Certificate signed successfully");

            // Parse certificate for details
            $certData = openssl_x509_parse($certPem);
            $serialHex = dechex($certData['serialNumber']);

            // Calculate SHA-256 fingerprint
            $fingerprint = hash('sha256', $certPem);

            // Clean up temp cert file
            @unlink($certOutFile);

            return [
                'certificate' => $certPem,
                'serial' => $serial,
                'fingerprint' => $fingerprint
            ];
        } finally {
            // Clean up temporary files
            if (file_exists($csrFile)) {
                unlink($csrFile);
            }
            if (file_exists($certFile)) {
                unlink($certFile);
            }
        }
    }

    /**
     * Revoke a certificate
     *
     * @param string $serialNumber Certificate serial number
     * @param string $reason Revocation reason
     * @return bool Success status
     */
    public function revokeCertificate(string $serialNumber, string $reason): bool
    {
        // TODO: Implement certificate revocation
        // 1. Add to CRL
        // 2. Update CRL file
        // 3. Return success

        throw new \Exception('Certificate revocation not yet implemented');
    }

    /**
     * Update Certificate Revocation List
     *
     * @return bool Success status
     */
    public function updateCRL(): bool
    {
        // TODO: Implement CRL update
        // 1. Generate new CRL from revoked certificates
        // 2. Write to public CRL path
        // 3. Return success

        throw new \Exception('CRL update not yet implemented');
    }
}