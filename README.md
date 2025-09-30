# Subscription Licensing System with Certificate Authentication

A production-ready subscription licensing system with **complete reference implementation** that enables clients to operate offline for extended periods while maintaining security through certificate-based authentication and preventing casual license sharing.

**This repository contains both the design specification AND a working reference implementation.**

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
- **Configurable device limits** per subscription (1, 5, 10+ devices)
- **Device management** via customer portal for enrollment tracking and revocation
- **User-friendly device identification** (device name + platform) for easy management
- **Grace periods**: 5 days (monthly) / 14 days (annual)
- **Extended offline operation** for full subscription periods
- **Automatic renewal** with background checks
- **Payment status tracking** and grace period handling

### Multi-Platform Support
- Windows (Certificate Store)
- macOS (Keychain)
- Linux (encrypted storage)
- Cross-platform device identification

### Device Management
- **Portal-based device enrollment tracking** with identifying information
- **Manual device revocation** to free up enrollment slots
- **Device limit enforcement** prevents unlimited certificate sharing
- **Lost device recovery** via portal-based certificate revocation
- **Secure device transfer** for hardware upgrades via migration feature

### License Migration
- **Secure device transfer** for hardware upgrades
- **24-hour migration tokens** with single-use validation
- **Certificate-verified transfers** preventing unauthorized moves
- **Does not increase device limit** - transfers license between devices

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

## Quick Start - Reference Implementation

### 1. Set Up Certificate Authority

```bash
cd ca
./docker-setup.sh
```

This creates:
- Root CA (offline, 20-year validity)
- Intermediate CA (online, 10-year validity)
- License signing keys (separate from CA)

**No OpenSSL installation required** - runs entirely in Docker.

### 2. Start Server (PHP + MySQL)

```bash
cd server
cp .env.example .env
# Edit .env with your configuration
./docker-helper.sh start
./docker-helper.sh migrate
```

Server endpoints:
- TLS: https://localhost:8443
- mTLS: https://localhost:9443
- PHPMyAdmin: http://localhost:8081

### 3. Build Client (macOS Java)

```bash
cd client/macos-java
mvn clean package
java -jar target/license-client-1.0.0.jar
```

**Requirements:**
- JDK 25 (for Virtual Threads and modern Java features)
- macOS 10.15 or later
- Maven 3.8+

## Repository Structure

This repository contains both specification and implementation:

```
sub-lic-spec/
├── spec.md                 # Complete technical specification (71KB)
├── ReferenceImplGuide.md   # Implementation guide (90KB+)
├── README.md               # This file
├── Sample_EULA.md          # Example EULA with free/premium tiers
├── CONTRIBUTING.md         # Contribution guidelines
│
├── ca/                     # Certificate Authority (IMPLEMENTED)
│   ├── docker-setup.sh     # Master CA setup script
│   ├── scripts/            # CA initialization scripts
│   ├── config/             # OpenSSL configurations
│   └── README.md
│
├── server/                 # PHP Server Implementation (IMPLEMENTED)
│   ├── public/             # Web root
│   ├── src/                # PHP application code
│   ├── config/             # Configuration files
│   ├── database/           # Migrations and seeds
│   ├── docker/             # Docker configs (Apache TLS/mTLS)
│   ├── docker-compose.yml  # Container orchestration
│   ├── docker-helper.sh    # Helper commands
│   └── README.md
│
└── client/                 # Client Implementations
    └── macos-java/         # macOS Java Client (IMPLEMENTED)
        ├── src/            # Java source code
        ├── pom.xml         # Maven configuration
        └── README.md
```

## What's Implemented

✅ **Certificate Authority**
- Containerized CA setup (no host OpenSSL required)
- Root CA and Intermediate CA generation
- Separate license signing keys
- OpenSSL configuration files

✅ **Server (PHP)**
- Docker-based deployment (PHP 8.1 + Apache + MySQL)
- Database migrations (7 tables)
- Dual-port configuration (TLS + mTLS)
- API controllers (stubs for Certificate, License, Migration, Portal)
- Service layer (PrivateCAService, LicenseTokenService)
- Configuration management
- Helper scripts for common operations

✅ **Client (macOS Java)**
- JDK 25 with modern Java features
- macOS Keychain integration
- Device identification (hardware-based)
- Certificate management
- Swing UI with enrollment and migration dialogs
- Configuration management

## Development Status

This is a **reference implementation**. The architecture and infrastructure are complete:
- ✅ Directory structure
- ✅ Configuration files
- ✅ Database schema
- ✅ Docker setup
- ✅ Build system (Maven, Composer)
- ✅ API structure
- ✅ UI framework

Core business logic is stubbed - marked with `TODO` comments showing what needs to be implemented. This provides a complete scaffold for building a production system.

## Prerequisites

**Development Machine:**
- Docker + Docker Compose
- JDK 25
- Maven 3.8+
- macOS (for client development)

**No OpenSSL installation required** - CA operations run in containers.

## Usage

### User Onboarding Flow

1. **Portal: Generate Enrollment Token**
   ```
   User logs into customer portal
   → Verifies active subscription
   → System checks device limit
   → If at limit: Must revoke existing device first
   → If under limit: Requests enrollment token
   → Receives single-use token (7-day validity)
   ```

2. **Client: Certificate Enrollment**
   ```
   User enters token in application
   → Provides device name for identification (e.g., "Work Laptop - Windows")
   → Client generates RSA key pair
   → Submits CSR + device info to server (TLS + token)
   → Server validates token and checks device limit
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

### Device Management

**Viewing Enrolled Devices:**
```
User logs into customer portal
→ Views list of enrolled devices with:
  - Device name (user-provided)
  - Platform (Windows, macOS, Linux)
  - Enrollment date
  - Last seen timestamp
  - Certificate status (active/revoked)
→ Can revoke specific device certificates
```

**Device Limit Enforcement:**
- Each subscription has a device limit (1, 5, 10+)
- Cannot enroll additional devices once limit reached
- Must revoke existing device via portal to free up slot
- Device name and platform help identify which device to revoke
- Revoked certificates added to CRL immediately

**Enrolling Additional Devices:**
```
If under device limit:
  → Generate new enrollment token
  → Install app on new device
  → Enter token and device name
  → Complete enrollment

If at device limit:
  → View enrolled devices in portal
  → Revoke device no longer needed
  → Generate new enrollment token
  → Enroll new device
```

**Lost Device Recovery:**
```
User logs into portal
→ Views enrolled devices list
→ Identifies lost device by name/platform/date
→ Revokes lost device certificate
→ Device limit slot freed immediately
→ Can enroll replacement device if needed
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

**When to Use Migration:**
- Replacing hardware (new computer, upgraded device)
- Transferring license to different device
- **Note:** Migration does not bypass device limits

**Migration Process:**

```
Old Device: 
  → Request migration token (mTLS)
  → Display token to user

New Device:
  → Install same certificate (must be previously enrolled OR have available device slot)
  → Submit migration token + new device ID (mTLS)
  → Receive new license token
  → Old device license deactivated

Important:
  - Migration transfers license between devices
  - Does not increase your device limit
  - If new device not previously enrolled and at limit, must revoke another device first
```

## API Endpoints

### Portal (Session Auth)
- `POST /portal/enrollment/generate` - Generate enrollment token (checks device limit)
- `GET /portal/devices` - List enrolled devices for user
- `DELETE /portal/devices/{fingerprint}` - Revoke specific device certificate
- `POST /portal/account/delete/confirm` - Delete account & revoke all certificates

### Certificate Management (TLS + Token)
- `POST /api/certificate/enroll` - Enroll with token + CSR + device info

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
- **Device limit enforcement**: Configurable limits prevent unlimited sharing
- **Portal-based management**: Manual revocation requires authentication
- **Migration control**: Secure, audited process for legitimate transfers
- **Offline validation**: Both certificate and license checked locally

### Threat Mitigation

- **Token extraction**: Device-specific encryption makes tokens useless elsewhere
- **Certificate sharing**: Requires platform keystore access + private key extraction + device limit allows controlled sharing within subscription
- **License theft**: Cannot use without matching certificate fingerprint
- **Man-in-the-middle**: mTLS provides mutual authentication
- **Unlimited sharing**: Device limits prevent casual distribution

## Platform-Specific Implementation

### Java (Desktop)
```java
// Cross-platform KeyStore abstraction
// See client/java/src/CertificateManager.java

// Windows: KeyStore.getInstance("Windows-MY")
// macOS: Native Keychain via JNA
// Linux: Custom encrypted storage
```

## Monitoring & Operations

### Key Metrics
- Certificate issuance/renewal rates
- Device enrollment patterns and limit utilization
- License activation/renewal success rates
- Grace period utilization
- Migration frequency
- Device revocation patterns
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

### Device Management
- Device limits enforced at enrollment token generation
- Portal provides device listing and revocation
- Certificate revocation via CRL (24-hour update cycle)
- User-friendly device identification

## Documentation

**[Complete Specification (spec.md)](spec.md)** - Comprehensive technical documentation including:
- System architecture and security model
- Certificate and license workflows
- Client certificate enrollment process
- Device identification and management
- API endpoint specifications
- Server implementation (PHP)
- Client implementation examples
- Database schema
- Security considerations
- Deployment and operational procedures
- Error codes and troubleshooting

**[Reference Implementation Guide (ReferenceImplGuide.md)](ReferenceImplGuide.md)** - Detailed implementation guide with:
- Complete project structure
- Development environment setup (containerized server, native client)
- Configuration management
- Database migrations
- Private CA setup scripts
- Server implementation (PHP in Docker containers)
- Client implementation (Java 25 on macOS)
- Build and packaging
- Testing strategy
- Deployment procedures

## Error Codes

| Code | Description | Action |
|------|-------------|---------|
| 1001 | Invalid client certificate | Re-enroll |
| 1007 | Certificate expired | Renew certificate |
| 1008 | Certificate revoked | Contact support |
| 1018 | Device limit reached | Revoke device in portal |
| 2001 | License not activated | Activate license |
| 2002 | Subscription expired | Renew subscription |
| 2008 | License-certificate mismatch | Re-activate |
| 3001 | Certificate valid but no license | Complete activation |

## Contributing

Please read [CONTRIBUTING.md](CONTRIBUTING.md) for details on our code of conduct and the process for submitting pull requests.

## License

This project is licensed under the MIT License - see [LICENSE](LICENSE) file for details.

## Support

- **Documentation**: See [spec.md](spec.md) for complete technical specification
- **Issues**: GitHub issue tracker
- **Security**: alan@whitemail.net

## Roadmap

- [ ] Hardware attestation (TPM, Secure Enclave)
- [ ] Cloud-based license synchronization
- [ ] Family/team sharing with sub-certificates
- [ ] Usage-based billing integration
- [ ] Certificate-based SSO
- [ ] Mobile platform support (iOS, Android)

---

**Note**: This system is designed for production use with legitimate subscribers. It focuses on preventing casual sharing while maintaining excellent user experience for authorized users. Device limits provide flexible multi-device support while preventing unlimited distribution.