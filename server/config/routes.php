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
        'POST /portal/v1/login' => [PortalController::class, 'login'],
        'POST /portal/v1/logout' => [PortalController::class, 'logout'],
        'GET /portal/v1/account' => [PortalController::class, 'getAccount'],
        'POST /portal/v1/account/delete' => [PortalController::class, 'deleteAccount'],
        'POST /portal/v1/account/delete/confirm' => [PortalController::class, 'confirmDeleteAccount'],

        // Enrollment tokens
        'POST /portal/v1/enrollment/generate' => [PortalController::class, 'generateEnrollmentToken'],
        'GET /portal/v1/enrollment/tokens' => [PortalController::class, 'listEnrollmentTokens'],

        // Device management
        'GET /portal/v1/devices' => [PortalController::class, 'listDevices'],
        'DELETE /portal/v1/devices/:fingerprint' => [PortalController::class, 'revokeDevice'],

        // Subscription management
        'GET /portal/v1/subscription' => [PortalController::class, 'getSubscription'],
        'POST /portal/v1/subscription/update' => [PortalController::class, 'updateSubscription'],
    ],

    // Certificate enrollment (TLS + token authentication)
    'certificate' => [
        'POST /api/v1/certificate/enroll' => [CertificateController::class, 'enroll'],
        'POST /api/v1/certificate/renew' => [CertificateController::class, 'renew'],
        'GET /crl/v1/current.crl' => [CertificateController::class, 'getCRL'],
    ],

    // License operations (mTLS required)
    'license' => [
        'POST /api/v1/license/activate' => [LicenseController::class, 'activate'],
        'POST /api/v1/license/renew' => [LicenseController::class, 'renew'],
        'GET /api/v1/license/status' => [LicenseController::class, 'getStatus'],
        'POST /api/v1/license/verify' => [LicenseController::class, 'verify'],
        'POST /api/v1/license/deactivate' => [LicenseController::class, 'deactivate'],
    ],

    // Migration operations (mTLS required)
    'migration' => [
        'POST /api/v1/migration/initiate' => [MigrationController::class, 'initiate'],
        'POST /api/v1/migration/complete' => [MigrationController::class, 'complete'],
        'GET /api/v1/migration/status/:token' => [MigrationController::class, 'getStatus'],
    ],
];