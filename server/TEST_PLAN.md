# License System Test Plan

## Overview
This document outlines comprehensive testing for the subscription licensing system with certificate authentication.

## Test Categories

### 1. Certificate Enrollment (Phase 1 - TLS)

#### 1.1 Valid Enrollment
**Test**: Enroll with valid token
- Generate enrollment token via admin panel
- Client submits CSR with token
- **Expected**: Client receives certificate + CA chain
- **Verify**: Certificate stored in `issued_certificates` table
- **Verify**: Client record created in `clients` table with `device_id`

#### 1.2 Invalid Token
**Test**: Enroll with invalid/expired token
- Use non-existent or already-used token
- **Expected**: HTTP 401 with error "Invalid or used enrollment token"

#### 1.3 Expired Token
**Test**: Enroll with expired token
- Create token, modify `expires_at` to past date
- **Expected**: HTTP 401 with error "Enrollment token expired"

#### 1.4 Device Limit - New Device
**Test**: Exceed device limit on subscription
- Create subscription with device_limit = 2
- Enroll 2 different devices (different device_id)
- Attempt to enroll 3rd device
- **Expected**: HTTP 403 "Device limit reached for this subscription"

#### 1.5 Device Limit - Same Device Re-enrollment
**Test**: Re-enroll same device (certificate renewal)
- Enroll device once (device_id: ABC)
- Enroll same device again (same device_id: ABC)
- **Expected**: Both enrollments succeed (certificate renewal)
- **Verify**: Only 1 unique device_id counted toward limit

#### 1.6 Missing Required Fields
**Test**: Submit enrollment without required fields
- Try without: token, csr, device_name, platform, device_id
- **Expected**: HTTP 400 "Missing required fields"

---

### 2. JWT License Acquisition (Phase 2 - mTLS)

#### 2.1 Valid mTLS Connection
**Test**: Connect with valid client certificate
- Use certificate from enrollment
- Request license token via mTLS
- **Expected**: Receive JWT token with claims (sub, cert_fp, device_id, tier, exp)

#### 2.2 Invalid Certificate
**Test**: Connect with invalid/self-signed certificate
- **Expected**: TLS handshake failure or HTTP 401

#### 2.3 Revoked Certificate
**Test**: Connect with revoked certificate
- Revoke certificate via admin panel
- Attempt mTLS connection
- **Expected**: Connection rejected (CRL/OCSP check)

#### 2.4 JWT Token Contents
**Test**: Validate JWT structure
- Decode JWT token
- **Verify Claims**:
  - `sub`: subscription_id
  - `cert_fp`: SHA-256 certificate fingerprint
  - `device_id`: Hardware device identifier
  - `tier`: Subscription tier (premium/free)
  - `exp`: Expiration timestamp (30 days)
  - `iat`: Issued at timestamp

#### 2.5 Certificate-JWT Binding
**Test**: JWT bound to certificate fingerprint
- Extract `cert_fp` from JWT
- Calculate fingerprint from client certificate
- **Expected**: Fingerprints match exactly

---

### 3. License Validation (Client-Side)

#### 3.1 Valid License Check
**Test**: Check valid, non-expired license
- Store JWT from Phase 2
- Validate signature with server public key
- Check expiration
- **Expected**: License valid, access granted

#### 3.2 Expired License
**Test**: Check expired license
- Modify JWT `exp` to past timestamp (or wait)
- **Expected**: License invalid, prompt renewal

#### 3.3 Tampered License
**Test**: Modify JWT claims
- Change `tier` from "free" to "premium"
- **Expected**: Signature validation fails

#### 3.4 Certificate-License Mismatch
**Test**: Use JWT with different certificate
- Get JWT on device A (cert fingerprint: AAA)
- Try to use on device B (cert fingerprint: BBB)
- **Expected**: Fingerprint mismatch, license invalid

---

### 4. License Renewal (Background)

#### 4.1 Automatic Renewal - Valid Subscription
**Test**: Renew before expiration
- 7 days before expiry, client connects via mTLS
- Server checks subscription status = "active"
- **Expected**: New JWT issued with extended expiration

#### 4.2 Renewal - Expired Subscription
**Test**: Renew with unpaid subscription
- Subscription `payment_status` = "failed"
- **Expected**: Renewal rejected, grace period check

#### 4.3 Grace Period Handling
**Test**: Renew during grace period
- Monthly: 5 days after expiry
- Annual: 14 days after expiry
- **Expected**: Renewal succeeds with warning, limited access

#### 4.4 Grace Period Expired
**Test**: Renew after grace period
- **Expected**: Renewal rejected, subscription required

---

### 5. Device Limit Enforcement

#### 5.1 Count Unique Devices
**Test**: Multiple enrollments, same device
- Subscription limit: 3 devices
- Enroll device A 5 times (certificate renewals)
- **Expected**: Only counts as 1 device

#### 5.2 Enforce Limit - Different Devices
**Test**: Exceed limit with different devices
- Subscription limit: 3 devices
- Enroll devices A, B, C (success)
- Attempt device D
- **Expected**: HTTP 403 "Device limit reached"

#### 5.3 Device Revocation Frees Slot
**Test**: Revoke device, enroll new one
- At device limit (3/3)
- Revoke device A via portal
- Enroll device D
- **Expected**: Enrollment succeeds (2/3 after revocation)

---

### 6. Device Migration

#### 6.1 Initiate Migration
**Test**: Request migration token
- Old device connects via mTLS
- Request migration token
- **Expected**: Receive 24-hour single-use token

#### 6.2 Complete Migration - Valid
**Test**: Transfer license to new device
- New device has certificate (previously enrolled or new enrollment)
- Submit migration token + new device_id via mTLS
- **Expected**: New JWT issued for new device, old license deactivated

#### 6.3 Migration Token Expiry
**Test**: Use expired migration token
- Generate token, wait 25 hours
- **Expected**: HTTP 401 "Migration token expired"

#### 6.4 Migration Token Reuse
**Test**: Use migration token twice
- Complete migration once
- Attempt again with same token
- **Expected**: HTTP 401 "Migration token already used"

#### 6.5 Migration Without Device Slot
**Test**: Migrate to new device at limit
- Subscription limit: 3 devices (all used)
- Migrate from device A to device D (new device)
- **Expected**: Must revoke another device first

---

### 7. Certificate Management

#### 7.1 Certificate Expiration
**Test**: Check expired certificate
- Modify `expires_at` to past date
- Attempt mTLS connection
- **Expected**: Connection rejected

#### 7.2 Certificate Revocation via Portal
**Test**: User revokes device
- User logs into portal
- Revokes device certificate
- **Expected**: Certificate added to CRL, mTLS fails

#### 7.3 CRL Update Cycle
**Test**: Revocation propagation
- Revoke certificate
- Check CRL updated (24-hour cycle)
- **Expected**: Revoked cert appears in CRL

#### 7.4 Certificate Serial Number Uniqueness
**Test**: Multiple enrollments
- Enroll 100 devices
- **Expected**: All serial numbers unique

---

### 8. Subscription Management

#### 8.1 Subscription Expiry
**Test**: Subscription ends
- Set `end_date` to yesterday
- Attempt license renewal
- **Expected**: Renewal fails, grace period check

#### 8.2 Payment Failure
**Test**: Payment status = failed
- Update subscription `payment_status` = "failed"
- Attempt renewal
- **Expected**: Grace period notification

#### 8.3 Subscription Upgrade
**Test**: Change device limit
- Subscription has 3 devices enrolled
- Upgrade `device_limit` from 3 to 10
- Enroll 4th device
- **Expected**: Enrollment succeeds

#### 8.4 Subscription Downgrade
**Test**: Reduce device limit
- Subscription has 5 devices enrolled
- Downgrade `device_limit` from 10 to 3
- Attempt new enrollment
- **Expected**: Enrollment fails (5 > 3)
- **Action Required**: User must revoke 2 devices

---

### 9. Security Tests

#### 9.1 TLS Version Enforcement
**Test**: Connect with TLS 1.2
- **Expected**: Connection rejected (TLS 1.3 only)

#### 9.2 Hostname Verification
**Test**: Certificate hostname mismatch (production)
- **Expected**: Connection rejected in production mode

#### 9.3 JWT Signature Verification
**Test**: Sign JWT with wrong key
- **Expected**: Signature validation fails

#### 9.4 OCSP/CRL Soft-Fail
**Test**: OCSP/CRL server unreachable
- **Expected**: Connection succeeds (soft-fail for availability)

#### 9.5 Token Extraction Attack
**Test**: Copy JWT from device A to device B
- **Expected**: Certificate fingerprint mismatch, validation fails

---

### 10. Edge Cases

#### 10.1 Clock Skew
**Test**: Client clock 10 minutes ahead/behind
- **Expected**: JWT validation accounts for reasonable skew

#### 10.2 Concurrent Enrollments
**Test**: 2 devices enroll simultaneously at device limit
- Device A and B both try to be the 3rd device
- **Expected**: One succeeds, one fails (race condition handling)

#### 10.3 Database Rollback
**Test**: Certificate issued but DB insert fails
- **Expected**: Proper error handling, no orphaned certificates

#### 10.4 Network Interruption
**Test**: Connection drops during enrollment
- **Expected**: Token remains valid, retry succeeds

#### 10.5 Empty Device Name
**Test**: Submit whitespace-only device name
- **Expected**: Validation fails

---

## Test Execution Priority

### P0 (Critical - Must Pass)
1. Valid enrollment flow (1.1)
2. Valid mTLS + JWT acquisition (2.1)
3. Valid license check (3.1)
4. Device limit enforcement (5.2)
5. Certificate-JWT binding (2.5)

### P1 (High - Should Pass)
6. License renewal (4.1)
7. Device limit with same device (1.5)
8. Expired token handling (1.3)
9. Certificate revocation (7.2)
10. Grace period handling (4.3)

### P2 (Medium - Nice to Have)
11. Migration flow (6.2)
12. Subscription expiry (8.1)
13. Security tests (9.1-9.5)
14. Edge cases (10.1-10.5)

---

## Automated Test Locations

- **Server Unit Tests**: `server/tests/`
- **Integration Tests**: `server/tests/Integration/`
- **Client Tests**: `client/macos-java/src/test/`
- **End-to-End Tests**: `tests/e2e/`

---

## Success Criteria

- All P0 tests pass
- 90%+ P1 tests pass
- No security vulnerabilities detected
- Device limit enforcement working correctly
- License renewal seamless for users
- Certificate-JWT binding prevents token theft
