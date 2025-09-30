<?php

/**
 * Application Routes
 * Maps HTTP routes to controller actions
 */

use App\Api\CertificateController;
use App\Api\LicenseController;
use App\Api\MigrationController;
use App\Api\PortalController;

return [
    // Portal endpoints (session-based authentication)
    'portal' => [
        'POST /portal/login' => [PortalController::class, 'login'],
        'POST /portal/logout' => [PortalController::class, 'logout'],
        'GET /portal/account' => [PortalController::class, 'getAccount'],
        'POST /portal/account/delete' => [PortalController::class, 'deleteAccount'],
        'POST /portal/account/delete/confirm' => [PortalController::class, 'confirmDeleteAccount'],

        // Enrollment tokens
        'POST /portal/enrollment/generate' => [PortalController::class, 'generateEnrollmentToken'],
        'GET /portal/enrollment/tokens' => [PortalController::class, 'listEnrollmentTokens'],

        // Device management
        'GET /portal/devices' => [PortalController::class, 'listDevices'],
        'DELETE /portal/devices/:fingerprint' => [PortalController::class, 'revokeDevice'],

        // Subscription management
        'GET /portal/subscription' => [PortalController::class, 'getSubscription'],
        'POST /portal/subscription/update' => [PortalController::class, 'updateSubscription'],
    ],

    // Certificate enrollment (TLS + token authentication)
    'certificate' => [
        'POST /api/certificate/enroll' => [CertificateController::class, 'enroll'],
        'POST /api/certificate/renew' => [CertificateController::class, 'renew'],
        'GET /crl/current.crl' => [CertificateController::class, 'getCRL'],
    ],

    // License operations (mTLS required)
    'license' => [
        'POST /api/license/activate' => [LicenseController::class, 'activate'],
        'POST /api/license/renew' => [LicenseController::class, 'renew'],
        'GET /api/license/status' => [LicenseController::class, 'getStatus'],
        'POST /api/license/deactivate' => [LicenseController::class, 'deactivate'],
    ],

    // Migration operations (mTLS required)
    'migration' => [
        'POST /api/migration/initiate' => [MigrationController::class, 'initiate'],
        'POST /api/migration/complete' => [MigrationController::class, 'complete'],
        'GET /api/migration/status/:token' => [MigrationController::class, 'getStatus'],
    ],
];