# License Client - macOS Java Application

Native macOS client application for subscription licensing with certificate authentication.

## Requirements

- **JDK 25** (required for Virtual Threads and modern Java features)
- macOS 10.15 or later
- Maven 3.8+

## Build

```bash
# Clean and build
mvn clean package

# Run directly
mvn exec:java

# Or run the JAR
java -jar target/license-client-1.0.0.jar
```

## Project Structure

```
client/macos-java/
├── src/main/
│   ├── java/com/licenseserver/client/
│   │   ├── Main.java                  # Application entry point
│   │   ├── AppConfig.java             # Configuration management
│   │   ├── CertificateManager.java    # macOS Keychain integration
│   │   ├── DeviceIdentifier.java      # Hardware ID generation
│   │   ├── JWTValidator.java          # License token validation
│   │   ├── LicenseApiClient.java      # mTLS HTTP client
│   │   ├── LicenseManager.java        # License lifecycle management
│   │   ├── LicenseStorage.java        # Encrypted license storage
│   │   ├── EnrollmentManager.java     # Enrollment workflow
│   │   └── ui/
│   │       ├── MainWindow.java        # Main application window
│   │       ├── EnrollmentDialog.java  # Device enrollment UI
│   │       └── MigrationDialog.java   # License migration UI
│   └── resources/
│       ├── ca-chain.pem               # CA certificate chain (embedded)
│       ├── license-server.pub         # License signing public key
│       └── config.properties          # Configuration
├── pom.xml                            # Maven configuration
└── README.md
```

## Features

### Certificate Management
- Integrates with macOS Keychain for secure certificate storage
- Uses native `security` command for certificate operations
- Automatic certificate validation and renewal

### Device Identification
- Hardware-based device ID using IOPlatformUUID
- Stable across OS reinstalls (tied to hardware)
- Privacy-preserving hashing

### License Management
- JWT-based license tokens
- Offline validation with embedded public key
- Automatic background renewal
- Grace period support

### User Interface
- Native macOS look and feel
- Device enrollment dialog
- License migration wizard
- Status display with automatic updates

## Development

### Install Dependencies
```bash
mvn clean install
```

### Run Tests
```bash
mvn test
```

### Build Distribution JAR
```bash
mvn clean package
```

### Code Style
This project uses modern Java 25 features:
- Virtual Threads for concurrent operations
- Structured Concurrency for resource management
- Pattern Matching for cleaner code
- Records for data classes

## macOS Integration

### Keychain Access
The application uses the macOS `security` command to:
- Store client certificates
- Retrieve certificates for mTLS
- Remove certificates on revocation

### System Commands Used
- `security` - Certificate and keychain operations
- `ioreg` - Hardware UUID retrieval
- `scutil` - Device name retrieval
- `system_profiler` - Platform information

### Permissions
The application requires:
- Keychain access for certificate storage
- Network access for license server communication

## Security

- Certificates stored in macOS Keychain (hardware-backed on T2/Apple Silicon Macs)
- Private keys never leave the Keychain
- License tokens encrypted at rest
- Device ID hashed for privacy
- mTLS for all license operations

## Distribution

### Code Signing (macOS)
```bash
# Sign the JAR
codesign --sign "Developer ID Application: Your Name" \
  --force --timestamp \
  target/license-client-1.0.0.jar

# Verify signature
codesign --verify --verbose target/license-client-1.0.0.jar
```

### Notarization (macOS)
```bash
# Create DMG
# Package and notarize with Apple
# See build.sh and notarize.sh for automation
```

## Troubleshooting

### Keychain Access Denied
Grant keychain access in System Preferences → Security & Privacy → Privacy → Keychain

### Certificate Not Found
Ensure certificate was successfully enrolled and check Keychain Access.app

### License Check Fails
Verify network connectivity and license server URL in config.properties

### Device ID Error
Requires macOS-specific commands - will not work in containers or VMs without proper passthrough

## Configuration

Edit `src/main/resources/config.properties`:

```properties
license.server.url=https://your-server.com:8443
license.server.mtls.url=https://your-server.com:9443
license.check.interval.hours=24
```