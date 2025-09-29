# Subscription Licensing System Design Document

## Table of Contents
1. [Executive Summary](#executive-summary)
2. [System Architecture Overview](#system-architecture-overview)
3. [Private Certificate Authority Management](#private-certificate-authority-management)
4. [Certificate Infrastructure](#certificate-infrastructure)
5. [Certificate Provisioning Workflow](#certificate-provisioning-workflow)
6. [Client Architecture (Java)](#client-architecture-java)
7. [Backend Architecture (PHP)](#backend-architecture-php)
8. [API Endpoints](#api-endpoints)
9. [Security Considerations](#security-considerations)
10. [Deployment Architecture](#deployment-architecture)
11. [Database Schema](#database-schema)
12. [Implementation Timeline](#implementation-timeline)
13. [Testing Strategy](#testing-strategy)

---

## Executive Summary

This document outlines the design for a secure subscription licensing solution using mutual TLS authentication. The system consists of a Java client application and a PHP backend API, utilizing X.509 certificates for bidirectional identity verification.

### Key Features
- Token-based client certificate provisioning via out-of-band communication
- Hierarchical private CA infrastructure using file-based certificate storage
- Certificate distribution via existing application update process
- Mutual TLS authentication for all licensing operations
- Secure subscription management with comprehensive audit logging

---

## System Architecture Overview

```
┌─────────────────┐    mTLS over HTTPS     ┌─────────────────┐
│   Java Client   │◄──────────────────────►│   PHP Backend   │
│                 │                        │                 │
│ • Client Cert   │                        │ • Server Cert   │
│ • License Logic │                        │ • License APIs  │
│ • Embedded CAs  │                        │ • CA Validation │
└─────────────────┘                        └─────────────────┘
```

### Component Overview
- **Java Client**: Swing-based desktop application with embedded CA certificates
- **PHP Backend**: Web frontend for authentication, certificate provisioning API, and license management
- **Private CA**: Hierarchical CA structure (Root CA + Intermediate CA) using standard certificate files
- **Database**: MySQL for users, subscriptions, certificates, and tokens
- **Certificate Storage**: File-based storage with appropriate security permissions

---

## Private Certificate Authority Management

### Hierarchical CA Structure

```
┌─────────────────────┐
│   Root CA (Offline) │ ← 20 year validity, secure offline storage
│  Self-signed cert   │    Private key encrypted with passphrase
└─────────────────────┘
          │
          │ Signs
          ▼
┌─────────────────────┐
│ Intermediate CA     │ ← 10 year validity, encrypted storage
│ (Online Signing)    │    Private key encrypted with passphrase
└─────────────────────┘
          │
          │ Signs
          ▼
┌─────────────────────┐
│   Client Certs      │ ← 1-2 year validity
│                     │
└─────────────────────┘
```

### CA Certificate Lifecycle
- **Root CA**: 20-year validity, offline storage, only signs intermediate CAs
- **Intermediate CA**: 10-year validity, encrypted file storage, signs client certificates
- **Client Certificates**: 1-2 year validity, regular renewal via token process
- **Certificate Chain**: Full chain embedded in application at build time

### CA Storage Structure
```
/etc/ca/
├── root-ca/
│   ├── root-ca.crt           # Root CA certificate (public)
│   ├── root-ca.key           # Root CA private key (encrypted, offline)
│   └── root-ca-config.cnf    # OpenSSL configuration
├── intermediate-ca/
│   ├── intermediate-ca.crt   # Intermediate CA certificate (public)
│   ├── intermediate-ca.key   # Intermediate CA private key (encrypted)
│   ├── intermediate-ca-config.cnf
│   └── crl/
│       └── intermediate.crl  # Certificate Revocation List
├── issued-certificates/
│   └── [serial-number].crt  # Issued client certificates
└── private/                  # Restricted permissions (chmod 700)
    └── passphrases.enc       # Encrypted passphrases
```

### CA Rotation Timeline
```
Month 0:  Generate new Intermediate CA
Month 1:  New application release with both old and new CA certificates
Month 6:  Most clients updated to new application version
Month 12: Stop issuing certificates with old CA
Month 18: Old CA expires, only new CA valid
```

### Integration with Existing Update Process

The CA certificate distribution leverages your existing application update infrastructure:

**Build Integration**: CA certificates are copied into the application resources during the build process. Your build system should include the certificates from `/var/releases/ca-certificates/current/` into the application package.

**Version Tracking**: The certificate version is recorded in the application manifest so the client can identify which CA certificates it has embedded.

**Rotation Support**: During CA rotation periods, multiple intermediate CA certificates are included (current and new), allowing seamless transition as certificates are renewed.

**No Runtime Updates**: The client application does not attempt to update certificates at runtime. All certificate updates are delivered through your existing application update mechanism.

---

## Certificate Infrastructure

### Certificate Types

#### Client Certificate (Private CA)
- **Purpose**: Authenticate and identify client applications
- **Issuer**: Private Certificate Authority (your organization)
- **Validity**: 1-2 years
- **Distribution**: Dynamic issuance via token-based provisioning API

#### Server Certificate (Public CA)
- **Purpose**: Verify server identity to clients
- **Issuer**: Public Certificate Authority (Let's Encrypt, DigiCert, etc.)
- **Installation**: Standard SSL certificate on web server

### Certificate Validation Flow
1. Client initiates HTTPS connection with issued client certificate
2. Server validates client certificate against private CA
3. Client validates server certificate against public CA roots
4. Mutual authentication established for licensing operations

---

## Certificate Provisioning Workflow

### Complete Flow Diagram
```
┌─────────────────┐    HTTPS/Login    ┌─────────────────┐
│   Web Browser   │◄─────────────────►│  PHP Web App    │
│                 │                   │                 │
│ 1. User Login   │                   │ 2. Auth & Token │
│ 3. Copy Token   │                   │    Generation   │
└─────────────────┘                   └─────────────────┘
         │                                      │
         │ Out-of-band Token Transfer          │
         ▼                                      ▼
┌─────────────────┐    HTTPS/TLS      ┌─────────────────┐
│   Java Client   │◄─────────────────►│ PHP Cert API    │
│                 │                   │                 │
│ 4. Paste Token  │                   │ 5. Validate &   │
│ 5. Generate CSR │                   │    Issue Cert   │
│ 6. Store Cert   │                   │                 │
└─────────────────┘                   └─────────────────┘
         │                                      │
         ▼              mTLS/HTTPS              ▼
┌─────────────────┐◄─────────────────►┌─────────────────┐
│   Java Client   │                   │ PHP License API │
│ 7. License Ops  │                   │ 8. Validate &   │
│    with Client  │                   │    Authorize    │
│    Certificate  │                   │                 │
└─────────────────┘                   └─────────────────┘
```

### Provisioning Process Steps

#### Phase 1: Web Authentication & Token Generation
1. User logs into web portal with username/password
2. User selects subscription to provision certificate for
3. Server generates single-use token (1-hour expiration)
4. Web interface displays token with copy button
5. User manually copies token (out-of-band transfer)

#### Phase 2: Certificate Provisioning
6. Java application detects missing certificate on startup
7. Swing UI prompts user to paste provisioning token
8. Client generates RSA key pair locally (private key never leaves client)
9. Client creates certificate signing request (CSR) with embedded token
10. Client sends CSR + token to provisioning API via HTTPS
11. Server validates token (not expired, not used, valid subscription)
12. Server validates CSR and embedded token match
13. Private CA signs certificate with client's public key
14. Client receives and stores certificate + private key in keystore
15. Server marks token as used (single-use enforcement)

#### Phase 3: License Operations
16. Client uses issued certificate for mutual TLS authentication
17. All license operations validated via certificate
18. Subscription and feature checks performed securely

---

## Client Architecture (Java)

### Core Components

**Certificate Provisioning Manager**: Handles key generation, CSR creation, and certificate storage

**Embedded Trust Store Manager**: Manages CA certificates embedded at build time, supports multiple CAs during rotation

**Certificate Manager**: Manages client certificate storage and SSL context configuration for mTLS

**License Client**: Handles license validation and subscription status checks using mTLS

### Build-Time Certificate Embedding

```xml
<!-- pom.xml -->
<project>
    <build>
        <resources>
            <resource>
                <directory>src/main/resources</directory>
            </resource>
            <resource>
                <directory>${ca.config.dir}/current</directory>
                <targetPath>certs</targetPath>
                <includes>
                    <include>*.crt</include>
                </includes>
            </resource>
        </resources>
        
        <plugins>
            <!-- Copy CA certificates during build -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-resources-plugin</artifactId>
                <version>3.3.0</version>
                <executions>
                    <execution>
                        <id>copy-ca-certificates</id>
                        <phase>process-resources</phase>
                        <goals>
                            <goal>copy-resources</goal>
                        </goals>
                        <configuration>
                            <outputDirectory>${project.build.outputDirectory}/certs</outputDirectory>
                            <resources>
                                <resource>
                                    <directory>${ca.config.dir}/current</directory>
                                    <includes>
                                        <include>*.crt</include>
                                    </includes>
                                </resource>
                            </resources>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
            
            <!-- Validate certificate chain -->
            <plugin>
                <groupId>org.codehaus.mojo</groupId>
                <artifactId>exec-maven-plugin</artifactId>
                <version>3.1.0</version>
                <executions>
                    <execution>
                        <id>validate-certificates</id>
                        <phase>process-resources</phase>
                        <goals>
                            <goal>exec</goal>
                        </goals>
                        <configuration>
                            <executable>${project.basedir}/scripts/validate-ca-certs.sh</executable>
                            <arguments>
                                <argument>${project.build.outputDirectory}/certs</argument>
                            </arguments>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
            
            <!-- Add certificate version to manifest -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-jar-plugin</artifactId>
                <version>3.3.0</version>
                <configuration>
                    <archive>
                        <manifest>
                            <addDefaultImplementationEntries>true</addDefaultImplementationEntries>
                        </manifest>
                        <manifestEntries>
                            <CA-Certificate-Version>${ca.cert.version}</CA-Certificate-Version>
                            <Build-Date>${maven.build.timestamp}</Build-Date>
                        </manifestEntries>
                    </archive>
                </configuration>
            </plugin>
        </plugins>
    </build>
    
    <properties>
        <ca.config.dir>/var/releases/ca-certificates</ca.config.dir>
        <ca.cert.version>2024-12</ca.cert.version>
        <maven.build.timestamp.format>yyyy-MM-dd HH:mm:ss</maven.build.timestamp.format>
    </properties>
</project>
```

### Build Script Integration

```bash
#!/bin/bash
# scripts/validate-ca-certs.sh
# Validates that required CA certificates are present

CERT_DIR=$1

if [ ! -d "$CERT_DIR" ]; then
    echo "ERROR: Certificate directory not found: $CERT_DIR"
    exit 1
fi

# Check for required certificates
if [ ! -f "$CERT_DIR/root-ca.crt" ]; then
    echo "ERROR: Root CA certificate not found: $CERT_DIR/root-ca.crt"
    exit 1
fi

if [ ! -f "$CERT_DIR/intermediate-ca.crt" ]; then
    echo "ERROR: Intermediate CA certificate not found: $CERT_DIR/intermediate-ca.crt"
    exit 1
fi

# Validate certificate format
openssl x509 -in "$CERT_DIR/root-ca.crt" -noout -text > /dev/null 2>&1
if [ $? -ne 0 ]; then
    echo "ERROR: Invalid root CA certificate format"
    exit 1
fi

openssl x509 -in "$CERT_DIR/intermediate-ca.crt" -noout -text > /dev/null 2>&1
if [ $? -ne 0 ]; then
    echo "ERROR: Invalid intermediate CA certificate format"
    exit 1
fi

echo "Certificate validation passed"
echo "  - Root CA: $(openssl x509 -in $CERT_DIR/root-ca.crt -noout -subject)"
echo "  - Intermediate CA: $(openssl x509 -in $CERT_DIR/intermediate-ca.crt -noout -subject)"

# Check for backup intermediate (during rotation, optional)
if [ -f "$CERT_DIR/intermediate-ca-backup.crt" ]; then
    echo "  - Backup Intermediate CA found: $(openssl x509 -in $CERT_DIR/intermediate-ca-backup.crt -noout -subject)"
fi

exit 0
```

### Pre-Build Setup Script

```bash
#!/bin/bash
# scripts/prepare-build.sh
# Prepares CA certificates before build

CA_SOURCE_DIR="/etc/ca"
CA_TARGET_DIR="/var/releases/ca-certificates/current"

echo "Preparing CA certificates for build..."

# Create target directory if it doesn't exist
mkdir -p "$CA_TARGET_DIR"

# Copy current CA certificates
cp "$CA_SOURCE_DIR/root-ca/root-ca.crt" "$CA_TARGET_DIR/"
cp "$CA_SOURCE_DIR/intermediate-ca/intermediate-ca.crt" "$CA_TARGET_DIR/"

# During CA rotation, copy backup intermediate
if [ -f "$CA_SOURCE_DIR/intermediate-ca/intermediate-ca-new.crt" ]; then
    cp "$CA_SOURCE_DIR/intermediate-ca/intermediate-ca-new.crt" "$CA_TARGET_DIR/intermediate-ca-backup.crt"
    echo "Backup intermediate CA included for rotation"
fi

# Create version file
CERT_VERSION=$(date +%Y-%m)
echo "$CERT_VERSION" > "$CA_TARGET_DIR/version.txt"

echo "CA certificates prepared in $CA_TARGET_DIR"
echo "Certificate version: $CERT_VERSION"

# List prepared certificates
ls -lh "$CA_TARGET_DIR"/*.crt
```

### Build Execution

```bash
#!/bin/bash
# build-application.sh
# Main build script

set -e

echo "===== Starting Application Build ====="

# Step 1: Prepare CA certificates
echo "Step 1: Preparing CA certificates..."
./scripts/prepare-build.sh

# Step 2: Run Maven build
echo "Step 2: Building application with Maven..."
mvn clean package -Dca.cert.version=$(cat /var/releases/ca-certificates/current/version.txt)

# Step 3: Verify build artifacts
echo "Step 3: Verifying build artifacts..."
if [ ! -f "target/licensing-app.jar" ]; then
    echo "ERROR: Build artifact not found"
    exit 1
fi

# Step 4: Verify certificates are embedded
echo "Step 4: Verifying embedded certificates..."
jar tf target/licensing-app.jar | grep "certs/.*\.crt"

echo "===== Build Complete ====="
echo "Artifact: target/licensing-app.jar"
echo "CA Certificate Version: $(cat /var/releases/ca-certificates/current/version.txt)"
```

---

## Backend Architecture (PHP)

### Core Components

**Token Manager**: Generates, validates, and invalidates single-use provisioning tokens

**Certificate Authority Manager**: Issues client certificates by signing CSRs with intermediate CA

**Web Authentication Controller**: Handles user login and token generation

**Certificate Provisioning Controller**: Validates tokens, issues certificates, links to subscriptions

**License Management Controller**: Validates licenses via mTLS, checks subscription status

**CA Rotation Manager**: Handles CA rotation and certificate bundle preparation

**CA Health Monitor**: Monitors CA certificate expiration and system health

### Key Security Features

- CA private keys encrypted with passphrases stored in environment variables
- File permissions: CA keys in chmod 600, directories in chmod 700
- Single-use tokens with 1-hour expiration
- Token embedded in CSR for validation
- Comprehensive audit logging of all operations

---

## API Endpoints

### Web Frontend

**POST /web/login** - User authentication

**POST /web/provisioning/token** - Generate provisioning token

### Certificate Provisioning

**POST /api/certificates/provision** - Issue client certificate (no client cert required)

### License Operations (mTLS Required)

**POST /api/license/validate** - Validate license

**GET /api/subscription/status** - Get subscription details

**GET /api/license/features/{feature}** - Check feature availability

**GET /api/certificates/revocation/{serial}** - Check revocation status

---

## Security Considerations

### Token Security
- Single-use tokens with 1-hour expiration
- 256-bit cryptographically secure random generation
- Out-of-band transfer prevents interception
- Token embedded in CSR for additional validation

### Certificate Security
- Private keys never leave client, generated locally
- Hierarchical CA with offline root
- Intermediate CA private key encrypted with strong passphrase
- File permissions: CA keys chmod 600, directories chmod 700
- Regular rotation with 18-month transition period

### Network Security
- TLS 1.3 for all communications
- Mutual TLS for licensing operations
- Separate endpoints with different TLS requirements
- Rate limiting on all APIs

### Application Security
- Code obfuscation and anti-tampering
- Offline license validation with grace periods
- Comprehensive audit logging
- Secure certificate storage in client keystore

---

## Deployment Architecture

### Docker Compose Configuration

```yaml
version: '3.8'

services:
  nginx:
    image: nginx:latest
    ports:
      - "443:443"
    volumes:
      - ./ssl:/etc/nginx/ssl
      - ./nginx.conf:/etc/nginx/nginx.conf
      
  php-backend:
    image: php:8.1-fpm
    volumes:
      - ./backend:/var/www/html
      - ca-certs:/etc/ca:ro
    environment:
      - CA_KEY_PASSPHRASE=${CA_KEY_PASSPHRASE}
      - CA_INTERMEDIATE_PATH=/etc/ca/intermediate-ca
      
  mysql:
    image: mysql:8.0
    environment:
      - MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PASSWORD}
      - MYSQL_DATABASE=licensing
    volumes:
      - mysql-data:/var/lib/mysql

volumes:
  mysql-data:
  ca-certs:
```

### CA Initialization Script

```bash
#!/bin/bash
# initialize-ca.sh

CA_ROOT="/etc/ca"

# Create directory structure
mkdir -p $CA_ROOT/{root-ca,intermediate-ca,issued-certificates,private}
mkdir -p $CA_ROOT/intermediate-ca/crl

# Set permissions
chmod 700 $CA_ROOT/root-ca
chmod 700 $CA_ROOT/intermediate-ca
chmod 700 $CA_ROOT/private

# Generate Root CA
openssl genrsa -aes256 -out $CA_ROOT/root-ca/root-ca.key 4096
openssl req -new -x509 -days 7300 -key $CA_ROOT/root-ca/root-ca.key \
    -out $CA_ROOT/root-ca/root-ca.crt

# Generate Intermediate CA
openssl genrsa -aes256 -out $CA_ROOT/intermediate-ca/intermediate-ca.key 4096
openssl req -new -key $CA_ROOT/intermediate-ca/intermediate-ca.key \
    -out $CA_ROOT/intermediate-ca/intermediate-ca.csr

# Sign Intermediate CA with Root CA
openssl x509 -req -days 3650 \
    -in $CA_ROOT/intermediate-ca/intermediate-ca.csr \
    -CA $CA_ROOT/root-ca/root-ca.crt \
    -CAkey $CA_ROOT/root-ca/root-ca.key \
    -out $CA_ROOT/intermediate-ca/intermediate-ca.crt

echo "CA initialization complete"
```

---

## Database Schema

```sql
-- Users
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(255) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_username (username)
);

-- Subscriptions
CREATE TABLE subscriptions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    client_cert_serial VARCHAR(255) UNIQUE NULL,
    subscription_type ENUM('basic', 'premium', 'enterprise'),
    start_date DATETIME NOT NULL,
    end_date DATETIME NOT NULL,
    status ENUM('active', 'suspended', 'cancelled'),
    product_ids JSON,
    FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_status (status),
    INDEX idx_cert_serial (client_cert_serial)
);

-- Provisioning Tokens
CREATE TABLE provisioning_tokens (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    token VARCHAR(255) UNIQUE NOT NULL,
    user_id BIGINT NOT NULL,
    subscription_id BIGINT NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (subscription_id) REFERENCES subscriptions(id),
    INDEX idx_token (token),
    INDEX idx_expires (expires_at)
);

-- CA Certificates
CREATE TABLE ca_certificates (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    certificate_type ENUM('root', 'intermediate') NOT NULL,
    serial_number VARCHAR(255) UNIQUE NOT NULL,
    subject_dn VARCHAR(512) NOT NULL,
    not_before TIMESTAMP NOT NULL,
    not_after TIMESTAMP NOT NULL,
    certificate_pem TEXT NOT NULL,
    file_path VARCHAR(512),
    status ENUM('active', 'retired', 'revoked') DEFAULT 'active',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_type_status (certificate_type, status),
    INDEX idx_serial (serial_number)
);

-- Issued Certificates
CREATE TABLE issued_certificates (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    serial_number VARCHAR(255) UNIQUE NOT NULL,
    user_id BIGINT NOT NULL,
    subscription_id BIGINT NOT NULL,
    common_name VARCHAR(255) NOT NULL,
    issued_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    status ENUM('active', 'revoked', 'expired'),
    certificate_pem TEXT NOT NULL,
    issuing_ca_serial VARCHAR(255) NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (subscription_id) REFERENCES subscriptions(id),
    INDEX idx_serial (serial_number),
    INDEX idx_user (user_id),
    INDEX idx_status (status)
);

-- Certificate Revocations
CREATE TABLE certificate_revocations (
    serial_number VARCHAR(255) PRIMARY KEY,
    revocation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    reason ENUM('unspecified', 'key_compromise', 'superseded', 'cessation_of_operation'),
    FOREIGN KEY (serial_number) REFERENCES issued_certificates(serial_number)
);
```

---

## Implementation Timeline

### Phase 1: CA Infrastructure & Basic Provisioning (4 weeks)
- Set up hierarchical private CA infrastructure with file-based storage
- Implement root CA (offline) and intermediate CA (online) generation
- Create secure file storage with appropriate permissions
- Develop certificate provisioning API with token validation
- Implement Java client certificate provisioning dialog
- Database schema implementation

### Phase 2: Integration & Testing (3 weeks)
- Integrate certificate authentication with license APIs
- Build-time certificate embedding process
- Comprehensive end-to-end testing
- Security testing and vulnerability assessment
- Documentation for build integration

### Phase 3: Deployment & Operations (2 weeks)
- Production environment setup
- Monitoring and alerting configuration
- Staff training on CA operations
- Operational runbooks
- Go-live and initial support

### Phase 4: CA Rotation Planning (1 week)
- Document CA rotation procedures
- Coordinate with application release team
- Establish monitoring for certificate expiration

**Total Timeline**: 10 weeks

---

## Testing Strategy

### Unit Testing
- Token generation and validation logic
- Certificate provisioning request handling
- CSR creation and validation
- Database operations
- Certificate chain validation

### Integration Testing
- End-to-end certificate provisioning flow
- Token lifecycle management
- mTLS establishment with issued certificates
- License validation with client certificates
- CA rotation simulation

### Security Testing
- Token brute force prevention
- Certificate validation bypass attempts
- TLS configuration testing
- File permission security
- Passphrase protection
- Certificate revocation

### Operational Testing
- CA health monitoring
- Certificate expiration alerting
- Backup and recovery procedures
- Build integration verification

---

## Operational Procedures

### Regular Maintenance

**Daily Tasks**:
- Monitor CA health checks
- Review certificate issuance logs
- Generate fresh CRL

**Weekly Tasks**:
- Backup CA infrastructure
- Review security logs
- Check certificate expiration dates

**Monthly Tasks**:
- Test backup restore procedures
- Review and clean up expired tokens
- Update documentation

### CA Rotation Procedure

1. **18 months before expiration**: Begin CA rotation process
2. **Generate new intermediate CA**: Create new CA and have root CA sign it
3. **Prepare certificate bundle**: Place certificates in `/var/releases/ca-certificates/next/`
4. **Coordinate with release team**: Schedule application update with new certificates
5. **Monitor adoption**: Track client updates via certificate usage
6. **Transition issuance**: Switch to new CA after 12 months (6 months of client adoption)
7. **Retire old CA**: Stop accepting old CA certificates after 18 months

---

## Conclusion

This design provides a secure, maintainable subscription licensing solution using mutual TLS authentication. Key benefits include:

- **Simplified Infrastructure**: File-based CA storage, no specialized hardware required
- **Integration Friendly**: Leverages existing application update process
- **Strong Security**: Hierarchical CA, encrypted keys, out-of-band token provisioning
- **Operational Excellence**: Automated monitoring, clear procedures, comprehensive logging
- **Cost-Effective**: Standard infrastructure, no HSM or complex runtime updates

The system is designed to operate reliably with minimal operational overhead while maintaining enterprise-grade security.