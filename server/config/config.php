<?php

/**
 * Application Configuration
 * Loads configuration from environment variables
 */

return [
    'app' => [
        'env' => $_ENV['APP_ENV'] ?? 'production',
        'debug' => filter_var($_ENV['APP_DEBUG'] ?? false, FILTER_VALIDATE_BOOLEAN),
        'url' => $_ENV['APP_URL'] ?? 'https://license-server.local',
        'port' => $_ENV['APP_PORT'] ?? 8443,
    ],

    'database' => [
        'connection' => $_ENV['DB_CONNECTION'] ?? 'mysql',
        'host' => $_ENV['DB_HOST'] ?? 'localhost',
        'port' => $_ENV['DB_PORT'] ?? 3306,
        'database' => $_ENV['DB_DATABASE'] ?? 'license_system',
        'username' => $_ENV['DB_USERNAME'] ?? 'license_user',
        'password' => $_ENV['DB_PASSWORD'] ?? '',
        'charset' => 'utf8mb4',
        'collation' => 'utf8mb4_unicode_ci',
    ],

    'ca' => [
        'root_cert_path' => $_ENV['CA_ROOT_CERT_PATH'] ?? '/etc/ca/root-ca/certs/root-ca.crt',
        'root_key_path' => $_ENV['CA_ROOT_KEY_PATH'] ?? '/etc/ca/root-ca/private/root-ca.key',
        'root_key_password' => $_ENV['CA_ROOT_KEY_PASSWORD'] ?? '',
        'intermediate_cert_path' => $_ENV['CA_INTERMEDIATE_CERT_PATH'] ?? '/etc/ca/intermediate-ca/certs/intermediate-ca.crt',
        'intermediate_key_path' => $_ENV['CA_INTERMEDIATE_KEY_PATH'] ?? '/etc/ca/intermediate-ca/private/intermediate-ca.key',
        'intermediate_key_password' => $_ENV['CA_INTERMEDIATE_KEY_PASSWORD'] ?? '',
        'ca_chain' => $_ENV['CA_CHAIN_PATH'] ?? '/etc/ca/intermediate-ca/certs/ca-chain.crt',
        'issued_certs_dir' => $_ENV['CA_ISSUED_CERTS_DIR'] ?? '/etc/ca/issued-certificates',
        'crl_path' => $_ENV['CA_CRL_PATH'] ?? '/var/www/html/public/crl/current.crl',
        'crl_update_interval' => $_ENV['CA_CRL_UPDATE_INTERVAL'] ?? 86400,
    ],

    'license' => [
        'signing_key_path' => $_ENV['LICENSE_SIGNING_KEY_PATH'] ?? '/etc/license-server/license-signing.key',
        'signing_key_password' => $_ENV['LICENSE_SIGNING_KEY_PASSWORD'] ?? '',
        'signing_pub_path' => $_ENV['LICENSE_SIGNING_PUB_PATH'] ?? '/etc/license-server/license-signing.pub',
    ],

    'tls' => [
        'cert_path' => $_ENV['TLS_CERT_PATH'] ?? '/etc/ssl/certs-custom/server.crt',
        'key_path' => $_ENV['TLS_KEY_PATH'] ?? '/etc/ssl/certs-custom/server.key',
        'port' => $_ENV['TLS_PORT'] ?? 8443,
        'mtls_port' => $_ENV['MTLS_PORT'] ?? 9443,
    ],

    'security' => [
        'jwt_secret' => $_ENV['JWT_SECRET'] ?? 'change_this_in_production',
        'encryption_key' => $_ENV['ENCRYPTION_KEY'] ?? 'change_this_in_production_32byte',
    ],

    'subscription' => [
        'grace_period_monthly' => $_ENV['GRACE_PERIOD_MONTHLY'] ?? 5,
        'grace_period_annual' => $_ENV['GRACE_PERIOD_ANNUAL'] ?? 14,
        'default_device_limit' => $_ENV['DEFAULT_DEVICE_LIMIT'] ?? 1,
    ],

    'certificate' => [
        'validity_years' => $_ENV['CERTIFICATE_VALIDITY_YEARS'] ?? 2,
        'renewal_days' => $_ENV['CERTIFICATE_RENEWAL_DAYS'] ?? 30,
    ],

    'rate_limit' => [
        'enrollment_tokens' => $_ENV['RATE_LIMIT_ENROLLMENT_TOKENS'] ?? 5,
        'enrollment_window' => $_ENV['RATE_LIMIT_ENROLLMENT_WINDOW'] ?? 86400,
        'license' => $_ENV['RATE_LIMIT_LICENSE'] ?? 100,
        'license_window' => $_ENV['RATE_LIMIT_LICENSE_WINDOW'] ?? 3600,
    ],

    'logging' => [
        'level' => $_ENV['LOG_LEVEL'] ?? 'info',
        'path' => $_ENV['LOG_PATH'] ?? '/var/log/license-server/app.log',
        'audit_path' => $_ENV['AUDIT_LOG_PATH'] ?? '/var/log/license-server/audit.log',
        'max_size' => $_ENV['LOG_MAX_SIZE'] ?? 10485760,
        'max_files' => $_ENV['LOG_MAX_FILES'] ?? 10,
    ],

    'payment' => [
        'provider' => $_ENV['PAYMENT_PROVIDER'] ?? null,
        'api_key' => $_ENV['PAYMENT_PROVIDER_API_KEY'] ?? null,
        'webhook_secret' => $_ENV['PAYMENT_PROVIDER_WEBHOOK_SECRET'] ?? null,
    ],

    'mail' => [
        'mailer' => $_ENV['MAIL_MAILER'] ?? 'smtp',
        'host' => $_ENV['MAIL_HOST'] ?? 'localhost',
        'port' => $_ENV['MAIL_PORT'] ?? 25,
        'username' => $_ENV['MAIL_USERNAME'] ?? null,
        'password' => $_ENV['MAIL_PASSWORD'] ?? null,
        'encryption' => $_ENV['MAIL_ENCRYPTION'] ?? null,
        'from_address' => $_ENV['MAIL_FROM_ADDRESS'] ?? 'noreply@license-server.com',
        'from_name' => $_ENV['MAIL_FROM_NAME'] ?? 'License Server',
    ],

    'monitoring' => [
        'sentry_dsn' => $_ENV['SENTRY_DSN'] ?? null,
        'analytics_enabled' => filter_var($_ENV['ANALYTICS_ENABLED'] ?? false, FILTER_VALIDATE_BOOLEAN),
    ],
];