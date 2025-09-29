# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a **design specification repository** for a subscription licensing system with certificate authentication. It contains comprehensive documentation, not implementation code. The system uses a dual-layer security architecture with X.509 certificates for identity and JWT tokens for subscription management.

## Repository Structure

- `spec.md` - Complete technical design specification (71KB detailed spec)
- `ReferenceImplGuide.md` - Implementation guide with concrete examples and project structure (90KB+ detailed implementation)
- `README.md` - Project overview with quick start guide
- `CONTRIBUTING.md` - Contribution guidelines for specification improvements
- `Sample_EULA.md` - Example end-user license agreement with free and premium tiers

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

Based on the ReferenceImplGuide.md, when implementing this specification:

### Server (PHP)
- **Build**: `server/build.sh` (creates deployable package)
- **Test**: `composer run test` or `phpunit`
- **Lint**: `composer run lint` or `phpcs --standard=PSR12 src/`
- **Test Coverage**: `composer run test:coverage`

### Client (Java Desktop)
- **Build**: `mvn clean package` or `client/java-desktop/build.sh`
- **Test**: `mvn test`

### Client (Flutter Multi-platform)
- **Build**: `client/flutter/build.sh` (builds all platforms)
- **Test**: `flutter test`
- **Code Generation**: `flutter pub run build_runner build --delete-conflicting-outputs`

### Certificate Authority Setup
- **Initialize Root CA**: `ca/scripts/01-setup-root-ca.sh`
- **Setup Intermediate CA**: `ca/scripts/02-setup-intermediate-ca.sh`
- **Generate License Keys**: `ca/scripts/03-generate-license-keys.sh`

## Key Security Principles

When working with this specification:

1. **Certificate and license keys are separate** - CA keys vs license signing keys
2. **Device binding is critical** - Hardware-backed storage prevents casual sharing
3. **Certificate-license binding** - Cryptographic binding prevents token reuse
4. **Grace periods vary by subscription type** - 5 days (monthly), 14 days (annual)
5. **Migration tokens are single-use** - 24-hour validity with device verification

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

- This is a **specification repository** - implementations would be separate projects
- Focus on security: certificate validation, proper mTLS configuration, secure storage
- Platform-specific considerations documented for Windows (Certificate Store), macOS (Keychain), Linux (encrypted files)
- Production deployment requires proper CA infrastructure and certificate management

## Document Changes

Recent updates (based on commit history):
- `Sample_EULA.md` updated with free tier licensing terms
- `ReferenceImplGuide.md` expanded with comprehensive implementation details
- All files reflect this is a specification project, not implementation code