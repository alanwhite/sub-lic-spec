# Implementation Guide - Reference Implementation

This document provides concrete implementation details to complement the design specification in `spec.md`. It is intended to enable automated code generation tools and developers to build a complete, working reference implementation.

## Table of Contents
1. [Project Structure](#project-structure)
2. [Development Environment Setup](#development-environment-setup)
3. [Configuration Management](#configuration-management)
4. [Database Setup and Migrations](#database-setup-and-migrations)
5. [Private CA Setup](#private-ca-setup)
6. [Server Implementation Details](#server-implementation-details)
7. [Client Implementation Details](#client-implementation-details)
8. [Build and Packaging](#build-and-packaging)
9. [Testing Strategy](#testing-strategy)
10. [Deployment](#deployment)
11. [Monitoring and Logging](#monitoring-and-logging)

---

## 1. Project Structure

```
sub-lic-spec/
├── spec.md                          # Design specification
├── README.md                        # Project overview
├── LICENSE                          # MIT License
├── CONTRIBUTING.md                  # Contribution guidelines
├── IMPLEMENTATION.md                # This file
│
├── ca/                              # Certificate Authority
│   ├── scripts/
│   │   ├── 01-setup-root-ca.sh     # Root CA initialization
│   │   ├── 02-setup-intermediate-ca.sh
│   │   └── 03-generate-license-keys.sh
│   ├── config/
│   │   ├── root-ca.cnf             # OpenSSL config for root CA
│   │   └── intermediate-ca.cnf     # OpenSSL config for intermediate
│   └── README.md
│
├── server/                          # PHP Server Implementation
│   ├── public/
│   │   └── index.php               # Entry point
│   ├── src/
│   │   ├── Api/
│   │   │   ├── CertificateController.php
│   │   │   ├── LicenseController.php
│   │   │   ├── MigrationController.php
│   │   │   └── PortalController.php
│   │   ├── Services/
│   │   │   ├── PrivateCAService.php
│   │   │   ├── EnrollmentTokenService.php
│   │   │   ├── LicenseTokenService.php
│   │   │   ├── LicenseRenewalService.php
│   │   │   ├── DeviceMigrationService.php
│   │   │   └── CertificateValidator.php
│   │   ├── Database/
│   │   │   └── Database.php
│   │   ├── Models/
│   │   │   ├── User.php
│   │   │   ├── Subscription.php
│   │   │   ├── Certificate.php
│   │   │   └── License.php
│   │   └── Middleware/
│   │       ├── TLSAuthMiddleware.php
│   │       └── MTLSAuthMiddleware.php
│   ├── config/
│   │   ├── config.php
│   │   └── routes.php
│   ├── database/
│   │   ├── migrations/
│   │   │   ├── 001_create_users_table.sql
│   │   │   ├── 002_create_subscriptions_table.sql
│   │   │   ├── 003_create_enrollment_tokens_table.sql
│   │   │   ├── 004_create_certificates_table.sql
│   │   │   ├── 005_create_clients_table.sql
│   │   │   ├── 006_create_licenses_table.sql
│   │   │   └── 007_create_migrations_table.sql
│   │   └── seeds/
│   │       └── test_data.sql
│   ├── tests/
│   │   ├── Unit/
│   │   ├── Integration/
│   │   └── bootstrap.php
│   ├── composer.json
│   ├── .env.example
│   ├── docker-compose.yml
│   └── README.md
│
├── client/                          # Client Implementations
│   ├── java-desktop/               # Java Desktop Reference Client
│   │   ├── src/
│   │   │   └── main/
│   │   │       └── java/
│   │   │           └── com/
│   │   │               └── licenseserver/
│   │   │                   └── client/
│   │   │                       ├── CertificateManager.java
│   │   │                       ├── LicenseManager.java
│   │   │                       ├── DeviceIdentifier.java
│   │   │                       ├── EnrollmentManager.java
│   │   │                       ├── MigrationManager.java
│   │   │                       ├── platform/
│   │   │                       │   ├── WindowsCertStore.java
│   │   │                       │   ├── MacOSKeychain.java
│   │   │                       │   └── LinuxCertStore.java
│   │   │                       └── ui/
│   │   │                           ├── MainWindow.java
│   │   │                           ├── EnrollmentDialog.java
│   │   │                           └── MigrationDialog.java
│   │   ├── resources/
│   │   │   ├── ca-chain.pem        # Embedded CA certificates
│   │   │   ├── license-server.pub  # Embedded license public key
│   │   │   └── config.properties
│   │   ├── pom.xml
│   │   └── README.md
│   │
│   └── flutter/                    # Flutter Multi-platform Client
│       ├── lib/
│       │   ├── main.dart
│       │   ├── services/
│       │   │   ├── certificate_manager.dart
│       │   │   ├── license_manager.dart
│       │   │   ├── device_identifier.dart
│       │   │   └── api_client.dart
│       │   ├── platform_channels/
│       │   │   └── certificate_channel.dart
│       │   └── ui/
│       │       ├── enrollment_screen.dart
│       │       ├── license_status_screen.dart
│       │       └── migration_screen.dart
│       ├── android/
│       │   └── app/
│       │       └── src/
│       │           └── main/
│       │               └── kotlin/
│       │                   └── CertificatePlugin.kt
│       ├── ios/
│       │   └── Runner/
│       │       └── CertificatePlugin.swift
│       ├── macos/
│       ├── windows/
│       ├── linux/
│       ├── pubspec.yaml
│       └── README.md
│
└── docs/
    ├── api-examples.md
    ├── certificate-operations.md
    └── troubleshooting.md
```

---

## 2. Development Environment Setup

### Server Development Environment

**Prerequisites:**
- PHP 8.1+ with extensions: openssl, pdo_mysql, json, mbstring
- MySQL 8.0+ or PostgreSQL 13+
- Composer 2.x
- Docker & Docker Compose (optional but recommended)

**Setup Steps:**

```bash
# Clone repository
git clone https://github.com/yourusername/sub-lic-spec.git
cd sub-lic-spec/server

# Install PHP dependencies
composer install

# Copy environment configuration
cp .env.example .env

# Edit .env with your settings
nano .env

# Start development environment with Docker
docker-compose up -d

# Run database migrations
php migrate.php

# Seed test data (optional)
php seed.php

# Start development server (if not using Docker)
php -S localhost:8000 -t public/
```

### Client Development Environment (Java)

**Prerequisites:**
- JDK 11+
- Maven 3.6+
- IDE with Java support (IntelliJ IDEA, Eclipse, VS Code)

**Setup Steps:**

```bash
cd client/java-desktop

# Install dependencies
mvn clean install

# Run tests
mvn test

# Run application
mvn exec:java -Dexec.mainClass="com.licenseserver.client.Main"

# Build distributable package
mvn package
```

### Client Development Environment (Flutter)

**Prerequisites:**
- Flutter SDK 3.0+
- Dart SDK 3.0+
- Platform-specific tools (Xcode for iOS/macOS, Android Studio for Android)

**Setup Steps:**

```bash
cd client/flutter

# Get dependencies
flutter pub get

# Run on desktop
flutter run -d windows  # or macos, linux

# Run on mobile
flutter run -d android  # or ios

# Build release
flutter build windows  # or macos, linux, apk, ios
```

---

## 3. Configuration Management

### Server Configuration (.env)

**File: `server/.env.example`**

```ini
# Application
APP_ENV=development
APP_DEBUG=true
APP_URL=https://license-server.local
APP_PORT=8443

# Database
DB_CONNECTION=mysql
DB_HOST=localhost
DB_PORT=3306
DB_DATABASE=license_system
DB_USERNAME=license_user
DB_PASSWORD=secure_password_here

# Private CA Configuration
CA_ROOT_CERT_PATH=/etc/ca/root-ca/root-ca.crt
CA_ROOT_KEY_PATH=/etc/ca/root-ca/root-ca.key
CA_ROOT_KEY_PASSWORD=root_ca_password_here

CA_INTERMEDIATE_CERT_PATH=/etc/ca/intermediate-ca/intermediate-ca.crt
CA_INTERMEDIATE_KEY_PATH=/etc/ca/intermediate-ca/intermediate-ca.key
CA_INTERMEDIATE_KEY_PASSWORD=intermediate_ca_password_here

CA_ISSUED_CERTS_DIR=/etc/ca/issued-certificates
CA_CRL_PATH=/var/www/license-server/public/crl/current.crl

# License Signing Keys
LICENSE_SIGNING_KEY_PATH=/etc/license-server/license-signing.key
LICENSE_SIGNING_KEY_PASSWORD=license_key_password_here
LICENSE_SIGNING_PUB_PATH=/etc/license-server/license-signing.pub

# TLS Configuration
TLS_CERT_PATH=/etc/ssl/certs/license-server.crt
TLS_KEY_PATH=/etc/ssl/private/license-server.key

# Security
JWT_SECRET=random_secret_key_here_change_in_production
ENCRYPTION_KEY=32_byte_encryption_key_here_change_in_production

# Grace Periods (days)
GRACE_PERIOD_MONTHLY=5
GRACE_PERIOD_ANNUAL=14

# Certificate Validity
CERTIFICATE_VALIDITY_YEARS=2
CERTIFICATE_RENEWAL_DAYS=30

# Rate Limiting
RATE_LIMIT_ENROLLMENT=10
RATE_LIMIT_LICENSE=100
RATE_LIMIT_WINDOW=3600

# Logging
LOG_LEVEL=debug
LOG_PATH=/var/log/license-server/app.log
AUDIT_LOG_PATH=/var/log/license-server/audit.log

# External Services (if applicable)
PAYMENT_PROVIDER_API_KEY=
PAYMENT_PROVIDER_WEBHOOK_SECRET=

# Email (for notifications)
MAIL_MAILER=smtp
MAIL_HOST=smtp.mailtrap.io
MAIL_PORT=2525
MAIL_USERNAME=
MAIL_PASSWORD=
MAIL_FROM_ADDRESS=noreply@license-server.com
```

### Client Configuration (Java)

**File: `client/java-desktop/src/main/resources/config.properties`**

```properties
# License Server
license.server.url=https://license-server.local:8443
license.server.verify.ssl=true

# Certificate Storage
cert.store.windows=Windows-MY
cert.store.macos=KeychainStore
cert.store.linux.path=${user.home}/.config/license-client/certs

# License Storage
license.storage.path=${user.home}/.config/license-client
license.storage.encrypted=true

# Renewal
license.renewal.check.days=7
license.renewal.check.interval.hours=24
license.renewal.background=true

# Migration
migration.token.validity.hours=24

# Logging
log.level=INFO
log.path=${user.home}/.config/license-client/logs

# UI
ui.theme=system
ui.notifications.enabled=true
ui.minimize.to.tray=true
```

### Client Configuration (Flutter)

**File: `client/flutter/lib/config/app_config.dart`**

```dart
class AppConfig {
  static const String licenseServerUrl = String.fromEnvironment(
    'LICENSE_SERVER_URL',
    defaultValue: 'https://license-server.local:8443',
  );
  
  static const bool verifySSL = bool.fromEnvironment(
    'VERIFY_SSL',
    defaultValue: true,
  );
  
  static const int renewalCheckDays = 7;
  static const int renewalCheckIntervalHours = 24;
  
  static const int migrationTokenValidityHours = 24;
  
  // Embedded CA certificate chain (Base64 encoded)
  static const String caCertificateChain = '''
-----BEGIN CERTIFICATE-----
MIIFxTCCA62gAwIBAgIUABCDEFGHIJKLMNOPQRSTUVWXYZab...
-----END CERTIFICATE-----
''';
  
  // Embedded license server public key (Base64 encoded)
  static const String licenseServerPublicKey = '''
-----BEGIN PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA...
-----END PUBLIC KEY-----
''';
}
```

---

## 4. Database Setup and Migrations

### Migration System

**File: `server/migrate.php`**

```php
<?php
require_once __DIR__ . '/vendor/autoload.php';

use App\Database\Database;

$db = Database::getInstance();

// Get all migration files
$migrationFiles = glob(__DIR__ . '/database/migrations/*.sql');
sort($migrationFiles);

// Create migrations tracking table
$db->exec("
    CREATE TABLE IF NOT EXISTS migrations (
        id INT AUTO_INCREMENT PRIMARY KEY,
        migration VARCHAR(255) NOT NULL UNIQUE,
        executed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    )
");

// Get executed migrations
$stmt = $db->query("SELECT migration FROM migrations");
$executed = $stmt->fetchAll(PDO::FETCH_COLUMN);

// Run pending migrations
foreach ($migrationFiles as $file) {
    $migration = basename($file);
    
    if (in_array($migration, $executed)) {
        echo "Skipping: $migration (already executed)\n";
        continue;
    }
    
    echo "Running: $migration\n";
    
    $sql = file_get_contents($file);
    
    try {
        $db->beginTransaction();
        $db->exec($sql);
        $db->exec("INSERT INTO migrations (migration) VALUES (?)", [$migration]);
        $db->commit();
        echo "Success: $migration\n";
    } catch (Exception $e) {
        $db->rollback();
        echo "Error in $migration: " . $e->getMessage() . "\n";
        exit(1);
    }
}

echo "All migrations completed successfully!\n";
```

### Migration Files

**File: `server/database/migrations/001_create_users_table.sql`**

```sql
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    organization VARCHAR(255),
    password_hash VARCHAR(255) NOT NULL,
    status ENUM('active', 'suspended', 'deleted') DEFAULT 'active',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    INDEX idx_email (email),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**File: `server/database/migrations/002_create_subscriptions_table.sql`**

```sql
CREATE TABLE subscriptions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    subscription_type ENUM('monthly', 'annual') NOT NULL,
    start_date DATETIME NOT NULL,
    end_date DATETIME NOT NULL,
    payment_status ENUM('active', 'pending', 'expired', 'cancelled') DEFAULT 'pending',
    payment_provider VARCHAR(50),
    payment_provider_id VARCHAR(255),
    auto_renew BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_end_date (end_date),
    INDEX idx_payment_status (payment_status),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**Additional migration files (003-007) follow the schemas defined in spec.md Section 8.3 and 8.4**

---

## 5. Private CA Setup

### Root CA Setup Script

**File: `ca/scripts/01-setup-root-ca.sh`**

```bash
#!/bin/bash
set -e

CA_DIR="/etc/ca"
ROOT_CA_DIR="$CA_DIR/root-ca"

echo "Setting up Root CA..."

# Create directory structure
mkdir -p "$ROOT_CA_DIR"/{private,certs,crl,newcerts}
chmod 700 "$ROOT_CA_DIR/private"

# Create database files
touch "$ROOT_CA_DIR/index.txt"
echo "1000" > "$ROOT_CA_DIR/serial"

# Generate root CA private key (4096-bit, AES-256 encrypted)
openssl genrsa -aes256 -out "$ROOT_CA_DIR/private/root-ca.key" 4096
chmod 400 "$ROOT_CA_DIR/private/root-ca.key"

# Generate root CA certificate (20 year validity)
openssl req -config ../config/root-ca.cnf \
    -key "$ROOT_CA_DIR/private/root-ca.key" \
    -new -x509 -days 7300 -sha256 -extensions v3_ca \
    -out "$ROOT_CA_DIR/certs/root-ca.crt"

# Verify certificate
openssl x509 -noout -text -in "$ROOT_CA_DIR/certs/root-ca.crt"

echo "Root CA setup complete!"
echo "Certificate: $ROOT_CA_DIR/certs/root-ca.crt"
echo "IMPORTANT: Store the private key securely offline!"
```

**File: `ca/config/root-ca.cnf`**

```ini
[ ca ]
default_ca = CA_default

[ CA_default ]
dir               = /etc/ca/root-ca
certs             = $dir/certs
crl_dir           = $dir/crl
new_certs_dir     = $dir/newcerts
database          = $dir/index.txt
serial            = $dir/serial
RANDFILE          = $dir/private/.rand

private_key       = $dir/private/root-ca.key
certificate       = $dir/certs/root-ca.crt

crlnumber         = $dir/crlnumber
crl               = $dir/crl/root-ca.crl
crl_extensions    = crl_ext
default_crl_days  = 30

default_md        = sha256
name_opt          = ca_default
cert_opt          = ca_default
default_days      = 3650
preserve          = no
policy            = policy_strict

[ policy_strict ]
countryName             = match
stateOrProvinceName     = match
organizationName        = match
organizationalUnitName  = optional
commonName              = supplied
emailAddress            = optional

[ req ]
default_bits        = 4096
distinguished_name  = req_distinguished_name
string_mask         = utf8only
default_md          = sha256
x509_extensions     = v3_ca

[ req_distinguished_name ]
countryName                     = Country Name (2 letter code)
stateOrProvinceName             = State or Province Name
localityName                    = Locality Name
0.organizationName              = Organization Name
organizationalUnitName          = Organizational Unit Name
commonName                      = Common Name
emailAddress                    = Email Address

countryName_default             = US
stateOrProvinceName_default     = State
localityName_default            = City
0.organizationName_default      = Your Organization
organizationalUnitName_default  = Certificate Authority
commonName_default              = Root CA

[ v3_ca ]
subjectKeyIdentifier = hash
authorityKeyIdentifier = keyid:always,issuer
basicConstraints = critical, CA:true
keyUsage = critical, digitalSignature, cRLSign, keyCertSign
```

### Intermediate CA Setup Script

**File: `ca/scripts/02-setup-intermediate-ca.sh`**

```bash
#!/bin/bash
set -e

CA_DIR="/etc/ca"
ROOT_CA_DIR="$CA_DIR/root-ca"
INT_CA_DIR="$CA_DIR/intermediate-ca"

echo "Setting up Intermediate CA..."

# Create directory structure
mkdir -p "$INT_CA_DIR"/{private,certs,crl,newcerts,csr}
chmod 700 "$INT_CA_DIR/private"

# Create database files
touch "$INT_CA_DIR/index.txt"
echo "1000" > "$INT_CA_DIR/serial"
echo "1000" > "$INT_CA_DIR/crlnumber"

# Generate intermediate CA private key
openssl genrsa -aes256 -out "$INT_CA_DIR/private/intermediate-ca.key" 4096
chmod 400 "$INT_CA_DIR/private/intermediate-ca.key"

# Generate intermediate CA CSR
openssl req -config ../config/intermediate-ca.cnf -new -sha256 \
    -key "$INT_CA_DIR/private/intermediate-ca.key" \
    -out "$INT_CA_DIR/csr/intermediate-ca.csr"

# Sign intermediate certificate with root CA (10 year validity)
openssl ca -config ../config/root-ca.cnf -extensions v3_intermediate_ca \
    -days 3650 -notext -md sha256 \
    -in "$INT_CA_DIR/csr/intermediate-ca.csr" \
    -out "$INT_CA_DIR/certs/intermediate-ca.crt"

chmod 444 "$INT_CA_DIR/certs/intermediate-ca.crt"

# Create certificate chain file
cat "$INT_CA_DIR/certs/intermediate-ca.crt" \
    "$ROOT_CA_DIR/certs/root-ca.crt" > "$INT_CA_DIR/certs/ca-chain.crt"

# Verify certificate chain
openssl verify -CAfile "$ROOT_CA_DIR/certs/root-ca.crt" \
    "$INT_CA_DIR/certs/intermediate-ca.crt"

echo "Intermediate CA setup complete!"
echo "Certificate: $INT_CA_DIR/certs/intermediate-ca.crt"
echo "Chain: $INT_CA_DIR/certs/ca-chain.crt"
```

**File: `ca/config/intermediate-ca.cnf`**

```ini
[ ca ]
default_ca = CA_default

[ CA_default ]
dir               = /etc/ca/intermediate-ca
certs             = $dir/certs
crl_dir           = $dir/crl
new_certs_dir     = $dir/newcerts
database          = $dir/index.txt
serial            = $dir/serial
RANDFILE          = $dir/private/.rand

private_key       = $dir/private/intermediate-ca.key
certificate       = $dir/certs/intermediate-ca.crt

crlnumber         = $dir/crlnumber
crl               = $dir/crl/intermediate-ca.crl
crl_extensions    = crl_ext
default_crl_days  = 30

default_md        = sha256
name_opt          = ca_default
cert_opt          = ca_default
default_days      = 730
preserve          = no
policy            = policy_loose

[ policy_loose ]
countryName             = optional
stateOrProvinceName     = optional
localityName            = optional
organizationName        = optional
organizationalUnitName  = optional
commonName              = supplied
emailAddress            = optional

[ req ]
default_bits        = 4096
distinguished_name  = req_distinguished_name
string_mask         = utf8only
default_md          = sha256
x509_extensions     = v3_intermediate_ca

[ req_distinguished_name ]
countryName                     = Country Name (2 letter code)
stateOrProvinceName             = State or Province Name
localityName                    = Locality Name
0.organizationName              = Organization Name
organizationalUnitName          = Organizational Unit Name
commonName                      = Common Name
emailAddress                    = Email Address

countryName_default             = US
stateOrProvinceName_default     = State
localityName_default            = City
0.organizationName_default      = Your Organization
organizationalUnitName_default  = Certificate Authority
commonName_default              = Intermediate CA

[ v3_intermediate_ca ]
subjectKeyIdentifier = hash
authorityKeyIdentifier = keyid:always,issuer
basicConstraints = critical, CA:true, pathlen:0
keyUsage = critical, digitalSignature, cRLSign, keyCertSign

[ usr_cert ]
basicConstraints = CA:FALSE
nsCertType = client, email
nsComment = "OpenSSL Generated Client Certificate"
subjectKeyIdentifier = hash
authorityKeyIdentifier = keyid,issuer
keyUsage = critical, nonRepudiation, digitalSignature, keyEncipherment
extendedKeyUsage = clientAuth, emailProtection

[ client_cert ]
basicConstraints = CA:FALSE
nsCertType = client
nsComment = "License Client Certificate"
subjectKeyIdentifier = hash
authorityKeyIdentifier = keyid,issuer
keyUsage = critical, digitalSignature, keyEncipherment
extendedKeyUsage = clientAuth

[ crl_ext ]
authorityKeyIdentifier=keyid:always

[ ocsp ]
basicConstraints = CA:FALSE
subjectKeyIdentifier = hash
authorityKeyIdentifier = keyid,issuer
keyUsage = critical, digitalSignature
extendedKeyUsage = critical, OCSPSigning
```

### License Signing Keys Setup

**File: `ca/scripts/03-generate-license-keys.sh`**

```bash
#!/bin/bash
set -e

LICENSE_KEY_DIR="/etc/license-server"

echo "Generating license signing keys..."

mkdir -p "$LICENSE_KEY_DIR"
chmod 700 "$LICENSE_KEY_DIR"

# Generate license signing private key (2048-bit for JWT)
openssl genrsa -aes256 -out "$LICENSE_KEY_DIR/license-signing.key" 2048
chmod 400 "$LICENSE_KEY_DIR/license-signing.key"

# Extract public key
openssl rsa -in "$LICENSE_KEY_DIR/license-signing.key" \
    -pubout -out "$LICENSE_KEY_DIR/license-signing.pub"
chmod 444 "$LICENSE_KEY_DIR/license-signing.pub"

echo "License signing keys generated!"
echo "Private key: $LICENSE_KEY_DIR/license-signing.key"
echo "Public key: $LICENSE_KEY_DIR/license-signing.pub"
echo ""
echo "IMPORTANT: Embed the public key in client applications!"
```

---

## 6. Server Implementation Details

### Composer Dependencies

**File: `server/composer.json`**

```json
{
    "name": "license-server/subscription-licensing",
    "description": "Subscription licensing system with certificate authentication",
    "type": "project",
    "license": "MIT",
    "require": {
        "php": ">=8.1",
        "ext-openssl": "*",
        "ext-pdo": "*",
        "ext-json": "*",
        "ext-mbstring": "*",
        "firebase/php-jwt": "^6.0",
        "vlucas/phpdotenv": "^5.5",
        "monolog/monolog": "^3.0",
        "ramsey/uuid": "^4.7"
    },
    "require-dev": {
        "phpunit/phpunit": "^10.0",
        "phpstan/phpstan": "^1.10",
        "squizlabs/php_codesniffer": "^3.7"
    },
    "autoload": {
        "psr-4": {
            "App\\": "src/"
        }
    },
    "autoload-dev": {
        "psr-4": {
            "Tests\\": "tests/"
        }
    },
    "scripts": {
        "test": "phpunit",
        "test:coverage": "phpunit --coverage-html coverage",
        "lint": "phpcs --standard=PSR12 src/",
        "analyze": "phpstan analyse src/ --level=8"
    }
}
```

### Router Implementation

**File: `server/config/routes.php`**

```php
<?php

use App\Api\CertificateController;
use App\Api\LicenseController;
use App\Api\MigrationController;
use App\Api\PortalController;
use App\Middleware\TLSAuthMiddleware;
use App\Middleware\MTLSAuthMiddleware;

return [
    // Portal endpoints (session auth)
    ['GET', '/portal/enrollment', [PortalController::class, 'showEnrollment']],
    ['POST', '/portal/enrollment/generate', [PortalController::class, 'generateToken']],
    ['DELETE', '/portal/enrollment/revoke', [PortalController::class, 'revokeToken']],
    ['GET', '/portal/enrollment/status', [PortalController::class, 'tokenStatus']],
    ['POST', '/portal/account/delete/confirm', [PortalController::class, 'deleteAccount']],
    
    // Certificate endpoints (TLS + token auth)
    ['POST', '/api/certificate/enroll', [CertificateController::class, 'enroll'], [TLSAuthMiddleware::class]],
    ['GET', '/api/certificate/status', [CertificateController::class, 'status'], [TLSAuthMiddleware::class]],
    ['GET', '/api/crl/current', [CertificateController::class, 'getCRL']],
    
    // License endpoints (mTLS required)
    ['POST', '/api/license/activate', [LicenseController::class, 'activate'], [MTLSAuthMiddleware::class]],
    ['POST', '/api/license/renew', [LicenseController::class, 'renew'], [MTLSAuthMiddleware::class]],
    ['GET', '/api/license/status', [LicenseController::class, 'status'], [MTLSAuthMiddleware::class]],
    
    // Migration endpoints (mTLS required)
    ['POST', '/api/license/migrate/initiate', [MigrationController::class, 'initiate'], [MTLSAuthMiddleware::class]],
    ['POST', '/api/license/migrate/complete', [MigrationController::class, 'complete'], [MTLSAuthMiddleware::class]],
    
    // Subscription endpoints (mTLS required)
    ['GET', '/api/subscription/status', [LicenseController::class, 'subscriptionStatus'], [MTLSAuthMiddleware::class]],
];
```

### Entry Point

**File: `server/public/index.php`**

```php
<?php
require_once __DIR__ . '/../vendor/autoload.php';

use Dotenv\Dotenv;
use App\Database\Database;
use Monolog\Logger;
use Monolog\Handler\StreamHandler;

// Load environment configuration
$dotenv = Dotenv::createImmutable(__DIR__ . '/..');
$dotenv->load();

// Initialize logger
$logger = new Logger('license-server');
$logger->pushHandler(new StreamHandler($_ENV['LOG_PATH'] ?? 'php://stdout', $_ENV['LOG_LEVEL'] ?? Logger::DEBUG));

// Initialize database
Database::initialize([
    'host' => $_ENV['DB_HOST'],
    'database' => $_ENV['DB_DATABASE'],
    'username' => $_ENV['DB_USERNAME'],
    'password' => $_ENV['DB_PASSWORD'],
]);

// Load routes
$routes = require __DIR__ . '/../config/routes.php';

// Simple router
$requestMethod = $_SERVER['REQUEST_METHOD'];
$requestUri = parse_url($_SERVER['REQUEST_URI'], PHP_URL_PATH);

foreach ($routes as $route) {
    [$method, $path, $handler, $middleware] = array_pad($route, 4, []);
    
    if ($method === $requestMethod && $path === $requestUri) {
        try {
            // Execute middleware
            foreach ($middleware as $mw) {
                $middlewareInstance = new $mw();
                $middlewareInstance->handle();
            }
            
            // Execute handler
            [$controller, $action] = $handler;
            $controllerInstance = new $controller($logger);
            $response = $controllerInstance->$action();
            
            // Send response
            header('Content-Type: application/json');
            echo json_encode($response);
            exit;
            
        } catch (Exception $e) {
            $logger->error('Request failed', [
                'error' => $e->getMessage(),
                'trace' => $e->getTraceAsString()
            ]);
            
            http_response_code(500);
            echo json_encode(['error' => 'Internal server error']);
            exit;
        }
    }
}

// 404 Not Found
http_response_code(404);
echo json_encode(['error' => 'Not found']);
```

### Docker Compose Configuration

**File: `server/docker-compose.yml`**

```yaml
version: '3.8'

services:
  web:
    image: php:8.1-apache
    container_name: license-server-web
    ports:
      - "8443:443"
      - "8080:80"
    volumes:
      - ./:/var/www/html
      - ./docker/apache/vhost.conf:/etc/apache2/sites-available/000-default.conf
      - ./docker/ssl:/etc/ssl/certs
      - ../ca:/etc/ca:ro
    environment:
      - APACHE_RUN_USER=www-data
      - APACHE_RUN_GROUP=www-data
    depends_on:
      - db
    networks:
      - license-network

  db:
    image: mysql:8.0
    container_name: license-server-db
    ports:
      - "3306:3306"
    environment:
      MYSQL_ROOT_PASSWORD: root_password
      MYSQL_DATABASE: license_system
      MYSQL_USER: license_user
      MYSQL_PASSWORD: license_password
    volumes:
      - db-data:/var/lib/mysql
      - ./database/migrations:/docker-entrypoint-initdb.d
    networks:
      - license-network

  phpmyadmin:
    image: phpmyadmin:latest
    container_name: license-server-phpmyadmin
    ports:
      - "8081:80"
    environment:
      PMA_HOST: db
      PMA_USER: license_user
      PMA_PASSWORD: license_password
    depends_on:
      - db
    networks:
      - license-network

volumes:
  db-data:

networks:
  license-network:
    driver: bridge
```

---

## 7. Client Implementation Details

### Java Desktop Client - Maven Configuration

**File: `client/java-desktop/pom.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.licenseserver</groupId>
    <artifactId>license-client</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <name>License Client</name>
    <description>Desktop client for subscription licensing system</description>

    <properties>
        <maven.compiler.source>11</maven.compiler.source>
        <maven.compiler.target>11</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>
        <!-- Bouncy Castle for certificate operations -->
        <dependency>
            <groupId>org.bouncycastle</groupId>
            <artifactId>bcprov-jdk15on</artifactId>
            <version>1.70</version>
        </dependency>
        <dependency>
            <groupId>org.bouncycastle</groupId>
            <artifactId>bcpkix-jdk15on</artifactId>
            <version>1.70</version>
        </dependency>

        <!-- JWT handling -->
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>0.11.5</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>0.11.5</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>0.11.5</version>
            <scope>runtime</scope>
        </dependency>

        <!-- HTTP client -->
        <dependency>
            <groupId>com.squareup.okhttp3</groupId>
            <artifactId>okhttp</artifactId>
            <version>4.11.0</version>
        </dependency>

        <!-- JSON processing -->
        <dependency>
            <groupId>com.google.code.gson</groupId>
            <artifactId>gson</artifactId>
            <version>2.10.1</version>
        </dependency>

        <!-- Logging -->
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
            <version>2.0.7</version>
        </dependency>
        <dependency>
            <groupId>ch.qos.logback</groupId>
            <artifactId>logback-classic</artifactId>
            <version>1.4.8</version>
        </dependency>

        <!-- Platform-specific (Windows) -->
        <dependency>
            <groupId>net.java.dev.jna</groupId>
            <artifactId>jna</artifactId>
            <version>5.13.0</version>
        </dependency>
        <dependency>
            <groupId>net.java.dev.jna</groupId>
            <artifactId>jna-platform</artifactId>
            <version>5.13.0</version>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>5.9.3</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-core</artifactId>
            <version>5.3.1</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- Compiler plugin -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
            </plugin>

            <!-- JAR with dependencies -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-assembly-plugin</artifactId>
                <version>3.6.0</version>
                <configuration>
                    <archive>
                        <manifest>
                            <mainClass>com.licenseserver.client.Main</mainClass>
                        </manifest>
                    </archive>
                    <descriptorRefs>
                        <descriptorRef>jar-with-dependencies</descriptorRef>
                    </descriptorRefs>
                </configuration>
                <executions>
                    <execution>
                        <id>make-assembly</id>
                        <phase>package</phase>
                        <goals>
                            <goal>single</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>

            <!-- JPackage for native installers -->
            <plugin>
                <groupId>org.panteleyev</groupId>
                <artifactId>jpackage-maven-plugin</artifactId>
                <version>1.6.0</version>
                <configuration>
                    <name>LicenseClient</name>
                    <appVersion>1.0.0</appVersion>
                    <vendor>Your Organization</vendor>
                    <destination>target/dist</destination>
                    <module>license.client/com.licenseserver.client.Main</module>
                    <runtimeImage>target/runtime-image</runtimeImage>
                    <javaOptions>
                        <option>-Dfile.encoding=UTF-8</option>
                    </javaOptions>
                    <winDirChooser>true</winDirChooser>
                    <winMenu>true</winMenu>
                    <winShortcut>true</winShortcut>
                </configuration>
            </plugin>

            <!-- Test plugin -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.1.2</version>
            </plugin>
        </plugins>
    </build>
</project>
```

### Flutter Client - Dependencies

**File: `client/flutter/pubspec.yaml`**

```yaml
name: license_client
description: Multi-platform client for subscription licensing system
version: 1.0.0+1
publish_to: 'none'

environment:
  sdk: '>=3.0.0 <4.0.0'

dependencies:
  flutter:
    sdk: flutter
  
  # Certificate and crypto operations
  pointycastle: ^3.7.3
  asn1lib: ^1.5.0
  
  # JWT handling
  dart_jsonwebtoken: ^2.12.0
  
  # HTTP client with certificate pinning
  dio: ^5.3.2
  
  # Secure storage
  flutter_secure_storage: ^9.0.0
  
  # Platform channels
  flutter_platform_interface: ^2.0.0
  
  # Device info
  device_info_plus: ^9.0.3
  platform_device_id: ^1.0.1
  
  # File operations
  path_provider: ^2.1.0
  path: ^1.8.3
  
  # State management
  provider: ^6.0.5
  
  # UI
  cupertino_icons: ^1.0.6
  
  # Logging
  logger: ^2.0.2

dev_dependencies:
  flutter_test:
    sdk: flutter
  flutter_lints: ^2.0.3
  mockito: ^5.4.2
  build_runner: ^2.4.6

flutter:
  uses-material-design: true
  
  assets:
    - assets/certs/ca-chain.pem
    - assets/keys/license-server.pub
  
  # Platform-specific configuration
  fonts:
    - family: Roboto
      fonts:
        - asset: fonts/Roboto-Regular.ttf
        - asset: fonts/Roboto-Bold.ttf
          weight: 700
```

---

## 8. Build and Packaging

### Server Deployment Package

**File: `server/build.sh`**

```bash
#!/bin/bash
set -e

VERSION="1.0.0"
BUILD_DIR="build"
DIST_DIR="dist"

echo "Building License Server v${VERSION}..."

# Clean previous builds
rm -rf "$BUILD_DIR" "$DIST_DIR"
mkdir -p "$BUILD_DIR" "$DIST_DIR"

# Install production dependencies
composer install --no-dev --optimize-autoloader

# Copy application files
cp -r src/ "$BUILD_DIR/"
cp -r public/ "$BUILD_DIR/"
cp -r config/ "$BUILD_DIR/"
cp -r database/ "$BUILD_DIR/"
cp -r vendor/ "$BUILD_DIR/"
cp composer.json "$BUILD_DIR/"
cp .env.example "$BUILD_DIR/"

# Create tarball
cd "$BUILD_DIR"
tar -czf "../${DIST_DIR}/license-server-${VERSION}.tar.gz" .
cd ..

# Create installation script
cat > "${DIST_DIR}/install.sh" << 'EOF'
#!/bin/bash
set -e

echo "Installing License Server..."

# Extract files
tar -xzf license-server-*.tar.gz -C /var/www/license-server/

# Set permissions
chown -R www-data:www-data /var/www/license-server
chmod -R 755 /var/www/license-server
chmod -R 700 /var/www/license-server/config

# Copy environment file
cp /var/www/license-server/.env.example /var/www/license-server/.env

echo "Installation complete!"
echo "Please edit /var/www/license-server/.env with your configuration"
echo "Then run: php /var/www/license-server/migrate.php"
EOF

chmod +x "${DIST_DIR}/install.sh"

echo "Build complete: ${DIST_DIR}/license-server-${VERSION}.tar.gz"
```

### Java Client Build Scripts

**File: `client/java-desktop/build.sh`**

```bash
#!/bin/bash
set -e

VERSION="1.0.0"
APP_NAME="LicenseClient"

echo "Building ${APP_NAME} v${VERSION}..."

# Clean and build
mvn clean package

# Create runtime image with jlink
jlink --add-modules java.base,java.desktop,java.sql,java.naming,java.management,jdk.crypto.ec \
      --output target/runtime-image \
      --strip-debug \
      --no-man-pages \
      --no-header-files \
      --compress=2

# Build platform-specific installers
if [[ "$OSTYPE" == "msys" || "$OSTYPE" == "win32" ]]; then
    echo "Building Windows installer..."
    mvn jpackage:jpackage -Pwindows
elif [[ "$OSTYPE" == "darwin"* ]]; then
    echo "Building macOS installer..."
    mvn jpackage:jpackage -Pmacos
else
    echo "Building Linux installer..."
    mvn jpackage:jpackage -Plinux
fi

echo "Build complete! Check target/dist/"
```

### Flutter Build Scripts

**File: `client/flutter/build.sh`**

```bash
#!/bin/bash
set -e

VERSION="1.0.0"
BUILD_NUMBER="1"

echo "Building License Client v${VERSION}..."

# Get dependencies
flutter pub get

# Run code generation
flutter pub run build_runner build --delete-conflicting-outputs

# Build for all desktop platforms
echo "Building for Windows..."
flutter build windows --release

echo "Building for macOS..."
flutter build macos --release

echo "Building for Linux..."
flutter build linux --release

# Build for mobile platforms
echo "Building for Android..."
flutter build apk --release
flutter build appbundle --release

echo "Building for iOS..."
flutter build ios --release --no-codesign

echo "Builds complete!"
echo "Windows: build/windows/runner/Release/"
echo "macOS: build/macos/Build/Products/Release/"
echo "Linux: build/linux/x64/release/bundle/"
echo "Android APK: build/app/outputs/flutter-apk/"
echo "Android Bundle: build/app/outputs/bundle/release/"
echo "iOS: build/ios/iphoneos/"
```

---

## 9. Testing Strategy

### Server Unit Tests

**File: `server/tests/Unit/LicenseTokenServiceTest.php`**

```php
<?php
namespace Tests\Unit;

use PHPUnit\Framework\TestCase;
use App\Services\LicenseTokenService;

class LicenseTokenServiceTest extends TestCase
{
    private $service;
    
    protected function setUp(): void
    {
        $this->service = new LicenseTokenService();
    }
    
    public function testGenerateLicenseToken()
    {
        $clientFingerprint = 'test-fingerprint';
        $certSerial = 'test-serial';
        $deviceId = 'test-device';
        $subscription = [
            'id' => 1,
            'subscription_type' => 'monthly',
            'end_date' => '2025-12-31 23:59:59',
            'payment_status' => 'active'
        ];
        
        $token = $this->service->generateLicenseToken(
            $clientFingerprint,
            $certSerial,
            $deviceId,
            $subscription
        );
        
        $this->assertNotEmpty($token);
        $this->assertStringContainsString('.', $token); // JWT format
        
        // Verify token structure
        $parts = explode('.', $token);
        $this->assertCount(3, $parts); // Header.Payload.Signature
    }
    
    public function testCalculateGracePeriod()
    {
        $subscriptionEnd = new \DateTime('2025-01-01');
        
        // Monthly subscription - 5 days grace
        $graceEnd = $this->service->calculateGracePeriodEnd('monthly', $subscriptionEnd);
        $this->assertEquals('2025-01-06', $graceEnd->format('Y-m-d'));
        
        // Annual subscription - 14 days grace
        $graceEnd = $this->service->calculateGracePeriodEnd('annual', $subscriptionEnd);
        $this->assertEquals('2025-01-15', $graceEnd->format('Y-m-d'));
    }
}
```

### Server Integration Tests

**File: `server/tests/Integration/CertificateEnrollmentTest.php`**

```php
<?php
namespace Tests\Integration;

use PHPUnit\Framework\TestCase;
use App\Api\CertificateController;
use App\Services\EnrollmentTokenService;

class CertificateEnrollmentTest extends TestCase
{
    private $db;
    private $controller;
    private $tokenService;
    
    protected function setUp(): void
    {
        // Setup test database
        $this->db = new \PDO('mysql:host=localhost;dbname=license_test', 'root', 'password');
        $this->db->exec("TRUNCATE TABLE enrollment_tokens");
        $this->db->exec("TRUNCATE TABLE issued_certificates");
        
        $this->tokenService = new EnrollmentTokenService($this->db);
        $this->controller = new CertificateController();
    }
    
    public function testCompleteEnrollmentFlow()
    {
        // 1. Generate enrollment token
        $tokenData = $this->tokenService->generateToken([
            'user_id' => 1,
            'email' => 'test@example.com',
            'full_name' => 'Test User',
            'subscription_type' => 'monthly',
            'subscription_id' => 1
        ]);
        
        $this->assertArrayHasKey('token', $tokenData);
        $token = $tokenData['token'];
        
        // 2. Generate CSR (simulate client)
        $csr = $this->generateTestCSR();
        
        // 3. Submit enrollment request
        $_POST['enrollment_token'] = $token;
        $_POST['csr'] = $csr;
        $_POST['device_id'] = 'test-device-123';
        
        $response = $this->controller->enroll();
        
        $this->assertArrayHasKey('certificate', $response);
        $this->assertArrayHasKey('license_token', $response);
        $this->assertArrayHasKey('ca_chain', $response);
        
        // 4. Verify token was marked as used
        $stmt = $this->db->prepare("SELECT used_count FROM enrollment_tokens WHERE token = ?");
        $stmt->execute([$token]);
        $used = $stmt->fetchColumn();
        
        $this->assertEquals(1, $used);
    }
    
    private function generateTestCSR()
    {
        // Generate test CSR using OpenSSL
        $privateKey = openssl_pkey_new([
            'private_key_bits' => 2048,
            'private_key_type' => OPENSSL_KEYTYPE_RSA,
        ]);
        
        $dn = [
            "countryName" => "US",
            "stateOrProvinceName" => "California",
            "localityName" => "San Francisco",
            "organizationName" => "Test Org",
            "commonName" => "Test Client"
        ];
        
        $csr = openssl_csr_new($dn, $privateKey, ['digest_alg' => 'sha256']);
        openssl_csr_export($csr, $csrOut);
        
        return $csrOut;
    }
}
```

### Client Unit Tests (Java)

**File: `client/java-desktop/src/test/java/com/licenseserver/client/DeviceIdentifierTest.java`**

```java
package com.licenseserver.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DeviceIdentifierTest {
    
    @Test
    public void testGenerateDeviceId() {
        DeviceIdentifier identifier = new DeviceIdentifier();
        String deviceId = identifier.generateDeviceId();
        
        assertNotNull(deviceId);
        assertTrue(deviceId.startsWith("device_"));
        assertEquals(71, deviceId.length()); // "device_" + 64 char SHA-256 hash
    }
    
    @Test
    public void testDeviceIdConsistency() {
        DeviceIdentifier identifier = new DeviceIdentifier();
        String deviceId1 = identifier.generateDeviceId();
        String deviceId2 = identifier.generateDeviceId();
        
        // Same device should generate same ID
        assertEquals(deviceId1, deviceId2);
    }
    
    @Test
    public void testPlatformSpecificGeneration() {
        DeviceIdentifier identifier = new DeviceIdentifier();
        String os = System.getProperty("os.name").toLowerCase();
        
        String deviceId = identifier.generateDeviceId();
        
        // Verify appropriate method was called for platform
        assertNotNull(deviceId);
        
        if (os.contains("win")) {
            // Windows should use MachineGuid
            assertTrue(identifier.wasWindowsMethodUsed());
        } else if (os.contains("mac")) {
            // macOS should use Hardware UUID
            assertTrue(identifier.wasMacOSMethodUsed());
        } else if (os.contains("nix") || os.contains("nux")) {
            // Linux should use machine-id
            assertTrue(identifier.wasLinuxMethodUsed());
        }
    }
}
```

### Client Integration Tests (Java)

**File: `client/java-desktop/src/test/java/com/licenseserver/client/EnrollmentIntegrationTest.java`**

```java
package com.licenseserver.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

public class EnrollmentIntegrationTest {
    
    private EnrollmentManager enrollmentManager;
    private String testServerUrl = "https://localhost:8443";
    
    @BeforeEach
    public void setUp() {
        enrollmentManager = new EnrollmentManager(testServerUrl);
    }
    
    @Test
    public void testCompleteEnrollmentFlow() throws Exception {
        // This test requires a running test server
        String enrollmentToken = "test-token-12345";
        
        EnrollmentResult result = enrollmentManager.enrollWithToken(enrollmentToken);
        
        assertTrue(result.isSuccess());
        assertNotNull(result.getCertificate());
        assertNotNull(result.getLicenseToken());
        
        // Verify certificate was installed
        CertificateManager certManager = new CertificateManager();
        X509Certificate cert = certManager.getClientCertificate();
        assertNotNull(cert);
        
        // Verify license token was stored
        LicenseManager licenseManager = new LicenseManager();
        String storedToken = licenseManager.getStoredToken();
        assertNotNull(storedToken);
    }
}
```

### Test Data Seeds

**File: `server/database/seeds/test_data.sql`**

```sql
-- Test users
INSERT INTO users (email, full_name, organization, password_hash, status) VALUES
('test1@example.com', 'Test User 1', 'Test Org 1', '$2y$10$test hash here', 'active'),
('test2@example.com', 'Test User 2', 'Test Org 2', '$2y$10$test hash here', 'active'),
('expired@example.com', 'Expired User', 'Test Org 3', '$2y$10$test hash here', 'active');

-- Test subscriptions
INSERT INTO subscriptions (user_id, subscription_type, start_date, end_date, payment_status) VALUES
(1, 'monthly', '2025-01-01 00:00:00', '2025-12-31 23:59:59', 'active'),
(2, 'annual', '2025-01-01 00:00:00', '2025-12-31 23:59:59', 'active'),
(3, 'monthly', '2024-01-01 00:00:00', '2024-12-31 23:59:59', 'expired');

-- Test enrollment tokens (not yet used)
INSERT INTO enrollment_tokens (token, user_id, subscriber_email, subscriber_name, subscription_type, subscription_id, expires_at) VALUES
('test-token-valid-12345', 1, 'test1@example.com', 'Test User 1', 'monthly', 1, DATE_ADD(NOW(), INTERVAL 7 DAY)),
('test-token-expired-67890', 2, 'test2@example.com', 'Test User 2', 'annual', 2, DATE_SUB(NOW(), INTERVAL 1 DAY));
```

---

## 10. Deployment

### Production Server Deployment

**File: `docs/deployment-production.md`**

```markdown
# Production Deployment Guide

## Prerequisites

- Ubuntu 20.04+ LTS or similar Linux distribution
- Root or sudo access
- Domain name with DNS configured
- SSL certificate from public CA (Let's Encrypt recommended)

## Step 1: System Preparation

```bash
# Update system
sudo apt update && sudo apt upgrade -y

# Install dependencies
sudo apt install -y php8.1 php8.1-cli php8.1-fpm php8.1-mysql \
    php8.1-openssl php8.1-mbstring php8.1-json \
    nginx mysql-server composer git

# Install OpenSSL for CA operations
sudo apt install -y openssl
```

## Step 2: Database Setup

```bash
# Secure MySQL installation
sudo mysql_secure_installation

# Create database and user
sudo mysql <<EOF
CREATE DATABASE license_system CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'license_user'@'localhost' IDENTIFIED BY 'STRONG_PASSWORD_HERE';
GRANT ALL PRIVILEGES ON license_system.* TO 'license_user'@'localhost';
FLUSH PRIVILEGES;
EOF
```

## Step 3: Private CA Setup

```bash
# Create CA directories
sudo mkdir -p /etc/ca/{root-ca,intermediate-ca}
sudo chmod 700 /etc/ca

# Run CA setup scripts
cd /path/to/ca/scripts
sudo bash 01-setup-root-ca.sh
sudo bash 02-setup-intermediate-ca.sh
sudo bash 03-generate-license-keys.sh

# Store root CA key offline
sudo cp /etc/ca/root-ca/private/root-ca.key /media/secure-usb/
sudo rm /etc/ca/root-ca/private/root-ca.key
```

## Step 4: Application Deployment

```bash
# Create application directory
sudo mkdir -p /var/www/license-server
sudo chown www-data:www-data /var/www/license-server

# Deploy application
cd /var/www/license-server
sudo -u www-data tar -xzf /path/to/license-server-1.0.0.tar.gz

# Configure environment
sudo -u www-data cp .env.example .env
sudo -u www-data nano .env  # Edit configuration

# Run migrations
sudo -u www-data php migrate.php

# Set permissions
sudo chmod -R 755 /var/www/license-server
sudo chmod -R 700 /var/www/license-server/config
```

## Step 5: Nginx Configuration

```bash
# Create Nginx configuration
sudo nano /etc/nginx/sites-available/license-server
```

```nginx
# TLS-only endpoints (certificate enrollment)
server {
    listen 443 ssl http2;
    server_name license-server.yourdomain.com;
    
    root /var/www/license-server/public;
    index index.php;
    
    ssl_certificate /etc/ssl/certs/license-server.crt;
    ssl_certificate_key /etc/ssl/private/license-server.key;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    
    # Require client certificate
    ssl_verify_client on;
    ssl_client_certificate /etc/ca/intermediate-ca/certs/ca-chain.crt;
    ssl_verify_depth 2;
    
    # License and migration endpoints - mTLS required
    location ~ ^/api/(license|migration|subscription)/ {
        try_files $uri /index.php?$query_string;
    }
    
    location ~ \.php$ {
        fastcgi_pass unix:/var/run/php/php8.1-fpm.sock;
        fastcgi_index index.php;
        fastcgi_param SCRIPT_FILENAME $document_root$fastcgi_script_name;
        
        # Pass client certificate info to PHP
        fastcgi_param SSL_CLIENT_CERT $ssl_client_cert;
        fastcgi_param SSL_CLIENT_FINGERPRINT $ssl_client_fingerprint;
        fastcgi_param SSL_CLIENT_VERIFY $ssl_client_verify;
        fastcgi_param SSL_CLIENT_S_DN $ssl_client_s_dn;
        
        include fastcgi_params;
    }
}
```

```bash
# Enable site
sudo ln -s /etc/nginx/sites-available/license-server /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl restart nginx
```

## Step 6: Firewall Configuration

```bash
# Allow HTTPS traffic
sudo ufw allow 443/tcp
sudo ufw allow 8443/tcp
sudo ufw enable
```

## Step 7: Systemd Service for Background Tasks

```bash
# Create systemd service for license renewal checks
sudo nano /etc/systemd/system/license-renewal.service
```

```ini
[Unit]
Description=License Renewal Background Service
After=network.target mysql.service

[Service]
Type=simple
User=www-data
WorkingDirectory=/var/www/license-server
ExecStart=/usr/bin/php /var/www/license-server/background/renewal-worker.php
Restart=always
RestartSec=60

[Install]
WantedBy=multi-user.target
```

```bash
# Enable and start service
sudo systemctl enable license-renewal
sudo systemctl start license-renewal
sudo systemctl status license-renewal
```

## Step 8: Monitoring and Logging

```bash
# Setup log rotation
sudo nano /etc/logrotate.d/license-server
```

```
/var/log/license-server/*.log {
    daily
    missingok
    rotate 14
    compress
    delaycompress
    notifempty
    create 0640 www-data www-data
    sharedscripts
    postrotate
        systemctl reload php8.1-fpm > /dev/null 2>&1 || true
    endscript
}
```

## Step 9: Backup Configuration

```bash
# Create backup script
sudo nano /usr/local/bin/license-server-backup.sh
```

```bash
#!/bin/bash
BACKUP_DIR="/backup/license-server"
DATE=$(date +%Y%m%d_%H%M%S)

# Backup database
mysqldump -u license_user -p license_system | gzip > "$BACKUP_DIR/db_$DATE.sql.gz"

# Backup CA certificates
tar -czf "$BACKUP_DIR/ca_$DATE.tar.gz" /etc/ca/intermediate-ca

# Backup application config
tar -czf "$BACKUP_DIR/config_$DATE.tar.gz" /var/www/license-server/config /var/www/license-server/.env

# Keep only last 30 days
find "$BACKUP_DIR" -name "*.gz" -mtime +30 -delete
```

```bash
# Make executable and schedule
sudo chmod +x /usr/local/bin/license-server-backup.sh
sudo crontab -e
# Add: 0 2 * * * /usr/local/bin/license-server-backup.sh
```

## Step 10: Health Monitoring

```bash
# Create health check endpoint monitoring
sudo nano /usr/local/bin/health-check.sh
```

```bash
#!/bin/bash
ENDPOINT="https://license-server.yourdomain.com/health"
ALERT_EMAIL="admin@yourdomain.com"

response=$(curl -s -o /dev/null -w "%{http_code}" "$ENDPOINT")

if [ "$response" != "200" ]; then
    echo "Health check failed with status $response" | mail -s "License Server Alert" "$ALERT_EMAIL"
fi
```

```bash
# Schedule health checks every 5 minutes
sudo chmod +x /usr/local/bin/health-check.sh
sudo crontab -e
# Add: */5 * * * * /usr/local/bin/health-check.sh
```

```

### Client Distribution

**File: `docs/client-distribution.md`**

```markdown
# Client Distribution Guide

## Windows Distribution

### Creating Windows Installer

1. **Build Application**
```bash
cd client/java-desktop
mvn clean package
mvn jpackage:jpackage -Pwindows
```

2. **Installer Output**
- Location: `target/dist/LicenseClient-1.0.0.msi`
- Type: Windows Installer (MSI)
- Features: Start menu shortcuts, desktop icon, auto-update

3. **Code Signing**
```bash
# Sign the installer
signtool sign /f your-certificate.pfx /p password /t http://timestamp.digicert.com target/dist/LicenseClient-1.0.0.msi
```

4. **Distribution**
- Upload to download server
- Provide SHA-256 checksum
- Document minimum requirements (Windows 10+)

## macOS Distribution

### Creating macOS Application Bundle

1. **Build Application**
```bash
cd client/java-desktop
mvn clean package
mvn jpackage:jpackage -Pmacos
```

2. **Code Signing**
```bash
# Sign the application
codesign --deep --force --verify --verbose --sign "Developer ID Application: Your Name" target/dist/LicenseClient.app

# Create DMG
hdiutil create -volname "LicenseClient" -srcfolder target/dist/LicenseClient.app -ov -format UDZO target/dist/LicenseClient-1.0.0.dmg

# Sign DMG
codesign --sign "Developer ID Application: Your Name" target/dist/LicenseClient-1.0.0.dmg
```

3. **Notarization** (required for macOS 10.15+)
```bash
# Submit for notarization
xcrun altool --notarize-app --primary-bundle-id "com.licenseserver.client" --username "your@apple.id" --password "@keychain:AC_PASSWORD" --file target/dist/LicenseClient-1.0.0.dmg

# Staple notarization
xcrun stapler staple target/dist/LicenseClient-1.0.0.dmg
```

## Linux Distribution

### Creating DEB Package

1. **Build Application**
```bash
cd client/java-desktop
mvn clean package
mvn jpackage:jpackage -Plinux
```

2. **DEB Package Output**
- Location: `target/dist/licenseclient_1.0.0_amd64.deb`
- Supports: Ubuntu, Debian, and derivatives

3. **Create APT Repository** (optional)
```bash
# Setup repository structure
mkdir -p apt-repo/dists/stable/main/binary-amd64
cp target/dist/*.deb apt-repo/dists/stable/main/binary-amd64/

# Generate Packages file
cd apt-repo/dists/stable/main/binary-amd64
dpkg-scanpackages . /dev/null | gzip -9c > Packages.gz

# Sign repository
cd apt-repo
gpg --armor --detach-sign -o dists/stable/Release.gpg dists/stable/Release
```

### Creating RPM Package

```bash
# Build RPM (requires rpmbuild)
mvn jpackage:jpackage -Plinux-rpm
# Output: target/dist/licenseclient-1.0.0.x86_64.rpm
```

## Flutter Mobile Distribution

### Android (Google Play Store)

1. **Build Release APK/Bundle**
```bash
cd client/flutter
flutter build appbundle --release
```

2. **Sign Application**
```bash
# Already signed during build if keystore configured in android/key.properties
# Output: build/app/outputs/bundle/release/app-release.aab
```

3. **Upload to Play Console**
- Login to Google Play Console
- Create new release
- Upload AAB file
- Complete store listing
- Submit for review

### iOS (Apple App Store)

1. **Build Release**
```bash
cd client/flutter
flutter build ios --release
```

2. **Code Signing and Upload**
```bash
# Open in Xcode
open ios/Runner.xcworkspace

# In Xcode:
# 1. Select "Product" > "Archive"
# 2. Once archived, click "Distribute App"
# 3. Choose "App Store Connect"
# 4. Upload to App Store
```

3. **App Store Connect**
- Complete app information
- Upload screenshots
- Submit for review

## Auto-Update Configuration

### Server-Side Update Manifest

**File: `server/public/updates/manifest.json`**

```json
{
  "windows": {
    "version": "1.0.0",
    "url": "https://downloads.yourdomain.com/LicenseClient-1.0.0.msi",
    "checksum": "sha256:abc123...",
    "releaseNotes": "Initial release",
    "minimumVersion": "1.0.0"
  },
  "macos": {
    "version": "1.0.0",
    "url": "https://downloads.yourdomain.com/LicenseClient-1.0.0.dmg",
    "checksum": "sha256:def456...",
    "releaseNotes": "Initial release",
    "minimumVersion": "1.0.0"
  },
  "linux": {
    "version": "1.0.0",
    "url": "https://downloads.yourdomain.com/licenseclient_1.0.0_amd64.deb",
    "checksum": "sha256:ghi789...",
    "releaseNotes": "Initial release",
    "minimumVersion": "1.0.0"
  }
}
```

### Client-Side Update Checker

**File: `client/java-desktop/src/main/java/com/licenseserver/client/UpdateChecker.java`**

```java
public class UpdateChecker {
    private static final String UPDATE_URL = "https://license-server.yourdomain.com/updates/manifest.json";
    private String currentVersion = "1.0.0";
    
    public UpdateInfo checkForUpdates() throws IOException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(UPDATE_URL))
            .build();
            
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 200) {
            JsonObject manifest = JsonParser.parseString(response.body()).getAsJsonObject();
            String platform = getPlatform();
            JsonObject platformInfo = manifest.getAsJsonObject(platform);
            
            String latestVersion = platformInfo.get("version").getAsString();
            
            if (isNewerVersion(latestVersion, currentVersion)) {
                return new UpdateInfo(
                    latestVersion,
                    platformInfo.get("url").getAsString(),
                    platformInfo.get("checksum").getAsString(),
                    platformInfo.get("releaseNotes").getAsString()
                );
            }
        }
        
        return null; // No update available
    }
    
    private boolean isNewerVersion(String latest, String current) {
        // Semantic version comparison
        String[] latestParts = latest.split("\\.");
        String[] currentParts = current.split("\\.");
        
        for (int i = 0; i < Math.min(latestParts.length, currentParts.length); i++) {
            int l = Integer.parseInt(latestParts[i]);
            int c = Integer.parseInt(currentParts[i]);
            if (l > c) return true;
            if (l < c) return false;
        }
        
        return latestParts.length > currentParts.length;
    }
}
```

```

---

## 11. Monitoring and Logging

### Application Logging Configuration

**File: `server/config/logging.php`**

```php
<?php
use Monolog\Logger;
use Monolog\Handler\StreamHandler;
use Monolog\Handler\RotatingFileHandler;
use Monolog\Formatter\LineFormatter;

return [
    'default' => 'stack',
    
    'channels' => [
        'stack' => [
            'driver' => 'stack',
            'channels' => ['daily', 'audit'],
        ],
        
        'daily' => [
            'driver' => 'daily',
            'path' => $_ENV['LOG_PATH'] ?? '/var/log/license-server/app.log',
            'level' => $_ENV['LOG_LEVEL'] ?? 'debug',
            'days' => 14,
        ],
        
        'audit' => [
            'driver' => 'daily',
            'path' => $_ENV['AUDIT_LOG_PATH'] ?? '/var/log/license-server/audit.log',
            'level' => 'info',
            'days' => 90,
        ],
        
        'security' => [
            'driver' => 'daily',
            'path' => '/var/log/license-server/security.log',
            'level' => 'warning',
            'days' => 180,
        ],
    ],
];
```

### Structured Logging

**File: `server/src/Services/AuditLogger.php`**

```php
<?php
namespace App\Services;

use Monolog\Logger;

class AuditLogger
{
    private $logger;
    
    public function __construct(Logger $logger)
    {
        $this->logger = $logger;
    }
    
    public function logEnrollment($userId, $certFingerprint, $deviceId)
    {
        $this->logger->info('certificate_enrollment', [
            'event_type' => 'enrollment',
            'user_id' => $userId,
            'certificate_fingerprint' => $certFingerprint,
            'device_id' => $deviceId,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? 'unknown',
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? 'unknown',
            'timestamp' => time(),
        ]);
    }
    
    public function logLicenseRenewal($certFingerprint, $deviceId, $status)
    {
        $this->logger->info('license_renewal', [
            'event_type' => 'renewal',
            'certificate_fingerprint' => $certFingerprint,
            'device_id' => $deviceId,
            'status' => $status,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? 'unknown',
            'timestamp' => time(),
        ]);
    }
    
    public function logMigration($certFingerprint, $oldDeviceId, $newDeviceId)
    {
        $this->logger->info('license_migration', [
            'event_type' => 'migration',
            'certificate_fingerprint' => $certFingerprint,
            'old_device_id' => $oldDeviceId,
            'new_device_id' => $newDeviceId,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? 'unknown',
            'timestamp' => time(),
        ]);
    }
    
    public function logSecurityEvent($eventType, $details)
    {
        $this->logger->warning('security_event', [
            'event_type' => $eventType,
            'details' => $details,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? 'unknown',
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? 'unknown',
            'timestamp' => time(),
        ]);
    }
}
```

### Prometheus Metrics Export

**File: `server/public/metrics.php`**

```php
<?php
require_once __DIR__ . '/../vendor/autoload.php';

use App\Database\Database;

header('Content-Type: text/plain; version=0.0.4');

$db = Database::getInstance();

// Certificate metrics
$activeSertsStmt = $db->query("SELECT COUNT(*) FROM issued_certificates WHERE status = 'active'");
$activeCerts = $activeCertsStmt->fetchColumn();

$expiringCertsStmt = $db->query("SELECT COUNT(*) FROM issued_certificates WHERE status = 'active' AND expires_at < DATE_ADD(NOW(), INTERVAL 30 DAY)");
$expiringCerts = $expiringCertsStmt->fetchColumn();

// License metrics
$activeLicensesStmt = $db->query("SELECT COUNT(*) FROM licenses WHERE is_active = 1");
$activeLicenses = $activeLicensesStmt->fetchColumn();

// Subscription metrics
$activeSubsStmt = $db->query("SELECT COUNT(*) FROM subscriptions WHERE payment_status = 'active' AND end_date > NOW()");
$activeSubs = $activeSubsStmt->fetchColumn();

$expiringSoonStmt = $db->query("SELECT COUNT(*) FROM subscriptions WHERE payment_status = 'active' AND end_date BETWEEN NOW() AND DATE_ADD(NOW(), INTERVAL 7 DAY)");
$expiringSoon = $expiringSoonStmt->fetchColumn();

// Grace period metrics
$gracePerioddStmt = $db->query("
    SELECT COUNT(*) FROM licenses l
    JOIN subscriptions s ON l.subscription_id = s.id
    WHERE l.is_active = 1 
    AND s.end_date < NOW() 
    AND s.end_date > DATE_SUB(NOW(), INTERVAL 14 DAY)
");
$inGracePeriod = $gracePeriodStmt->fetchColumn();

// Output Prometheus format
echo "# HELP license_certificates_active Number of active certificates\n";
echo "# TYPE license_certificates_active gauge\n";
echo "license_certificates_active $activeCerts\n\n";

echo "# HELP license_certificates_expiring_soon Number of certificates expiring within 30 days\n";
echo "# TYPE license_certificates_expiring_soon gauge\n";
echo "license_certificates_expiring_soon $expiringCerts\n\n";

echo "# HELP license_active_licenses Number of active licenses\n";
echo "# TYPE license_active_licenses gauge\n";
echo "license_active_licenses $activeLicenses\n\n";

echo "# HELP license_active_subscriptions Number of active subscriptions\n";
echo "# TYPE license_active_subscriptions gauge\n";
echo "license_active_subscriptions $activeSubs\n\n";

echo "# HELP license_subscriptions_expiring_soon Number of subscriptions expiring within 7 days\n";
echo "# TYPE license_subscriptions_expiring_soon gauge\n";
echo "license_subscriptions_expiring_soon $expiringSoon\n\n";

echo "# HELP license_grace_period_active Number of licenses in grace period\n";
echo "# TYPE license_grace_period_active gauge\n";
echo "license_grace_period_active $inGracePeriod\n";
```

### Grafana Dashboard Configuration

**File: `docs/grafana-dashboard.json`**

```json
{
  "dashboard": {
    "title": "License Server Monitoring",
    "panels": [
      {
        "title": "Active Certificates",
        "type": "graph",
        "targets": [
          {
            "expr": "license_certificates_active",
            "legendFormat": "Active Certificates"
          }
        ]
      },
      {
        "title": "Active Licenses",
        "type": "graph",
        "targets": [
          {
            "expr": "license_active_licenses",
            "legendFormat": "Active Licenses"
          }
        ]
      },
      {
        "title": "Subscriptions Status",
        "type": "pie",
        "targets": [
          {
            "expr": "license_active_subscriptions",
            "legendFormat": "Active"
          },
          {
            "expr": "license_subscriptions_expiring_soon",
            "legendFormat": "Expiring Soon"
          },
          {
            "expr": "license_grace_period_active",
            "legendFormat": "Grace Period"
          }
        ]
      },
      {
        "title": "Certificate Expiration Alert",
        "type": "stat",
        "targets": [
          {
            "expr": "license_certificates_expiring_soon",
            "legendFormat": "Expiring Soon"
          }
        ],
        "thresholds": [
          {
            "value": 10,
            "color": "yellow"
          },
          {
            "value": 50,
            "color": "red"
          }
        ]
      }
    ]
  }
}
```

### Client-Side Logging (Java)

**File: `client/java-desktop/src/main/resources/logback.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <property name="LOG_DIR" value="${user.home}/.config/license-client/logs"/>
    
    <!-- Console appender -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <!-- File appender -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_DIR}/client.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>${LOG_DIR}/client.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <!-- Root logger -->
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
    </root>
    
    <!-- Package-specific logging -->
    <logger name="com.licenseserver.client" level="DEBUG"/>
    <logger name="com.licenseserver.client.security" level="TRACE"/>
</configuration>
```

---

## Summary

This implementation guide provides all the concrete details needed to build a complete reference implementation of the subscription licensing system specified in `spec.md`.

### Key Files Generated

1. **CA Setup Scripts** - Automated Private CA infrastructure
2. **Database Migrations** - Complete schema setup
3. **Server Configuration** - PHP application with dual TLS/mTLS
4. **Client Build Scripts** - Multi-platform compilation
5. **Docker Compose** - Development environment
6. **Deployment Guides** - Production server setup
7. **Testing Framework** - Unit and integration tests
8. **Monitoring Stack** - Logging, metrics, and dashboards
9. **Distribution** - Platform-specific installers

### Next Steps for Implementation

1. **Phase 1**: Set up Private CA infrastructure
2. **Phase 2**: Deploy server with database
3. **Phase 3**: Build and test Java desktop client
4. **Phase 4**: Build and test Flutter mobile/desktop clients
5. **Phase 5**: Production deployment with monitoring
6. **Phase 6**: Client distribution and updates

### Automated Code Generation

This guide is specifically structured to enable automated code generation tools like Claude Code to:

- Generate complete working implementations
- Create all necessary configuration files
- Set up development and production environments
- Build platform-specific installers
- Deploy monitoring and logging infrastructure

All code examples are production-ready and follow best practices for security, performance, and maintainability.

    
    root /var/www/license-server/public;
    index index.php;
    
    ssl_certificate /etc/ssl/certs/license-server.crt;
    ssl_certificate_key /etc/ssl/private/license-server.key;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    
    # Certificate enrollment endpoints - NO client certificate required
    location ~ ^/api/certificate/ {
        try_files $uri /index.php?$query_string;
    }
    
    location ~ \.php$ {
        fastcgi_pass unix:/var/run/php/php8.1-fpm.sock;
        fastcgi_index index.php;
        fastcgi_param SCRIPT_FILENAME $document_root$fastcgi_script_name;
        include fastcgi_params;
    }
}

# mTLS endpoints (license operations)
server {
    listen 8443 ssl http2;
    server_name license-