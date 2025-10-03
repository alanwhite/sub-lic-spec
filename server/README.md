# License Server - PHP Implementation

This is the reference implementation of the subscription licensing server with certificate authentication.

## Quick Start

### 1. Setup Environment

```bash
# Copy environment template
cp .env.example .env

# Edit .env with your configuration
nano .env
```

### 2. Start Containers

```bash
# Start all services
./docker-helper.sh start

# View logs
./docker-helper.sh logs
```

### 3. Run Migrations

```bash
# Run database migrations
./docker-helper.sh migrate
```

### 4. Verify Setup

- TLS Endpoint: https://localhost:8443
- mTLS Endpoint: https://localhost:9443
- PHPMyAdmin: http://localhost:8081

## Project Structure

```
server/
├── public/              # Web root
│   ├── index.php       # Application entry point
│   ├── crl/            # Certificate Revocation Lists
│   └── portal/         # Portal UI pages
├── src/
│   ├── Api/            # API Controllers
│   ├── Services/       # Business Logic
│   ├── Database/       # Database connection
│   ├── Models/         # Data models
│   └── Middleware/     # Authentication middleware
├── config/             # Configuration files
├── database/
│   ├── migrations/     # Database migrations
│   └── seeds/          # Test data
├── docker/
│   ├── apache/         # Apache configs (TLS + mTLS)
│   ├── ssl/            # SSL certificates
│   └── license-keys/   # License signing keys
├── logs/               # Application logs
├── tests/              # Unit and integration tests
├── docker-compose.yml
├── composer.json
└── migrate.php
```

## Development

### Install Dependencies

```bash
./docker-helper.sh shell
composer install
```

### Run Tests

```bash
./docker-helper.sh shell
composer run test
```

### Lint Code

```bash
./docker-helper.sh shell
composer run lint
```

## API Endpoints

### Portal (Session Auth)
- `POST /portal/v1/login` - User login
- `POST /portal/v1/enrollment/generate` - Generate enrollment token
- `GET /portal/v1/devices` - List enrolled devices
- `DELETE /portal/v1/devices/:fingerprint` - Revoke device

### Certificate (TLS + Token)
- `POST /api/v1/certificate/enroll` - Enroll with CSR + token

### License (mTLS Required)
- `POST /api/v1/license/activate` - Activate license
- `POST /api/v1/license/renew` - Renew license
- `GET /api/v1/license/status` - Check license status

### Migration (mTLS Required)
- `POST /api/v1/migration/initiate` - Start device migration
- `POST /api/v1/migration/complete` - Complete migration

## Configuration

Key configuration options in `.env`:

- **Database**: Connection settings for MySQL
- **CA Paths**: Paths to CA certificates and keys
- **License Keys**: Separate signing keys for JWT tokens
- **TLS Ports**: 8443 (TLS), 9443 (mTLS)
- **Grace Periods**: 5 days (monthly), 14 days (annual)
- **Device Limits**: Default 1, configurable per subscription

## Security

- Root CA offline after initial setup
- Intermediate CA for daily operations
- Separate license signing keys (not CA keys)
- mTLS required for all license operations
- Device binding with hardware IDs
- Certificate revocation via CRL

## Deployment

See `../docs/deployment-production.md` for production deployment guide.

## Troubleshooting

### Container won't start
```bash
./docker-helper.sh logs-web
./docker-helper.sh logs-db
```

### Database connection failed
```bash
# Check database is running
./docker-helper.sh status

# Check credentials in .env match docker-compose.yml
```

### Permission errors
```bash
./docker-helper.sh shell
chown -R www-data:www-data /var/www/html
```

## Helper Commands

```bash
./docker-helper.sh start      # Start all containers
./docker-helper.sh stop       # Stop all containers
./docker-helper.sh restart    # Restart containers
./docker-helper.sh logs       # View all logs
./docker-helper.sh shell      # Open shell in web container
./docker-helper.sh shell-db   # Open MySQL shell
./docker-helper.sh migrate    # Run database migrations
./docker-helper.sh clean      # Remove all containers and volumes
./docker-helper.sh rebuild    # Rebuild from scratch
./docker-helper.sh status     # Show container status
```