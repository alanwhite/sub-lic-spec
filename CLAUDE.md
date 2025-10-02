# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a **design specification AND reference implementation repository** for a subscription licensing system with certificate authentication. It contains comprehensive documentation AND working implementation code. The system uses a dual-layer security architecture with X.509 certificates for identity and JWT tokens for subscription management.

## Repository Structure

### Documentation
- `spec.md` - Complete technical design specification (71KB detailed spec)
- `ReferenceImplGuide.md` - Implementation guide with concrete examples and project structure (90KB+ detailed implementation)
- `README.md` - Project overview with quick start guide
- `CONTRIBUTING.md` - Contribution guidelines
- `Sample_EULA.md` - Example end-user license agreement with free and premium tiers

### Implementation
- `ca/` - Certificate Authority setup scripts and configurations (IMPLEMENTED)
  - `docker-setup.sh` - Master setup script (runs in Docker)
  - `scripts/` - CA initialization scripts (01-root-ca, 02-intermediate-ca, 03-license-keys)
  - `config/` - OpenSSL configuration files
- `server/` - PHP server implementation (IMPLEMENTED)
  - `public/` - Web root with index.php entry point
  - `src/` - PHP application code (controllers, services, models)
  - `config/` - Configuration management
  - `database/migrations/` - 7 SQL migration files
  - `docker-compose.yml` - Container orchestration
  - `docker-helper.sh` - Development helper commands
- `client/macos-java/` - macOS Java client (IMPLEMENTED)
  - `src/main/java/` - Java application code
  - `pom.xml` - Maven build configuration (requires JDK 25)
  - UI with enrollment and migration dialogs

## Architecture Overview

The system implements a two-layer security model:

1. **Certificate Layer**: Private CA with hierarchical structure (Root CA → Intermediate CA → Client Certificates)
2. **License Layer**: JWT tokens for subscription management bound to certificates

Key components:
- **mTLS Authentication**: Mutual TLS for all license operations
- **Offline Operation**: Extended offline capability for full subscription periods
- **Device Migration**: Secure license transfer between devices
- **Multi-Platform Support**: Windows, macOS, Linux with platform-specific secure storage

## Common Development Tasks

### Certificate Authority
```bash
cd ca
./docker-setup.sh  # Sets up entire CA infrastructure in Docker
```

### Server (PHP)
```bash
cd server
./docker-helper.sh start      # Start all containers
./docker-helper.sh stop       # Stop containers
./docker-helper.sh migrate    # Run database migrations
./docker-helper.sh logs       # View logs
./docker-helper.sh shell      # Open shell in web container
./docker-helper.sh shell-db   # Open MySQL shell
composer run test             # Run tests (inside container)
composer run lint             # Lint code (inside container)
```

### Client (macOS Java)
```bash
cd client/macos-java
mvn clean package    # Build JAR
mvn test             # Run tests
java -jar target/license-client-1.0.0.jar  # Run application
```

**Important:** Client requires JDK 25 for Virtual Threads and modern Java features.

## Key Security Principles

When working with this specification:

1. **Two-phase JWT delivery** - JWT tokens delivered via mTLS (Phase 2), NOT during enrollment (Phase 1)
   - Phase 1 (TLS): Certificate enrollment - client receives certificate + CA chain
   - Phase 2 (mTLS): JWT acquisition - client authenticates with certificate to receive JWT
   - Prevents JWT theft during enrollment and ensures only certificate holders get licenses
2. **Certificate and license keys are separate** - CA keys vs license signing keys
3. **Device binding is critical** - Hardware-backed storage prevents casual sharing
4. **Certificate-license binding** - JWT contains cert_fingerprint claim, cryptographically bound
5. **Grace periods vary by subscription type** - 5 days (monthly), 14 days (annual)
6. **Migration tokens are single-use** - 24-hour validity with device verification
7. **Production-ready security** - TLS 1.3 only, hostname verification, certificate revocation (OCSP/CRL)

## Database Schema

The system uses 7 main tables:
- `users` - User accounts and organizations
- `subscriptions` - Monthly/annual subscription records
- `enrollment_tokens` - Single-use tokens for certificate enrollment
- `certificates` - Client certificate records with fingerprints
- `clients` - Device registrations with platform-specific IDs
- `licenses` - Active license tokens bound to certificates
- `migrations` - License transfer records

## API Structure

- **Portal endpoints** (`/portal/*`) - Session-based authentication for user management
- **Certificate endpoints** (`/api/certificate/*`) - TLS + token authentication for enrollment
- **License endpoints** (`/api/license/*`) - mTLS required for all operations
- **Migration endpoints** (`/api/migration/*`) - mTLS for secure device transfers

## Implementation Notes

- This is a **specification AND reference implementation repository**
- Reference implementation provides complete scaffold with architecture and infrastructure
- Core business logic is stubbed with TODO comments showing what to implement
- Focus on security: certificate validation, proper mTLS configuration, secure storage
- Platform-specific considerations: macOS (Keychain) implemented, Windows/Linux in spec
- Production deployment requires proper CA infrastructure and certificate management

### Development Approach
When adding new features:
1. Review relevant section in `spec.md` for requirements
2. Check `ReferenceImplGuide.md` for implementation patterns
3. Locate the appropriate controller/service file in `server/src/`
4. Implement the TODO-marked methods
5. Add tests in `server/tests/` or `client/src/test/`
6. Update documentation if behavior changes

## Implementation Status

**Completed (Production-Ready):**
- ✅ Certificate Authority setup (containerized, no host OpenSSL needed)
- ✅ Server directory structure and configuration
- ✅ Database schema (7 migration files)
- ✅ Docker setup (docker-compose.yml, Apache configs for TLS/mTLS dual-port)
- ✅ API structure (controllers with method stubs)
- ✅ Service layer (PrivateCAService, LicenseTokenService with stubs)
- ✅ Client UI framework (Swing with enrollment and migration dialogs)
- ✅ Client infrastructure (CertificateManager, DeviceIdentifier)
- ✅ **Production mTLS implementation** (TLS 1.3, hostname verification, cert revocation)
- ✅ **JWT validation with RS256** (signature verification, expiration checking)
- ✅ **Environment-aware configuration** (LicenseConfig for dev/prod switching)
- ✅ **Certificate-JWT binding** (cert_fingerprint validation)
- ✅ **Two-phase authentication flow** (TLS enrollment → mTLS JWT acquisition)
- ✅ **PKCS#12 certificate management** (full client cert lifecycle)

**To Be Implemented (marked with TODO):**
- ⏳ Server PHP business logic (certificate issuance endpoint, full JWT generation)
- ⏳ Complete server-side JWT issuance flow
- ⏳ Tests (unit and integration for both server and client)
- ⏳ Portal UI pages (HTML/JS for web interface)
- ⏳ Platform-specific secure storage (Windows Certificate Store, Linux encrypted files)

## Document Changes

Recent updates (latest):
- **Production-ready client implementation completed** with JDK 25
- **Two-phase JWT delivery** documented as core security feature across all docs
- **Security rationale** added explaining why JWT must be delivered via mTLS (Phase 2)
- `spec.md` - Added "Security Rationale for Two-Phase JWT Delivery" section
- `spec.md` - Updated sequence diagram showing Phase 1 (TLS) and Phase 2 (mTLS)
- `ReferenceImplGuide.md` - Added "Production-Ready Security Implementation" section
- `ReferenceImplGuide.md` - Added code examples for TLS 1.3, OCSP/CRL, hostname verification
- `README.md` - Updated user onboarding flow with two-phase model
- `README.md` - Enhanced implementation status with production security features
- `CLAUDE.md` - Updated with two-phase authentication and production features

Earlier updates:
- Repository transformed from specification-only to specification + reference implementation
- Complete implementation scaffold added for CA, server, and client
- Production mTLS implementation with environment-aware configuration
- JWT validation with RS256 signature verification
- Certificate-JWT cryptographic binding implemented