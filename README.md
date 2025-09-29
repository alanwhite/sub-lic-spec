# Subscription Licensing System with Certificate Authentication

A production-ready subscription licensing system that enables clients to operate offline for extended periods while maintaining security through certificate-based authentication and preventing casual license sharing.

## Overview

This system implements a two-layer security architecture:

- **Certificate Layer**: X.509 certificates from a Private CA provide cryptographic identity
- **License Layer**: JWT tokens provide subscription management and feature entitlements

The dual-layer approach separates identity (WHO you are) from authorization (WHAT you can access), providing robust security while enabling flexible subscription management.

## Key Features

### Security & Authentication
- **Mutual TLS (mTLS)** for all license operations after initial enrollment
- **Private CA infrastructure** with hierarchical certificate authority (Root CA → Intermediate CA → Client Certs)
- **Certificate-bound licenses** with cryptographic binding to prevent unauthorized transfers
- **Device-specific encryption** using hardware keystores
- **Separate key management** for CA operations and license signing

### Subscription Management
- **Monthly and annual** subscription models
- **Grace periods**: 5 days (monthly) / 14 days (annual)
- **Extended offline operation** for full subscription periods
- **Automatic renewal** with background checks
- **Payment status tracking** and grace period handling

### Multi-Platform Support
- Windows (Certificate Store)
- macOS (Keychain)
- Linux (encrypted storage)
- Cross-platform device identification

### License Migration
- **Secure device transfer** for hardware upgrades
- **24-hour migration tokens** with single-use validation
- **Certificate-verified transfers** preventing unauthorized moves

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                   Certificate Layer                         │
│        (Certificate Provisioning & mTLS)                    │
│                                                             │
│  ┌──────────────┐         ┌──────────────┐                  │
│  │ Private CA   │         │ Client Cert  │                  │
│  │ - Root CA    │────────►│ Provisioning │                  │
│  │ - Inter. CA  │         │ - Web Token  │                  │
│  └──────────────┘         └──────────────┘                  │
└─────────────────────────────────────────────────────────────┘
                              │
                              │ Authenticated mTLS Channel
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                   License Layer                             │
│         (Subscription & License Management)                 │
│                                                             │
│  ┌──────────────┐         ┌──────────────┐                  │
│  │ JWT License  │         │ Subscription │                  │
│  │ Tokens       │◄────────│ Management   │                  │
│  │ - Device ID  │         │ - Grace      │                  │
│  │ - Offline    │         │ - Migration  │                  │
│  └──────────────┘         └──────────────┘                  │
└─────────────────────────────────────────────────────────────┘
```

## Getting Started

### Prerequisites

**Server Requirements:**
- PHP 7.4+ with OpenSSL support
- MySQL/PostgreSQL database
- Web server (Apache/Nginx) with TLS/mTLS configuration
- Private CA infrastructure setup

**Client Requirements:**
- Platform-specific development environment (Flutter, Java, Swift, etc.)
- OpenSSL libraries for certificate operations
- Secure storage APIs (Keychain, KeyStore, etc.)

### Installation

1. **Set up Private CA**
   ```bash
   # Generate Root CA (offline, 20-year validity)
   openssl genrsa -aes256 -out root-ca.key 4096
   openssl req -new -x509 -days 7300 -key root-ca.key -out root-ca.crt
   
   # Generate Intermediate CA (online, 10-year validity)
   openssl genrsa -aes256 -out intermediate-ca.key 4096
   openssl req -new -key intermediate-ca.key -out intermediate-ca.csr
   openssl x509 -req -in intermediate-ca.csr -CA root-ca.crt -CAkey root-ca.key \
     -CAcreateserial -out intermediate-ca.crt -days 3650
   ```

2. **Generate License Signing Keys**
   ```bash
   # Separate from CA keys - used for JWT signing
   openssl genrsa -aes256 -out license-signing.key 2048
   openssl rsa -in license-signing.key -pubout -out license-signing.pub
   ```

3. **Configure Database**
   ```bash
   mysql -u root -p < schema/01_users_and_subscriptions.sql
   mysql -u root -p < schema/02_certificates.sql
   mysql -u root -p < schema/03_licenses.sql
   ```

4. **Configure Web Server**
   - See `docs/server-setup.md` for Apache/Nginx configuration
   - Configure dual authentication modes:
     - TLS-only for certificate enrollment endpoints
     - mTLS for all license operation endpoints

5. **Build Client Application**
   - Embed CA certificate chain
   - Embed license server public key
   - Configure platform-specific certificate storage

## Usage

### User Onboarding Flow

1. **Portal: Generate Enrollment Token**
   ```
   User logs into customer portal
   → Requests enrollment token
   → Receives single-use token (7-day validity)
   ```

2. **Client: Certificate Enrollment**
   ```
   User enters token in application
   → Client generates RSA key pair
   → Submits CSR to server (TLS + token)
   → Receives certificate + initial license token
   → Installs certificate in platform keystore
   ```

3. **Application Launch**
   ```
   Client validates certificate (Layer 1)
   → Validates license token (Layer 2)
   → Checks subscription status
   → Grants access to entitled features
   ```

### License Renewal (Background)

```
Client checks subscription 7 days before expiry
→ Connects via mTLS with client certificate
→ Server validates certificate + subscription
→ Returns new license token if payment received
→ Client stores encrypted token
```

### Device Migration

```
Old Device: 
  → Request migration token (mTLS)
  → Display token to user

New Device:
  → Install same certificate
  → Submit migration token + new device ID (mTLS)
  → Receive new license token
  → Old device license deactivated
```

## API Endpoints

### Portal (Session Auth)
- `POST /portal/enrollment/generate` - Generate enrollment token
- `POST /portal/account/delete/confirm` - Delete account & revoke certificates

### Certificate Management (TLS + Token)
- `POST /api/certificate/enroll` - Enroll with token + CSR

### License Operations (mTLS Required)
- `POST /api/license/activate` - Initial activation
- `POST /api/license/renew` - Check renewal
- `POST /api/license/migrate/initiate` - Start migration
- `POST /api/license/migrate/complete` - Complete migration

## Security Considerations

### Two-Layer Authentication

**Why separate certificates and license tokens?**

1. **Different lifecycles**: Certificates last 2 years; licenses renew monthly/annually
2. **Different purposes**: Certificates prove identity; licenses prove subscription status
3. **Key separation**: CA keys vs. license signing keys - compromised license key doesn't affect CA
4. **Flexibility**: Update entitlements without reissuing certificates

### Anti-Piracy Measures

- **Device binding**: Hardware-backed identification with encrypted storage
- **Certificate-license binding**: Cryptographic binding prevents separation
- **Migration control**: Secure, audited process for legitimate transfers
- **Offline validation**: Both certificate and license checked locally

### Threat Mitigation

- **Token extraction**: Device-specific encryption makes tokens useless elsewhere
- **Certificate sharing**: Requires platform keystore access + private key extraction
- **License theft**: Cannot use without matching certificate fingerprint
- **Man-in-the-middle**: mTLS provides mutual authentication

## Platform-Specific Implementation

### Flutter (Multi-platform)
```dart
// Use platform channels for native certificate operations
// See client/flutter/lib/certificate_manager.dart

// Windows: Windows Certificate Store
// macOS: Keychain Services
// Linux: Encrypted file storage
// Android: Android Keystore
// iOS: Secure Enclave
```

### Java (Desktop)
```java
// Cross-platform KeyStore abstraction
// See client/java/src/CertificateManager.java

// Windows: KeyStore.getInstance("Windows-MY")
// macOS: Native Keychain via JNA
// Linux: Custom encrypted storage
```

### Swift (macOS/iOS)
```swift
// Keychain Services with Secure Enclave
// See client/swift/CertificateManager.swift
```

## Monitoring & Operations

### Key Metrics
- Certificate issuance/renewal rates
- License activation/renewal success rates
- Grace period utilization
- Migration frequency
- mTLS handshake failures

### Certificate Management
- Root CA: Offline, used only for intermediate CA signing
- Intermediate CA: Online, issues client certificates
- Certificate validity: 2 years
- Renewal window: 30 days before expiry

### License Management
- JWT signing with server private key (separate from CA)
- Subscription-aligned validity periods
- Grace periods: 5 days (monthly), 14 days (annual)
- Device-encrypted storage

## Documentation

- **[Design Document](spec.md)** - Complete technical specification
- **[API Reference](docs/api.md)** - Endpoint documentation
- **[Security Model](docs/security.md)** - Detailed security analysis
- **[Deployment Guide](docs/deployment.md)** - Production setup
- **[Client SDK Guide](docs/client-sdk.md)** - Implementation examples

## Error Codes

| Code | Description | Action |
|------|-------------|---------|
| 1001 | Invalid client certificate | Re-enroll |
| 1007 | Certificate expired | Renew certificate |
| 1008 | Certificate revoked | Contact support |
| 2001 | License not activated | Activate license |
| 2002 | Subscription expired | Renew subscription |
| 2008 | License-certificate mismatch | Re-activate |
| 3001 | Certificate valid but no license | Complete activation |

## Contributing

Please read [CONTRIBUTING.md](CONTRIBUTING.md) for details on our code of conduct and the process for submitting pull requests.

## License

This project is licensed under the MIT License - see [LICENSE](LICENSE) file for details.

## Support

- **Documentation**: See `docs/` directory
- **Issues**: GitHub issue tracker
- **Security**: security@example.com

## Roadmap

- [ ] Hardware attestation (TPM, Secure Enclave)
- [ ] Cloud-based license synchronization
- [ ] Family/team sharing with sub-certificates
- [ ] Usage-based billing integration
- [ ] Certificate-based SSO

---

**Note**: This system is designed for production use with legitimate subscribers. It focuses on preventing casual sharing while maintaining excellent user experience for authorized users.