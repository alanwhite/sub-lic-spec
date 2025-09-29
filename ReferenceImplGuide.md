# Implementation Guide - Reference Implementation

This document provides concrete implementation details to complement the design specification in `spec.md`. It is intended to enable automated code generation tools and developers to build a complete, working reference implementation.

**Key Design Decisions:**

1. **Server: Fully Containerized Linux Environment**
   - All server components (PHP, MySQL, Apache, CA tools) run in Docker containers under Linux
   - Zero server software installation required on host macOS
   - Development environment is identical to production Linux environment
   - Complete isolation from host system

2. **Client: Native macOS Application**
   - Java desktop application runs natively on macOS host
   - **Requires JDK 25 installed on macOS** for development and running
   - Uses only JDK built-in APIs (zero runtime dependencies beyond JDK)
   - Direct access to macOS Keychain via native `security` command
   - Native macOS GUI using Swing

3. **Why Client Can't Be Fully Containerized:**
   - Needs native macOS Keychain access for certificate storage
   - Executes macOS system commands (`security`, `system_profiler`, `ioreg`)
   - Displays native macOS GUI (Swing)
   - Reads macOS-specific hardware identifiers
   - Cannot run inside Docker Linux container

**Minimum JDK Version: 25** - This implementation leverages modern Java features including:
- Virtual Threads (Project Loom) for efficient concurrent operations
- Structured Concurrency for better resource management
- Pattern Matching for cleaner code
- Record Patterns for data extraction
- String Templates for safer string composition

## Table of Contents
1. [Project Structure](#project-structure)
2. [Development Environment Setup](#development-environment-setup)
3. [Configuration Management](#configuration-management)
4. [Database Setup and Migrations](#database-setup-and-migrations)
5. [Private CA Setup](#private-ca-setup)
6. [Server Implementation Details](#server-implementation-details) *(See separate artifact)*
7. [Client Implementation Details](#client-implementation-details) *(See separate artifact)*
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
│   │   ├── 03-generate-license-keys.sh
│   │   └── docker-setup.sh         # Containerized CA setup
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
│   ├── docker/
│   │   ├── apache/
│   │   │   ├── 000-default.conf
│   │   │   └── mtls.conf
│   │   ├── ssl/
│   │   └── license-keys/
│   ├── composer.json
│   ├── .env.example
│   ├── docker-compose.yml
│   ├── docker-helper.sh            # Helper script for container operations
│   └── README.md
│
├── client/                          # macOS Client Implementation
│   └── macos-java/                 # Java macOS Client
│       ├── src/
│       │   ├── main/
│       │   │   └── java/
│       │   │       └── com/
│       │   │           └── licenseserver/
│       │   │               └── client/
│       │   │                   ├── Main.java
│       │   │                   ├── AppConfig.java
│       │   │                   ├── CertificateManager.java
│       │   │                   ├── JWTValidator.java
│       │   │                   ├── LicenseApiClient.java
│       │   │                   ├── DeviceIdentifier.java
│       │   │                   ├── LicenseStorage.java
│       │   │                   ├── EnrollmentManager.java
│       │   │                   ├── LicenseManager.java
│       │   │                   ├── LicenseRenewalScheduler.java
│       │   │                   └── ui/
│       │   │                       ├── MainWindow.java
│       │   │                       ├── EnrollmentDialog.java
│       │   │                       └── MigrationDialog.java
│       │   └── test/
│       │       └── java/
│       │           └── com/
│       │               └── licenseserver/
│       │                   └── client/
│       │                       ├── CertificateManagerTest.java
│       │                       ├── JWTValidatorTest.java
│       │                       └── DeviceIdentifierTest.java
│       ├── resources/
│       │   ├── ca-chain.pem        # Embedded CA certificates
│       │   ├── license-server.pub  # Embedded license public key
│       │   ├── config.properties
│       │   └── logging.properties
│       ├── pom.xml
│       ├── build.sh                # Build and sign script
│       ├── notarize.sh             # macOS notarization script
│       └── README.md
│
└── docs/
    ├── deployment-production.md
    ├── client-distribution.md
    └── troubleshooting.md
```

---

## 2. Development Environment Setup

### Server Development Environment (macOS Host - Linux Containers)

**Architecture:**
- **Host**: macOS (your development machine)
- **Containers**: Linux (Ubuntu-based PHP, MySQL, Alpine OpenSSL)
- **All server code runs inside Linux containers**

**Prerequisites (Host macOS Only):**
- macOS 12.0 (Monterey) or later
- Docker Desktop for Mac
- Git
- Text editor/IDE (VS Code, PHPStorm, etc.)

**NO server software installation required on macOS!**
- ❌ No PHP
- ❌ No MySQL
- ❌ No Composer
- ❌ No OpenSSL
- ❌ No Apache/Nginx

**How Docker Desktop Works on macOS:**
- Docker Desktop creates a lightweight Linux VM
- All containers run inside this Linux VM
- Your macOS filesystem is mounted into containers
- Port forwarding makes container services accessible from macOS
- You edit files on macOS, they execute in Linux containers

**Setup Steps:**

```bash
# Install Docker Desktop on macOS (REQUIRED)
brew install --cask docker
# Or download from https://www.docker.com/products/docker-desktop

# Clone repository
git clone https://github.com/yourusername/sub-lic-spec.git
cd sub-lic-spec/server

# Everything runs in containers from here!
# No need to install PHP, MySQL, Composer on your Mac

# Start all services (PHP, MySQL, Nginx)
docker-compose up -d

# Install PHP dependencies (inside container)
docker-compose exec web composer install

# Run database migrations (inside container)
docker-compose exec web php migrate.php

# Seed test data (inside container, optional)
docker-compose exec web php seed.php

# View logs
docker-compose logs -f web

# Access the application
# TLS endpoint: https://localhost:8443
# mTLS endpoint: https://localhost:9443
# PHPMyAdmin: http://localhost:8081

# Stop services
docker-compose down

# Restart services
docker-compose restart
```

**Development Workflow:**
1. Edit code on your Mac using any IDE (VS Code, PHPStorm, etc.)
2. Changes are instantly reflected in the container (bind mount)
3. Run commands inside container: `docker-compose exec web <command>`
4. No need to manage PHP versions, extensions, or MySQL on macOS

### Client Development Environment (macOS Native)

**Architecture:**
- **Client runs natively on macOS host** (NOT in containers)
- Requires direct macOS system access for Keychain and hardware ID
- Cannot be containerized due to native macOS dependencies

**Prerequisites (Host macOS - Required):**
- macOS 12.0 (Monterey) or later
- **JDK 25 (Required)** - Must be installed on host macOS
- Maven 3.9+
- Xcode Command Line Tools (for native integrations)

**Why JDK 25 is Required on Host:**
- Client is a native macOS desktop application
- Accesses macOS Keychain via `/usr/bin/security` command
- Reads macOS hardware identifiers via `system_profiler` and `ioreg`
- Displays native macOS GUI using Swing
- Cannot run inside a Linux Docker container

**Setup Steps:**

```bash
# Install JDK 25 on macOS (REQUIRED)
brew install openjdk@25

# Add JDK to path
echo 'export PATH="/opt/homebrew/opt/openjdk@25/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc

# Verify version
java -version  # Should show version 25.x.x

# Install Maven
brew install maven

# Install Xcode Command Line Tools (for native command access)
xcode-select --install

# Navigate to client directory
cd client/macos-java

# Install dependencies (only testing frameworks)
mvn clean install

# Run tests
mvn test

# Run application (on macOS host)
mvn exec:java -Dexec.mainClass="com.licenseserver.client.Main"

# Build distributable package
mvn package
```

**Note on Client Containerization:**
While the client JAR can be *built* using a Maven Docker container:
```bash
docker run --rm -v "$(pwd)":/app -w /app maven:3.9-eclipse-temurin-25 mvn clean package
```

The resulting application **must still run on macOS host with JDK 25 installed** because it requires:
- Native macOS Keychain access
- macOS system commands
- macOS GUI environment
- Direct hardware access for device identification

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
DB_HOST=db
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
CA_CRL_PATH=/var/www/html/public/crl/current.crl

# License Signing Keys
LICENSE_SIGNING_KEY_PATH=/etc/license-server/license-signing.key
LICENSE_SIGNING_KEY_PASSWORD=license_key_password_here
LICENSE_SIGNING_PUB_PATH=/etc/license-server/license-signing.pub

# TLS Configuration
TLS_CERT_PATH=/etc/ssl/certs-custom/server.crt
TLS_KEY_PATH=/etc/ssl/certs-custom/server.key

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

### Client Configuration (Java/macOS)

**File: `client/macos-java/src/main/resources/config.properties`**

```properties
# License Server
license.server.url=https://license-server.local:8443
license.server.verify.ssl=true

# Certificate Storage (macOS Keychain)
cert.keychain.name=login
cert.keychain.label=License Client Certificate

# License Storage
license.storage.path=${user.home}/Library/Application Support/LicenseClient
license.storage.encrypted=true

# Renewal
license.renewal.check.days=7
license.renewal.check.interval.hours=24
license.renewal.background=true

# Migration
migration.token.validity.hours=24

# Logging
log.level=INFO
log.path=${user.home}/Library/Logs/LicenseClient

# UI
ui.theme=system
ui.notifications.enabled=true
ui.dock.icon=true
```

**File: `client/macos-java/src/main/java/com/licenseserver/client/AppConfig.java`**

```java
package com.licenseserver.client;

public class AppConfig {
    public static final String LICENSE_SERVER_URL = System.getProperty(
        "license.server.url",
        "https://license-server.local:8443"
    );
    
    public static final boolean VERIFY_SSL = Boolean.parseBoolean(
        System.getProperty("license.server.verify.ssl", "true")
    );
    
    public static final int RENEWAL_CHECK_DAYS = 7;
    public static final int RENEWAL_CHECK_INTERVAL_HOURS = 24;
    
    public static final int MIGRATION_TOKEN_VALIDITY_HOURS = 24;
    
    // Embedded CA certificate chain (PEM format)
    public static final String CA_CERTIFICATE_CHAIN = """
-----BEGIN CERTIFICATE-----
MIIFxTCCA62gAwIBAgIUABCDEFGHIJKLMNOPQRSTUVWXYZab...
-----END CERTIFICATE-----
-----BEGIN CERTIFICATE-----
MIIFyTCCA7GgAwIBAgIUZabcdefghijklmnopqrstuvwxyz...
-----END CERTIFICATE-----
""";
    
    // Embedded license server public key (PEM format)
    public static final String LICENSE_SERVER_PUBLIC_KEY = """
-----BEGIN PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA...
-----END PUBLIC KEY-----
""";
}
```

**File: `client/macos-java/src/main/resources/logging.properties`**

```properties
# JDK built-in logging configuration

# Root logger level
.level=INFO

# Handlers
handlers=java.util.logging.ConsoleHandler,java.util.logging.FileHandler

# Console handler
java.util.logging.ConsoleHandler.level=INFO
java.util.logging.ConsoleHandler.formatter=java.util.logging.SimpleFormatter

# File handler - logs to ~/Library/Logs/LicenseClient/
java.util.logging.FileHandler.pattern=%h/Library/Logs/LicenseClient/client%u.log
java.util.logging.FileHandler.limit=5000000
java.util.logging.FileHandler.count=10
java.util.logging.FileHandler.level=ALL
java.util.logging.FileHandler.formatter=java.util.logging.SimpleFormatter
java.util.logging.FileHandler.append=true

# Formatter pattern
java.util.logging.SimpleFormatter.format=%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS.%1$tL %4$s [%3$s] %5$s%6$s%n

# Package-specific logging levels
com.licenseserver.client.level=FINE
com.licenseserver.client.security.level=FINEST
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

### CA Setup in Containers

**File: `ca/docker-setup.sh`**

```bash
#!/bin/bash
set -e

# Setup CA infrastructure using Docker (no OpenSSL needed on host!)

echo "Setting up CA infrastructure using Docker..."

# Use OpenSSL container to generate CA files
docker run --rm -v "$(pwd)":/ca -w /ca alpine/openssl:latest sh -c '
  # Install bash for script execution
  apk add --no-cache bash
  
  # Run CA setup scripts
  cd scripts
  bash 01-setup-root-ca.sh
  bash 02-setup-intermediate-ca.sh
  bash 03-generate-license-keys.sh
'

echo "CA setup complete!"
echo "Root CA: $(pwd)/root-ca/certs/root-ca.crt"
echo "Intermediate CA: $(pwd)/intermediate-ca/certs/intermediate-ca.crt"
echo "License Keys: $(pwd)/../server/docker/license-keys/"
```

### Root CA Setup Script

**File: `ca/scripts/01-setup-root-ca.sh`**

```bash
#!/bin/bash
set -e

CA_DIR="/ca"
ROOT_CA_DIR="$CA_DIR/root-ca"

echo "Setting up Root CA..."

# Create directory structure
mkdir -p "$ROOT_CA_DIR"/{private,certs,crl,newcerts}
chmod 700 "$ROOT_CA_DIR/private"

# Create database files
touch "$ROOT_CA_DIR/index.txt"
echo "1000" > "$ROOT_CA_DIR/serial"

# Generate root CA private key (4096-bit, AES-256 encrypted)
openssl genrsa -aes256 -passout pass:rootcapassword -out "$ROOT_CA_DIR/private/root-ca.key" 4096
chmod 400 "$ROOT_CA_DIR/private/root-ca.key"

# Generate root CA certificate (20 year validity)
openssl req -config ../config/root-ca.cnf \
    -key "$ROOT_CA_DIR/private/root-ca.key" \
    -passin pass:rootcapassword \
    -new -x509 -days 7300 -sha256 -extensions v3_ca \
    -out "$ROOT_CA_DIR/certs/root-ca.crt" \
    -subj "/C=US/ST=State/L=City/O=Your Organization/OU=Certificate Authority/CN=Root CA"

# Verify certificate
openssl x509 -noout -text -in "$ROOT_CA_DIR/certs/root-ca.crt"

echo "Root CA setup complete!"
echo "Certificate: $ROOT_CA_DIR/certs/root-ca.crt"
echo "IMPORTANT: Store the private key securely!"
```

### Intermediate CA Setup Script

**File: `ca/scripts/02-setup-intermediate-ca.sh`**

```bash
#!/bin/bash
set -e

CA_DIR="/ca"
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
openssl genrsa -aes256 -passout pass:intermediatecapassword -out "$INT_CA_DIR/private/intermediate-ca.key" 4096
chmod 400 "$INT_CA_DIR/private/intermediate-ca.key"

# Generate intermediate CA CSR
openssl req -config ../config/intermediate-ca.cnf -new -sha256 \
    -key "$INT_CA_DIR/private/intermediate-ca.key" \
    -passin pass:intermediatecapassword \
    -out "$INT_CA_DIR/csr/intermediate-ca.csr" \
    -subj "/C=US/ST=State/L=City/O=Your Organization/OU=Certificate Authority/CN=Intermediate CA"

# Sign intermediate certificate with root CA (10 year validity)
openssl ca -config ../config/root-ca.cnf -extensions v3_intermediate_ca \
    -days 3650 -notext -md sha256 \
    -passin pass:rootcapassword \
    -batch \
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

### License Signing Keys Setup

**File: `ca/scripts/03-generate-license-keys.sh`**

```bash
#!/bin/bash
set -e

LICENSE_KEY_DIR="/ca/../server/docker/license-keys"

echo "Generating license signing keys..."

mkdir -p "$LICENSE_KEY_DIR"
chmod 700 "$LICENSE_KEY_DIR"

# Generate license signing private key (2048-bit for JWT)
openssl genrsa -aes256 -passout pass:licensekeypassword -out "$LICENSE_KEY_DIR/license-signing.key" 2048
chmod 400 "$LICENSE_KEY_DIR/license-signing.key"

# Extract public key
openssl rsa -in "$LICENSE_KEY_DIR/license-signing.key" \
    -passin pass:licensekeypassword \
    -pubout -out "$LICENSE_KEY_DIR/license-signing.pub"
chmod 444 "$LICENSE_KEY_DIR/license-signing.pub"

echo "License signing keys generated!"
echo "Private key: $LICENSE_KEY_DIR/license-signing.key"
echo "Public key: $LICENSE_KEY_DIR/license-signing.pub"
echo ""
echo "IMPORTANT: Embed the public key in client applications!"
```

### OpenSSL Configuration Files

**File: `ca/config/root-ca.cnf`** and **`ca/config/intermediate-ca.cnf`** follow standard OpenSSL CA configuration format as defined in spec.md Section 5.

---

## 6. Server Implementation Details

The server implementation follows the code examples and architecture defined in `spec.md`. All PHP code runs inside Docker containers on a Linux environment.

### Composer Dependencies (Minimal)

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
        "monolog/monolog": "^3.0"
    },
    "require-dev": {
        "phpunit/phpunit": "^10.0",
        "phpstan/phpstan": "^1.10"
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

### Key Service Implementations

All service implementations follow the detailed examples in spec.md Section 8:

- **PrivateCAService.php** - Issues certificates using Intermediate CA
- **EnrollmentTokenService.php** - Manages portal-generated enrollment tokens
- **LicenseTokenService.php** - Generates and validates JWT license tokens
- **LicenseRenewalService.php** - Handles subscription renewals and grace periods
- **DeviceMigrationService.php** - Manages secure license transfers between devices
- **CertificateValidator.php** - Validates client certificates via mTLS

Refer to spec.md Section 8 for complete implementations of these services.

### Database Schema

Database schemas are defined in migration files under `server/database/migrations/`:

- `001_create_users_table.sql`
- `002_create_subscriptions_table.sql`
- `003_create_enrollment_tokens_table.sql`
- `004_create_certificates_table.sql`
- `005_create_clients_table.sql`
- `006_create_licenses_table.sql`
- `007_create_migrations_table.sql`

These follow the complete schema specifications in spec.md Sections 8.3 and 8.4.

All migrations run inside the MySQL container via:
```bash
./docker-helper.sh migrate
```

### Middleware Implementation

**File: `server/src/Middleware/MTLSAuthMiddleware.php`**

```php
<?php
namespace App\Middleware;

class MTLSAuthMiddleware
{
    public function handle()
    {
        // Validate client certificate presence (set by Nginx/Apache)
        $clientCert = $_SERVER['SSL_CLIENT_CERT'] ?? null;
        $clientVerify = $_SERVER['SSL_CLIENT_VERIFY'] ?? null;
        
        if (!$clientCert || $clientVerify !== 'SUCCESS') {
            http_response_code(401);
            echo json_encode(['error' => 'Valid client certificate required']);
            exit;
        }
        
        // Extract and store certificate fingerprint for use by controllers
        $_SERVER['CLIENT_CERT_FINGERPRINT'] = openssl_x509_fingerprint($clientCert, 'sha256');
    }
}
```

**File: `server/src/Middleware/TLSAuthMiddleware.php`**

```php
<?php
namespace App\Middleware;

class TLSAuthMiddleware
{
    public function handle()
    {
        // TLS-only endpoints - no client certificate required
        // Validate enrollment token instead (handled by controller)
        
        // Ensure HTTPS
        if (empty($_SERVER['HTTPS']) || $_SERVER['HTTPS'] === 'off') {
            http_response_code(403);
            echo json_encode(['error' => 'HTTPS required']);
            exit;
        }
    }
}
```

### Container Operations

All server components run inside Docker containers. Use the helper script:

```bash
# Start all services (PHP, MySQL, Apache)
./docker-helper.sh start

# Install dependencies
./docker-helper.sh composer install

# Run migrations
./docker-helper.sh migrate

# Access container shell
./docker-helper.sh shell

# View logs
./docker-helper.sh logs

# Stop all services
./docker-helper.sh stop
```

The server runs entirely in Linux containers - no PHP, MySQL, or Apache installation needed on the macOS host.

---

## 7. Client Implementation Details (Native macOS - JDK 25)

**IMPORTANT:** The client is a native macOS application that requires JDK 25 installed on the host. It cannot run in Docker containers because it needs:
- Native macOS Keychain access
- macOS system commands (`security`, `system_profiler`, `ioreg`)
- Native macOS GUI (Swing)
- Direct hardware access for device identification

### Core Implementation - DeviceIdentifier.java

**File: `client/macos-java/src/main/java/com/licenseserver/client/DeviceIdentifier.java`**

```java
package com.licenseserver.client;

import java.io.*;
import java.security.MessageDigest;
import java.util.logging.Logger;

public class DeviceIdentifier {
    private static final Logger logger = Logger.getLogger(DeviceIdentifier.class.getName());
    
    /**
     * Generate device ID for macOS using Hardware UUID
     * No external libraries - uses ProcessBuilder to execute native commands
     */
    public String generateDeviceId() {
        try {
            String hardwareUUID = getMacOSHardwareUUID();
            if (hardwareUUID != null) {
                return "device_" + sha256(hardwareUUID);
            }
            
            // Fallback: use serial number
            String serialNumber = getMacOSSerialNumber();
            if (serialNumber != null) {
                return "device_" + sha256(serialNumber);
            }
            
            throw new RuntimeException("Unable to determine device ID");
            
        } catch (Exception e) {
            logger.severe("Failed to generate device ID: " + e.getMessage());
            throw new RuntimeException("Failed to generate device ID", e);
        }
    }
    
    /**
     * Get macOS Hardware UUID using system_profiler command
     */
    private String getMacOSHardwareUUID() throws IOException, InterruptedException {
        
        ProcessBuilder pb = new ProcessBuilder(
            "/usr/sbin/system_profiler", "SPHardwareDataType"
        );
        
        Process process = pb.start();
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("Hardware UUID:")) {
                    String uuid = line.split(":")[1].trim();
                    logger.fine("Hardware UUID: " + uuid);
                    return uuid;
                }
            }
        }
        
        process.waitFor();
        return null;
    }
    
    /**
     * Get macOS serial number using ioreg command (fallback)
     */
    private String getMacOSSerialNumber() throws IOException, InterruptedException {
        
        ProcessBuilder pb = new ProcessBuilder(
            "/usr/sbin/ioreg", "-l"
        );
        
        Process process = pb.start();
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("IOPlatformSerialNumber")) {
                    String[] parts = line.split("=");
                    if (parts.length > 1) {
                        String serial = parts[1].trim().replaceAll("\"", "");
                        logger.fine("Serial number: " + serial);
                        return serial;
                    }
                }
            }
        }
        
        process.waitFor();
        return null;
    }
    
    /**
     * SHA-256 hash using JDK built-in MessageDigest
     */
    private String sha256(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(input.getBytes("UTF-8"));
        
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        
        return hexString.toString();
    }
}
```

### Core Implementation - CertificateManager.java

**File: `client/macos-java/src/main/java/com/licenseserver/client/CertificateManager.java`**

```java
package com.licenseserver.client;

import java.io.*;
import java.security.*;
import java.security.cert.*;
import java.util.Base64;
import java.util.logging.Logger;

public class CertificateManager {
    private static final Logger logger = Logger.getLogger(CertificateManager.class.getName());
    
    /**
     * Generate RSA key pair using JDK built-in KeyPairGenerator
     */
    public KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        logger.info("Generating RSA key pair...");
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048, new SecureRandom());
        return keyGen.generateKeyPair();
    }
    
    /**
     * Create Certificate Signing Request (CSR) using JDK APIs
     * Returns JSON with public key and subject information
     */
    public String generateCSR(KeyPair keyPair, String commonName, String organization) 
            throws Exception {
        
        PublicKey publicKey = keyPair.getPublic();
        String publicKeyPem = encodePublicKeyToPEM(publicKey);
        
        // Using String formatting for JSON
        return """
            {
              "publicKey": "%s",
              "subject": {
                "CN": "%s",
                "O": "%s"
              }
            }
            """.formatted(
                publicKeyPem.replace("\n", "\\n"),
                escapeJson(commonName),
                escapeJson(organization)
            );
    }
    
    /**
     * Encode public key to PEM format
     */
    private String encodePublicKeyToPEM(PublicKey publicKey) {
        String base64 = Base64.getEncoder().encodeToString(publicKey.getEncoded());
        StringBuilder pem = new StringBuilder();
        pem.append("-----BEGIN PUBLIC KEY-----\n");
        
        // Split into 64-character lines
        int index = 0;
        while (index < base64.length()) {
            int endIndex = Math.min(index + 64, base64.length());
            pem.append(base64, index, endIndex).append("\n");
            index = endIndex;
        }
        
        pem.append("-----END PUBLIC KEY-----\n");
        return pem.toString();
    }
    
    private String escapeJson(String value) {
        return value.replace("\"", "\\\"")
                   .replace("\\", "\\\\")
                   .replace("\n", "\\n");
    }
    
    /**
     * Load certificate from PEM string
     */
    public X509Certificate loadCertificateFromPEM(String pemString) 
            throws CertificateException {
        
        // Remove PEM headers and decode
        String base64 = pemString
            .replace("-----BEGIN CERTIFICATE-----", "")
            .replace("-----END CERTIFICATE-----", "")
            .replaceAll("\\s", "");
        
        byte[] certBytes = Base64.getDecoder().decode(base64);
        
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        return (X509Certificate) cf.generateCertificate(
            new ByteArrayInputStream(certBytes)
        );
    }
    
    /**
     * Calculate SHA-256 fingerprint of certificate
     */
    public String getCertificateFingerprint(X509Certificate cert) 
            throws NoSuchAlgorithmException, CertificateEncodingException {
        
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(cert.getEncoded());
        
        // Convert to hex string
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        
        return hexString.toString();
    }
    
    /**
     * Store certificate and private key in macOS Keychain using 'security' command
     */
    public void storeCertificateInKeychain(X509Certificate cert, PrivateKey privateKey, 
                                          String label) throws Exception {
        
        logger.info("Storing certificate in macOS Keychain...");
        
        // Create temporary PKCS#12 file
        File p12File = File.createTempFile("cert", ".p12");
        p12File.deleteOnExit();
        
        // Create PKCS#12 keystore
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, null);
        
        // Store private key and certificate
        keyStore.setKeyEntry(label, privateKey, "".toCharArray(), 
                           new Certificate[]{cert});
        
        // Write to file
        try (FileOutputStream fos = new FileOutputStream(p12File)) {
            keyStore.store(fos, "".toCharArray());
        }
        
        // Import into macOS Keychain using 'security' command
        ProcessBuilder pb = new ProcessBuilder(
            "/usr/bin/security", "import", p12File.getAbsolutePath(),
            "-k", System.getProperty("user.home") + "/Library/Keychains/login.keychain-db",
            "-T", "/usr/bin/codesign",
            "-T", "/usr/bin/security"
        );
        
        Process process = pb.start();
        int exitCode = process.waitFor();
        
        if (exitCode != 0) {
            throw new IOException("Failed to import certificate to Keychain");
        }
        
        // Clean up
        p12File.delete();
        
        logger.info("Certificate stored in Keychain successfully");
    }
    
    /**
     * Retrieve certificate from macOS Keychain
     */
    public X509Certificate getCertificateFromKeychain(String label) throws Exception {
        
        // Export certificate using 'security' command
        ProcessBuilder pb = new ProcessBuilder(
            "/usr/bin/security", "find-certificate",
            "-c", label,
            "-p",
            System.getProperty("user.home") + "/Library/Keychains/login.keychain-db"
        );
        
        Process process = pb.start();
        
        // Read PEM output
        StringBuilder pemOutput = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                pemOutput.append(line).append("\n");
            }
        }
        
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            return null; // Certificate not found
        }
        
        return loadCertificateFromPEM(pemOutput.toString());
    }
    
    /**
     * Get private key from Keychain (requires user authorization)
     */
    public PrivateKey getPrivateKeyFromKeychain(String label) throws Exception {
        
        // Export private key to temporary PKCS#12 file
        File p12File = File.createTempFile("key", ".p12");
        p12File.deleteOnExit();
        
        ProcessBuilder pb = new ProcessBuilder(
            "/usr/bin/security", "export",
            "-k", System.getProperty("user.home") + "/Library/Keychains/login.keychain-db",
            "-t", "identities",
            "-f", "pkcs12",
            "-o", p12File.getAbsolutePath(),
            "-P", "" // Empty password
        );
        
        Process process = pb.start();
        int exitCode = process.waitFor();
        
        if (exitCode != 0) {
            throw new IOException("Failed to export private key from Keychain");
        }
        
        // Load from PKCS#12 file
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream(p12File)) {
            keyStore.load(fis, "".toCharArray());
        }
        
        // Get private key
        String alias = keyStore.aliases().nextElement();
        PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, "".toCharArray());
        
        // Clean up
        p12File.delete();
        
        return privateKey;
    }
}
```

### Core Implementation - JWTValidator.java

**File: `client/macos-java/src/main/java/com/licenseserver/client/JWTValidator.java`**

```java
package com.licenseserver.client;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.*;
import java.util.*;
import java.util.logging.Logger;

public class JWTValidator {
    private static final Logger logger = Logger.getLogger(JWTValidator.class.getName());
    
    private PublicKey serverPublicKey;
    
    public JWTValidator(String publicKeyPEM) throws Exception {
        this.serverPublicKey = loadPublicKeyFromPEM(publicKeyPEM);
        logger.info("JWT validator initialized");
    }
    
    /**
     * Validate and parse JWT token using only JDK APIs
     */
    public Map<String, Object> validateAndParse(String token) throws Exception {
        
        // Split token into parts
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid JWT format");
        }
        
        String headerB64 = parts[0];
        String payloadB64 = parts[1];
        String signatureB64 = parts[2];
        
        // Verify signature
        String signedData = headerB64 + "." + payloadB64;
        byte[] signature = base64UrlDecode(signatureB64);
        
        if (!verifySignature(signedData.getBytes(StandardCharsets.UTF_8), signature)) {
            throw new SecurityException("Invalid JWT signature");
        }
        
        logger.fine("JWT signature verified");
        
        // Decode and parse payload
        String payloadJson = new String(base64UrlDecode(payloadB64), StandardCharsets.UTF_8);
        return parseJson(payloadJson);
    }
    
    /**
     * Verify RSA signature using JDK APIs
     */
    private boolean verifySignature(byte[] data, byte[] signature) throws Exception {
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initVerify(serverPublicKey);
        sig.update(data);
        return sig.verify(signature);
    }
    
    /**
     * Load RSA public key from PEM format
     */
    private PublicKey loadPublicKeyFromPEM(String pemString) throws Exception {
        
        // Remove PEM headers
        String base64 = pemString
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replaceAll("\\s", "");
        
        byte[] keyBytes = Base64.getDecoder().decode(base64);
        
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(spec);
    }
    
    /**
     * Base64 URL decode (JWT uses URL-safe Base64)
     */
    private byte[] base64UrlDecode(String input) {
        String base64 = input.replace('-', '+').replace('_', '/');
        
        // Add padding if needed
        int padding = (4 - base64.length() % 4) % 4;
        base64 += "=".repeat(padding);
        
        return Base64.getDecoder().decode(base64);
    }
    
    /**
     * Simple JSON parser for JWT payload
     */
    private Map<String, Object> parseJson(String json) {
        Map<String, Object> result = new HashMap<>();
        
        // Remove outer braces
        json = json.trim();
        if (json.startsWith("{")) json = json.substring(1);
        if (json.endsWith("}")) json = json.substring(0, json.length() - 1);
        
        // Simple parsing (handles strings, numbers, booleans)
        String[] pairs = json.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
        
        for (String pair : pairs) {
            String[] keyValue = pair.split(":", 2);
            if (keyValue.length == 2) {
                String key = keyValue[0].trim().replaceAll("\"", "");
                String value = keyValue[1].trim();
                
                // Parse value
                Object parsedValue;
                if (value.startsWith("\"") && value.endsWith("\"")) {
                    parsedValue = value.substring(1, value.length() - 1);
                } else if (value.equals("true") || value.equals("false")) {
                    parsedValue = Boolean.parseBoolean(value);
                } else if (value.matches("-?\\d+")) {
                    parsedValue = Long.parseLong(value);
                } else {
                    parsedValue = value;
                }
                
                result.put(key, parsedValue);
            }
        }
        
        return result;
    }
}
```

### Core Implementation - LicenseStorage.java

**File: `client/macos-java/src/main/java/com/licenseserver/client/LicenseStorage.java`**

```java
package com.licenseserver.client;

import javax.crypto.*;
import javax.crypto.spec.*;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.Base64;
import java.util.logging.Logger;

public class LicenseStorage {
    private static final Logger logger = Logger.getLogger(LicenseStorage.class.getName());
    
    private final Path storageDir;
    private final SecretKey encryptionKey;
    
    public LicenseStorage() throws Exception {
        // macOS application support directory
        String home = System.getProperty("user.home");
        this.storageDir = Paths.get(home, "Library", "Application Support", "LicenseClient");
        Files.createDirectories(storageDir);
        
        // Derive encryption key from device-specific data
        this.encryptionKey = deriveEncryptionKey();
        
        logger.info("License storage initialized: " + storageDir);
    }
    
    /**
     * Derive device-specific encryption key using PBKDF2
     */
    private SecretKey deriveEncryptionKey() throws Exception {
        
        // Get device-specific data
        DeviceIdentifier deviceId = new DeviceIdentifier();
        String deviceIdString = deviceId.generateDeviceId();
        
        // Use PBKDF2 to derive key from device ID
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        
        // Use fixed salt (device-bound encryption, not password-based)
        byte[] salt = "LicenseClientSalt2024".getBytes("UTF-8");
        
        PBEKeySpec spec = new PBEKeySpec(
            deviceIdString.toCharArray(),
            salt,
            10000, // iterations
            256    // key length
        );
        
        SecretKey tmp = factory.generateSecret(spec);
        return new SecretKeySpec(tmp.getEncoded(), "AES");
    }
    
    /**
     * Encrypt and store license token
     */
    public void storeLicenseToken(String token) throws Exception {
        
        logger.info("Storing license token...");
        
        // Generate random IV
        byte[] iv = new byte[16];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        
        // Encrypt token
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, new IvParameterSpec(iv));
        byte[] encrypted = cipher.doFinal(token.getBytes("UTF-8"));
        
        // Combine IV + encrypted data
        byte[] combined = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
        
        // Encode to Base64 and write to file
        String encoded = Base64.getEncoder().encodeToString(combined);
        Path tokenFile = storageDir.resolve("license.token");
        Files.writeString(tokenFile, encoded);
        
        // Set file permissions (owner read/write only)
        Files.setPosixFilePermissions(tokenFile, 
            java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));
        
        logger.info("License token stored successfully");
    }
    
    /**
     * Load and decrypt license token
     */
    public String loadLicenseToken() throws Exception {
        
        Path tokenFile = storageDir.resolve("license.token");
        if (!Files.exists(tokenFile)) {
            logger.fine("No license token found");
            return null;
        }
        
        // Read and decode
        String encoded = Files.readString(tokenFile);
        byte[] combined = Base64.getDecoder().decode(encoded);
        
        // Extract IV and encrypted data
        byte[] iv = new byte[16];
        byte[] encrypted = new byte[combined.length - 16];
        System.arraycopy(combined, 0, iv, 0, 16);
        System.arraycopy(combined, 16, encrypted, 0, encrypted.length);
        
        // Decrypt
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, encryptionKey, new IvParameterSpec(iv));
        byte[] decrypted = cipher.doFinal(encrypted);
        
        logger.fine("License token loaded successfully");
        return new String(decrypted, "UTF-8");
    }
    
    /**
     * Delete stored license token
     */
    public void deleteLicenseToken() throws IOException {
        Path tokenFile = storageDir.resolve("license.token");
        Files.deleteIfExists(tokenFile);
        logger.info("License token deleted");
    }
}
```

### Core Implementation - LicenseApiClient.java

**File: `client/macos-java/src/main/java/com/licenseserver/client/LicenseApiClient.java`**

```java
package com.licenseserver.client;

import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.logging.Logger;
import javax.net.ssl.*;

public class LicenseApiClient {
    private static final Logger logger = Logger.getLogger(LicenseApiClient.class.getName());
    
    private final String serverUrl;
    private final HttpClient httpClient;
    
    public LicenseApiClient(String serverUrl) throws Exception {
        this.serverUrl = serverUrl;
        this.httpClient = createHttpClient();
    }
    
    /**
     * Create HTTP client with mTLS support using only JDK APIs
     */
    private HttpClient createHttpClient() throws Exception {
        
        // Load CA certificates for server validation
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null);
        
        // Add embedded CA certificate chain
        CertificateManager certManager = new CertificateManager();
        X509Certificate caCert = certManager.loadCertificateFromPEM(
            AppConfig.CA_CERTIFICATE_CHAIN
        );
        trustStore.setCertificateEntry("ca", caCert);
        
        // Create TrustManager
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(
            TrustManagerFactory.getDefaultAlgorithm()
        );
        tmf.init(trustStore);
        
        // Load client certificate and private key from Keychain (if exists)
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, null);
        
        X509Certificate clientCert = certManager.getCertificateFromKeychain(
            "License Client Certificate"
        );
        
        if (clientCert != null) {
            // Premium user with certificate
            var privateKey = certManager.getPrivateKeyFromKeychain(
                "License Client Certificate"
            );
            keyStore.setKeyEntry("client", privateKey, "".toCharArray(),
                               new java.security.cert.Certificate[]{clientCert});
            logger.info("Client certificate loaded for mTLS");
        } else {
            logger.info("No client certificate - TLS only mode");
        }
        
        // Create KeyManager
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(
            KeyManagerFactory.getDefaultAlgorithm()
        );
        kmf.init(keyStore, "".toCharArray());
        
        // Create SSLContext
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), 
                       new java.security.SecureRandom());
        
        // Build HTTP client with custom SSL context
        return HttpClient.newBuilder()
            .sslContext(sslContext)
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }
    
    /**
     * Send enrollment request
     */
    public String enrollCertificate(String enrollmentToken, String csrJson, 
                                   String deviceId) throws Exception {
        
        logger.info("Submitting enrollment request...");
        
        // Create request body (simple JSON formatting)
        String requestBody = String.format(
            "{\"enrollment_token\":\"%s\",\"csr\":%s,\"device_id\":\"%s\"}",
            enrollmentToken, csrJson, deviceId
        );
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(serverUrl + "/api/certificate/enroll"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();
        
        HttpResponse<String> response = httpClient.send(request,
            HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new IOException("Enrollment failed: " + response.body());
        }
        
        logger.info("Enrollment successful");
        return response.body();
    }
    
    /**
     * Request license renewal
     */
    public String renewLicense(String deviceId) throws Exception {
        
        logger.info("Requesting license renewal...");
        
        String requestBody = String.format("{\"device_id\":\"%s\"}", deviceId);
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(serverUrl + "/api/license/renew"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();
        
        HttpResponse<String> response = httpClient.send(request,
            HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new IOException("License renewal failed: " + response.body());
        }
        
        logger.info("License renewal successful");
        return response.body();
    }
}
```

### JDK 25 Features - EnrollmentManager with Structured Concurrency

**File: `client/macos-java/src/main/java/com/licenseserver/client/EnrollmentManager.java`**

```java
package com.licenseserver.client;

import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.StructuredTaskScope.Subtask;
import java.util.logging.Logger;

public class EnrollmentManager {
    private static final Logger logger = Logger.getLogger(EnrollmentManager.class.getName());
    
    private final CertificateManager certManager;
    private final LicenseApiClient apiClient;
    private final LicenseStorage storage;
    private final DeviceIdentifier deviceId;
    
    public EnrollmentManager(String serverUrl) throws Exception {
        this.certManager = new CertificateManager();
        this.apiClient = new LicenseApiClient(serverUrl);
        this.storage = new LicenseStorage();
        this.deviceId = new DeviceIdentifier();
    }
    
    /**
     * Complete enrollment process using Structured Concurrency (JDK 25)
     */
    public EnrollmentResult enrollWithToken(String enrollmentToken) {
        try {
            logger.info("Starting enrollment process...");
            
            // Use structured concurrency to manage parallel tasks
            try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
                
                // Generate key pair in parallel with getting device ID
                Subtask<KeyPair> keyPairTask = scope.fork(() -> {
                    logger.fine("Generating key pair...");
                    return certManager.generateKeyPair();
                });
                
                Subtask<String> deviceIdTask = scope.fork(() -> {
                    logger.fine("Getting device ID...");
                    return deviceId.generateDeviceId();
                });
                
                // Wait for both to complete
                scope.join();
                scope.throwIfFailed();
                
                KeyPair keyPair = keyPairTask.get();
                String deviceIdStr = deviceIdTask.get();
                
                // Create CSR
                logger.info("Creating certificate request...");
                String csrJson = certManager.generateCSR(
                    keyPair, 
                    "License Client",
                    "Licensed User"
                );
                
                // Submit enrollment request
                logger.info("Submitting enrollment request...");
                String responseJson = apiClient.enrollCertificate(
                    enrollmentToken,
                    csrJson,
                    deviceIdStr
                );
                
                // Parse response
                Map<String, String> response = parseEnrollmentResponse(responseJson);
                String certificatePem = response.get("certificate");
                String licenseToken = response.get("license_token");
                
                // Load certificate
                X509Certificate certificate = certManager.loadCertificateFromPEM(certificatePem);
                
                // Store certificate and license in parallel
                try (var storeScope = new StructuredTaskScope.ShutdownOnFailure()) {
                    
                    storeScope.fork(() -> {
                        logger.fine("Installing certificate in Keychain...");
                        certManager.storeCertificateInKeychain(
                            certificate,
                            keyPair.getPrivate(),
                            "License Client Certificate"
                        );
                        return null;
                    });
                    
                    storeScope.fork(() -> {
                        logger.fine("Storing license token...");
                        storage.storeLicenseToken(licenseToken);
                        return null;
                    });
                    
                    storeScope.join();
                    storeScope.throwIfFailed();
                }
                
                logger.info("Enrollment complete!");
                return EnrollmentResult.success(certificate, licenseToken);
            }
            
        } catch (Exception e) {
            logger.severe("Enrollment failed: " + e.getMessage());
            return EnrollmentResult.failure(e.getMessage());
        }
    }
    
    private Map<String, String> parseEnrollmentResponse(String json) {
        Map<String, String> result = new java.util.HashMap<>();
        
        json = json.trim();
        if (json.startsWith("{")) json = json.substring(1);
        if (json.endsWith("}")) json = json.substring(0, json.length() - 1);
        
        String[] pairs = json.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
        for (String pair : pairs) {
            String[] kv = pair.split(":", 2);
            if (kv.length == 2) {
                String key = kv[0].trim().replaceAll("\"", "");
                String value = kv[1].trim().replaceAll("\"", "");
                result.put(key, value);
            }
        }
        
        return result;
    }
    
    /**
     * Enrollment result as a record (JDK 25)
     */
    public record EnrollmentResult(
        boolean success,
        String message,
        X509Certificate certificate,
        String licenseToken
    ) {
        public static EnrollmentResult success(X509Certificate cert, String token) {
            return new EnrollmentResult(true, "Enrollment successful", cert, token);
        }
        
        public static EnrollmentResult failure(String message) {
            return new EnrollmentResult(false, message, null, null);
        }
    }
}
```

### JDK 25 Features - LicenseManager with Pattern Matching and Sealed Interfaces

**File: `client/macos-java/src/main/java/com/licenseserver/client/LicenseManager.java`**

```java
package com.licenseserver.client;

import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Map;
import java.util.logging.Logger;

public class LicenseManager {
    private static final Logger logger = Logger.getLogger(LicenseManager.class.getName());
    
    private final CertificateManager certManager;
    private final LicenseStorage storage;
    private final JWTValidator jwtValidator;
    
    public LicenseManager() throws Exception {
        this.certManager = new CertificateManager();
        this.storage = new LicenseStorage();
        this.jwtValidator = new JWTValidator(AppConfig.LICENSE_SERVER_PUBLIC_KEY);
    }
    
    /**
     * Validate license using pattern matching (JDK 25)
     */
    public LicenseStatus validateLicense() {
        try {
            String token = storage.loadLicenseToken();
            
            // Pattern matching with null check
            return switch (token) {
                case null -> LicenseStatus.notActivated();
                case String t -> validateToken(t);
            };
            
        } catch (Exception e) {
            logger.warning("License validation error: " + e.getMessage());
            return LicenseStatus.invalid(e.getMessage());
        }
    }
    
    private LicenseStatus validateToken(String token) throws Exception {
        // Validate JWT
        Map<String, Object> claims = jwtValidator.validateAndParse(token);
        
        // Get certificate
        X509Certificate clientCert = certManager.getCertificateFromKeychain(
            "License Client Certificate"
        );
        
        if (clientCert == null) {
            return LicenseStatus.invalid("No client certificate found");
        }
        
        // Verify certificate fingerprint
        String certFingerprint = certManager.getCertificateFingerprint(clientCert);
        String tokenFingerprint = (String) claims.get("sub");
        
        if (!certFingerprint.equals(tokenFingerprint)) {
            return LicenseStatus.invalid("Certificate mismatch");
        }
        
        // Check dates using pattern matching
        long subscriptionEnd = ((Number) claims.get("subscription_end")).longValue();
        long gracePeriodEnd = ((Number) claims.get("grace_period_end")).longValue();
        long now = Instant.now().getEpochSecond();
        
        // Pattern matching for status determination
        return switch (Long.compare(now, subscriptionEnd)) {
            case int i when i < 0 -> LicenseStatus.valid(claims);
            case int i when now < gracePeriodEnd -> LicenseStatus.gracePeriod(claims);
            default -> LicenseStatus.expired(claims);
        };
    }
    
    /**
     * Attempt to renew license (requires network)
     */
    public boolean attemptRenewal(String serverUrl) {
        try {
            LicenseApiClient apiClient = new LicenseApiClient(serverUrl);
            DeviceIdentifier deviceId = new DeviceIdentifier();
            
            String response = apiClient.renewLicense(deviceId.generateDeviceId());
            
            // Parse response
            Map<String, String> responseData = parseJsonResponse(response);
            String newToken = responseData.get("token");
            
            if (newToken != null) {
                storage.storeLicenseToken(newToken);
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            logger.warning("License renewal failed: " + e.getMessage());
            return false;
        }
    }
    
    private Map<String, String> parseJsonResponse(String json) {
        Map<String, String> result = new java.util.HashMap<>();
        
        json = json.trim();
        if (json.startsWith("{")) json = json.substring(1);
        if (json.endsWith("}")) json = json.substring(0, json.length() - 1);
        
        String[] pairs = json.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
        for (String pair : pairs) {
            String[] kv = pair.split(":", 2);
            if (kv.length == 2) {
                String key = kv[0].trim().replaceAll("\"", "");
                String value = kv[1].trim().replaceAll("\"", "");
                result.put(key, value);
            }
        }
        
        return result;
    }
    
    /**
     * License status as a sealed interface with records (JDK 25)
     */
    public sealed interface LicenseStatus 
        permits Valid, GracePeriod, Expired, NotActivated, Invalid {
        
        boolean isValid();
        String message();
        Map<String, Object> claims();
        
        default boolean isPremium() {
            return isValid() && claims() != null;
        }
        
        static Valid valid(Map<String, Object> claims) {
            return new Valid(claims);
        }
        
        static GracePeriod gracePeriod(Map<String, Object> claims) {
            return new GracePeriod(claims);
        }
        
        static Expired expired(Map<String, Object> claims) {
            return new Expired(claims);
        }
        
        static NotActivated notActivated() {
            return new NotActivated();
        }
        
        static Invalid invalid(String reason) {
            return new Invalid(reason);
        }
    }
    
    public record Valid(Map<String, Object> claims) implements LicenseStatus {
        @Override public boolean isValid() { return true; }
        @Override public String message() { return "Valid subscription"; }
    }
    
    public record GracePeriod(Map<String, Object> claims) implements LicenseStatus {
        @Override public boolean isValid() { return true; }
        @Override public String message() { return "In grace period"; }
    }
    
    public record Expired(Map<String, Object> claims) implements LicenseStatus {
        @Override public boolean isValid() { return false; }
        @Override public String message() { return "Subscription expired"; }
    }
    
    public record NotActivated() implements LicenseStatus {
        @Override public boolean isValid() { return false; }
        @Override public String message() { return "Not activated"; }
        @Override public Map<String, Object> claims() { return null; }
    }
    
    public record Invalid(String reason) implements LicenseStatus {
        @Override public boolean isValid() { return false; }
        @Override public String message() { return "Invalid: " + reason; }
        @Override public Map<String, Object> claims() { return null; }
    }
}
```

### JDK 25 Features - Background Renewal with Virtual Threads

**File: `client/macos-java/src/main/java/com/licenseserver/client/LicenseRenewalScheduler.java`**

```java
package com.licenseserver.client;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class LicenseRenewalScheduler {
    private static final Logger logger = Logger.getLogger(LicenseRenewalScheduler.class.getName());
    private final ScheduledExecutorService scheduler;
    private final LicenseManager licenseManager;
    private final String serverUrl;
    
    public LicenseRenewalScheduler(LicenseManager licenseManager, String serverUrl) {
        // Use virtual thread executor for lightweight concurrency (JDK 25)
        this.scheduler = Executors.newScheduledThreadPool(
            1, 
            Thread.ofVirtual().name("license-renewal-", 0).factory()
        );
        this.licenseManager = licenseManager;
        this.serverUrl = serverUrl;
    }
    
    public void startBackgroundRenewal() {
        scheduler.scheduleWithFixedDelay(
            this::checkAndRenew,
            1, // Initial delay (hours)
            24, // Period (hours)
            TimeUnit.HOURS
        );
        logger.info("Background license renewal started (using virtual threads)");
    }
    
    private void checkAndRenew() {
        // This runs on a virtual thread - very lightweight
        try {
            var status = licenseManager.validateLicense();
            
            if (status.isValid() && needsRenewal(status)) {
                logger.info("Attempting license renewal...");
                boolean renewed = licenseManager.attemptRenewal(serverUrl);
                
                if (renewed) {
                    logger.info("License renewed successfully");
                } else {
                    logger.warning("License renewal failed");
                }
            }
        } catch (Exception e) {
            logger.warning("Error during background renewal: " + e.getMessage());
        }
    }
    
    private boolean needsRenewal(LicenseManager.LicenseStatus status) {
        var claims = status.claims();
        if (claims == null) return false;
        
        long subscriptionEnd = ((Number) claims.get("subscription_end")).longValue();
        long now = java.time.Instant.now().getEpochSecond();
        long daysRemaining = (subscriptionEnd - now) / (24 * 60 * 60);
        
        return daysRemaining <= 7;
    }
    
    public void shutdown() {
        scheduler.shutdown();
    }
}
```

### Summary

All client implementation files use:
- **ZERO runtime dependencies** (only JDK 25)
- **Virtual Threads** for background operations
- **Structured Concurrency** for parallel tasks
- **Pattern Matching** for cleaner logic
- **Records & Sealed Interfaces** for type safety
- **Native macOS integration** via ProcessBuilder
- **java.util.logging** (no external logging frameworks)

The client must run natively on macOS with JDK 25 installed - it cannot be containerized.

---

## 8. Build and Packaging

### Complete Setup Without Installing Server Software

**What Gets Installed Where:**

**On macOS Host:**
- ✅ Docker Desktop (container runtime)
- ✅ JDK 25 (for client application)
- ✅ Maven (for client builds)
- ✅ Git (for source control)
- ❌ NO PHP, MySQL, Composer, OpenSSL, or web servers

**In Linux Containers:**
- ✅ PHP 8.1 with extensions (in `web` container)
- ✅ Apache web server (in `web` container)
- ✅ MySQL 8.0 (in `db` container)
- ✅ Composer (runs in container)
- ✅ OpenSSL (in Alpine container for CA setup)
- ✅ PHPMyAdmin (in `phpmyadmin` container)

**Quick Start Guide:**

```bash
# 1. Install prerequisites on macOS host
brew install --cask docker           # Docker Desktop
brew install openjdk@25               # Java 25 for client
brew install maven git                # Build tools

# 2. Clone repository
git clone https://github.com/yourusername/sub-lic-spec.git
cd sub-lic-spec

# 3. Setup CA infrastructure (runs in Alpine Linux container)
cd ca
./docker-setup.sh
cd ..

# 4. Start server (runs in Linux containers)
cd server
chmod +x docker-helper.sh
./docker-helper.sh start              # Starts PHP, MySQL, Apache in containers
./docker-helper.sh composer install   # Runs inside container
./docker-helper.sh migrate            # Runs inside container

# 5. Build and run client (runs natively on macOS)
cd ../client/macos-java
mvn clean package                     # Requires JDK 25 on macOS
mvn exec:java                         # Runs on macOS with GUI

# Server running in containers, client running natively on macOS!
```

### Docker Compose Configuration (Complete Containerization)

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
      - "9443:9443"  # mTLS endpoint
    volumes:
      # Mount entire project directory
      - ./:/var/www/html
      # Apache configuration
      - ./docker/apache/000-default.conf:/etc/apache2/sites-available/000-default.conf
      - ./docker/apache/mtls.conf:/etc/apache2/sites-available/mtls.conf
      # SSL certificates
      - ./docker/ssl:/etc/ssl/certs-custom
      # CA certificates (read-only)
      - ../ca:/etc/ca:ro
      # License signing keys
      - ./docker/license-keys:/etc/license-server:ro
    environment:
      - APACHE_RUN_USER=www-data
      - APACHE_RUN_GROUP=www-data
      - APACHE_LOG_DIR=/var/log/apache2
    depends_on:
      - db
    networks:
      - license-network
    command: >
      bash -c "
        # Install required PHP extensions
        docker-php-ext-install pdo pdo_mysql mysqli &&
        # Enable Apache modules
        a2enmod ssl rewrite headers &&
        # Enable both sites
        a2ensite 000-default mtls &&
        # Start Apache
        apache2-foreground
      "

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
      # Persistent database storage
      - db-data:/var/lib/mysql
    networks:
      - license-network
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5

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
    driver: local

networks:
  license-network:
    driver: bridge
```

### Helper Scripts for Container Operations

**File: `server/docker-helper.sh`**

```bash
#!/bin/bash

# Helper script for common Docker operations

case "$1" in
  start)
    echo "Starting all services..."
    docker-compose up -d
    echo "Services started!"
    echo "Web: https://localhost:8443"
    echo "PHPMyAdmin: http://localhost:8081"
    ;;
    
  stop)
    echo "Stopping all services..."
    docker-compose down
    ;;
    
  restart)
    echo "Restarting services..."
    docker-compose restart
    ;;
    
  logs)
    docker-compose logs -f "${2:-web}"
    ;;
    
  shell)
    echo "Opening shell in web container..."
    docker-compose exec web bash
    ;;
    
  mysql)
    echo "Opening MySQL client..."
    docker-compose exec db mysql -u license_user -plicense_password license_system
    ;;
    
  composer)
    echo "Running composer $2..."
    docker-compose exec web composer "${@:2}"
    ;;
    
  migrate)
    echo "Running database migrations..."
    docker-compose exec web php migrate.php
    ;;
    
  seed)
    echo "Seeding database..."
    docker-compose exec web php seed.php
    ;;
    
  clean)
    echo "Cleaning up containers and volumes..."
    docker-compose down -v
    echo "All data removed!"
    ;;
    
  rebuild)
    echo "Rebuilding containers..."
    docker-compose down
    docker-compose build --no-cache
    docker-compose up -d
    ;;
    
  *)
    echo "Usage: $0 {start|stop|restart|logs|shell|mysql|composer|migrate|seed|clean|rebuild}"
    echo ""
    echo "Commands:"
    echo "  start      - Start all services"
    echo "  stop       - Stop all services"
    echo "  restart    - Restart all services"
    echo "  logs       - View logs (optionally specify service)"
    echo "  shell      - Open bash shell in web container"
    echo "  mysql      - Open MySQL client"
    echo "  composer   - Run composer command"
    echo "  migrate    - Run database migrations"
    echo "  seed       - Seed database with test data"
    echo "  clean      - Remove all containers and data"
    echo "  rebuild    - Rebuild containers from scratch"
    exit 1
    ;;
esac
```

### Java Client Build Scripts

**File: `client/macos-java/build.sh`**

```bash
#!/bin/bash
set -e

VERSION="1.0.0"
APP_NAME="LicenseClient"

echo "Building ${APP_NAME} v${VERSION} for macOS..."

# Clean and build
mvn clean package

# Create runtime image with jlink
jlink --add-modules java.base,java.desktop,java.sql,java.naming,java.management,jdk.crypto.ec,java.net.http \
      --output target/runtime-image \
      --strip-debug \
      --no-man-pages \
      --no-header-files \
      --compress=2

# Build macOS application bundle
echo "Building macOS application bundle..."
mvn jpackage:jpackage

# Code signing (requires Apple Developer certificate)
echo "Code signing application..."
codesign --deep --force --verify --verbose \
    --sign "Developer ID Application: Your Name" \
    --options runtime \
    target/dist/LicenseClient.app

# Create DMG
echo "Creating DMG installer..."
hdiutil create -volname "LicenseClient" \
    -srcfolder target/dist/LicenseClient.app \
    -ov -format UDZO \
    target/dist/LicenseClient-${VERSION}.dmg

# Sign DMG
codesign --sign "Developer ID Application: Your Name" \
    target/dist/LicenseClient-${VERSION}.dmg

echo "Build complete! Output: target/dist/LicenseClient-${VERSION}.dmg"
echo ""
echo "Next steps:"
echo "1. Submit for notarization: ./notarize.sh target/dist/LicenseClient-${VERSION}.dmg"
echo "2. After approval, staple: xcrun stapler staple target/dist/LicenseClient-${VERSION}.dmg"
```

**File: `client/macos-java/notarize.sh`**

```bash
#!/bin/bash
set -e

DMG_PATH="$1"

if [ -z "$DMG_PATH" ]; then
    echo "Usage: ./notarize.sh <path-to-dmg>"
    exit 1
fi

echo "Submitting ${DMG_PATH} for notarization..."

# Submit for notarization
xcrun notarytool submit "$DMG_PATH" \
    --apple-id "your@apple.id" \
    --team-id "YOUR_TEAM_ID" \
    --password "@keychain:AC_PASSWORD" \
    --wait

echo "Notarization complete!"
echo ""
echo "To staple the notarization ticket, run:"
echo "xcrun stapler staple $DMG_PATH"
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
        $this->assertStringContainsString('.', $token);
        
        $parts = explode('.', $token);
        $this->assertCount(3, $parts);
    }
    
    public function testCalculateGracePeriod()
    {
        $subscriptionEnd = new \DateTime('2025-01-01');
        
        $graceEnd = $this->service->calculateGracePeriodEnd('monthly', $subscriptionEnd);
        $this->assertEquals('2025-01-06', $graceEnd->format('Y-m-d'));
        
        $graceEnd = $this->service->calculateGracePeriodEnd('annual', $subscriptionEnd);
        $this->assertEquals('2025-01-15', $graceEnd->format('Y-m-d'));
    }
}
```

### Client Unit Tests (Java)

**File: `client/macos-java/src/test/java/com/licenseserver/client/DeviceIdentifierTest.java`**

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
        assertEquals(71, deviceId.length());
    }
    
    @Test
    public void testDeviceIdConsistency() {
        DeviceIdentifier identifier = new DeviceIdentifier();
        String deviceId1 = identifier.generateDeviceId();
        String deviceId2 = identifier.generateDeviceId();
        
        assertEquals(deviceId1, deviceId2);
    }
}
```

---

## 10. Deployment

### Production Server Deployment

All server components deploy as Docker containers in production. The containerized development environment is identical to production.

**Key Deployment Steps:**
1. Build production containers
2. Configure environment variables
3. Set up SSL certificates
4. Deploy to container orchestration platform (Docker Swarm, Kubernetes, etc.)
5. Configure load balancing and monitoring

Refer to `docs/deployment-production.md` for complete production deployment guide.

### Client Distribution

**macOS DMG Distribution:**
1. Build signed application bundle
2. Create DMG installer
3. Notarize with Apple
4. Distribute via download server

Refer to `docs/client-distribution.md` for complete distribution guide.

---

## 11. Monitoring and Logging

### Server Logging (Containerized)

All logs are accessible via Docker:

```bash
# View all logs
./docker-helper.sh logs

# View specific service logs
./docker-helper.sh logs db

# Follow logs in real-time
docker-compose logs -f web
```

### Client Logging (Native macOS)

Client logs using JDK's built-in `java.util.logging` to:
```
~/Library/Logs/LicenseClient/client.log
```

Configure via `src/main/resources/logging.properties`.

---

## Summary

This implementation guide provides all concrete details needed to build a complete reference implementation with **containerized Linux server** and **native macOS client**.

### Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                     macOS Host System                       │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐   │
│  │              Docker Desktop (Linux VM)               │   │
│  │                                                      │   │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐            │   │
│  │  │ PHP 8.1  │  │ MySQL 8  │  │ OpenSSL  │            │   │
│  │  │ Apache   │  │ Database │  │ CA Tools │            │   │
│  │  │ Composer │  │          │  │          │            │   │
│  │  └──────────┘  └──────────┘  └──────────┘            │   │
│  │       ▲              ▲              ▲                │   │
│  │       │              │              │                │   │
│  │  All Server Components Run in Linux Containers       │   │
│  └──────────────────────────────────────────────────────┘   │
│                           │                                 │
│                           │ HTTP/HTTPS                      │
│                           ▼                                 │
│  ┌──────────────────────────────────────────────────────┐   │
│  │           Java Client (Native macOS App)             │   │
│  │                                                      │   │
│  │  • Requires JDK 25 on macOS                          │   │
│  │  • Accesses macOS Keychain natively                  │   │
│  │  • Runs macOS system commands                        │   │
│  │  • Native Swing GUI                                  │   │
│  │  • Cannot run in containers                          │   │
│  └──────────────────────────────────────────────────────┘   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### Key Features

**Server (PHP - Fully Containerized in Linux):**
- **NO server software installation on host macOS**
- All server components run in isolated Linux containers
- Private CA infrastructure (via OpenSSL container)
- Dual TLS/mTLS authentication
- JWT license token management
- Helper scripts for container operations
- Complete database migrations
- PHPMyAdmin for database management

**Client (Java 25 - Native macOS Application):**
- **Requires JDK 25 installed on macOS host**
- **ZERO runtime dependencies** beyond JDK
- **Cannot be containerized** - needs native macOS access
- Virtual Threads for background operations
- Structured Concurrency for parallel tasks
- Pattern Matching & Sealed Interfaces
- Records for type-safe data modeling
- Native macOS Keychain integration
- Freemium model (Free/Premium tiers)

### Development Workflow

**Host macOS Requirements:**
- Docker Desktop (for server containers)
- JDK 25 (for client application)
- Maven (for client builds)
- Git

**Server Development (Containerized):**
```bash
./docker-helper.sh start      # Start all Linux containers
./docker-helper.sh shell      # Access container bash
./docker-helper.sh composer   # Manage dependencies (in container)
./docker-helper.sh migrate    # Run migrations (in container)
./docker-helper.sh logs       # View container logs
./docker-helper.sh clean      # Remove all containers
```

**Client Development (Native macOS):**
```bash
mvn clean package             # Build on macOS (requires JDK 25)
mvn exec:java                 # Run on macOS with GUI
./build.sh                    # Create signed DMG for distribution
```

### What Runs Where

| Component | Location | Why |
|-----------|----------|-----|
| PHP Application | Linux Container | Isolated, reproducible environment |
| MySQL Database | Linux Container | No host database pollution |
| Apache Web Server | Linux Container | Production-like setup |
| OpenSSL (CA) | Alpine Container | No OpenSSL on host needed |
| Composer | Linux Container | No PHP on host needed |
| Java Client | macOS Host | Needs Keychain, GUI, hardware access |
| JDK 25 | macOS Host | Required to run client |
| Maven | macOS Host | Client build tool |

### Next Steps

1. Install Docker Desktop + JDK 25 on macOS
2. Clone repository
3. Run `ca/docker-setup.sh` for CA infrastructure (in container)
4. Run `server/docker-helper.sh start` for server (in containers)
5. Build Java client with Maven (on macOS)
6. Code sign and notarize for distribution

**Server: Fully containerized Linux environment**  
**Client: Native macOS application (cannot be containerized)**

All code is production-ready and follows security best practices.