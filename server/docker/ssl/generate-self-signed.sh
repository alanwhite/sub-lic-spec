#!/bin/bash
set -e

# Generate self-signed SSL certificate for development
# For production, use certificates from a public CA (Let's Encrypt, etc.)

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "Generating self-signed SSL certificate for development..."

# Use Docker with OpenSSL to generate certificates (no host OpenSSL needed)
docker run --rm -v "$SCRIPT_DIR":/ssl -w /ssl alpine/openssl:latest \
  req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout server.key \
  -out server.crt \
  -subj "/C=US/ST=California/L=San Francisco/O=License Server/OU=Development/CN=localhost"

echo ""
echo "✓ Self-signed certificate generated!"
echo "  Certificate: $SCRIPT_DIR/server.crt"
echo "  Private key: $SCRIPT_DIR/server.key"
echo ""
echo "⚠️  WARNING: This is a self-signed certificate for DEVELOPMENT ONLY"
echo "   For production, use certificates from a trusted CA"
echo ""