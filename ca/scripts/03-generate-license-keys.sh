#!/bin/bash
set -e

LICENSE_KEY_DIR="/ca/../server/docker/license-keys"

echo "=========================================="
echo "Generating License Signing Keys"
echo "=========================================="
echo ""

mkdir -p "$LICENSE_KEY_DIR"
chmod 700 "$LICENSE_KEY_DIR"

echo "Generating license signing private key (2048-bit for JWT, AES-256 encrypted)..."
# Generate license signing private key (2048-bit for JWT)
openssl genrsa -aes256 \
    -passout pass:licensekeypassword \
    -out "$LICENSE_KEY_DIR/license-signing.key" 2048

chmod 400 "$LICENSE_KEY_DIR/license-signing.key"

echo "Extracting public key..."
# Extract public key
openssl rsa -in "$LICENSE_KEY_DIR/license-signing.key" \
    -passin pass:licensekeypassword \
    -pubout -out "$LICENSE_KEY_DIR/license-signing.pub"

chmod 444 "$LICENSE_KEY_DIR/license-signing.pub"

echo ""
echo "✓ License signing keys generated!"
echo "  Private key: $LICENSE_KEY_DIR/license-signing.key"
echo "  Public key: $LICENSE_KEY_DIR/license-signing.pub"
echo ""
echo "⚠️  IMPORTANT:"
echo "  1. These keys are SEPARATE from CA keys"
echo "  2. Used only for signing JWT license tokens"
echo "  3. Embed the public key in client applications"
echo "  4. Rotate annually for security"
echo ""