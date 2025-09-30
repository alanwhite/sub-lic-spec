#!/bin/bash
set -e

CA_DIR="/ca"
ROOT_CA_DIR="$CA_DIR/root-ca"

echo "=========================================="
echo "Setting up Root CA (Offline)"
echo "=========================================="
echo ""

# Create directory structure
echo "Creating directory structure..."
mkdir -p "$ROOT_CA_DIR"/{private,certs,crl,newcerts}
chmod 700 "$ROOT_CA_DIR/private"

# Create database files
touch "$ROOT_CA_DIR/index.txt"
echo "1000" > "$ROOT_CA_DIR/serial"
echo "1000" > "$ROOT_CA_DIR/crlnumber"

echo "Generating Root CA private key (4096-bit, AES-256 encrypted)..."
# Generate root CA private key (4096-bit, AES-256 encrypted)
openssl genrsa -aes256 \
    -passout pass:rootcapassword \
    -out "$ROOT_CA_DIR/private/root-ca.key" 4096

chmod 400 "$ROOT_CA_DIR/private/root-ca.key"

echo "Generating Root CA certificate (20-year validity)..."
# Generate root CA certificate (20 year validity)
openssl req -config "$CA_DIR/config/root-ca.cnf" \
    -key "$ROOT_CA_DIR/private/root-ca.key" \
    -passin pass:rootcapassword \
    -new -x509 -days 7300 -sha256 -extensions v3_ca \
    -out "$ROOT_CA_DIR/certs/root-ca.crt" \
    -subj "/C=US/ST=California/L=San Francisco/O=License Server/OU=Certificate Authority/CN=License Server Root CA"

chmod 444 "$ROOT_CA_DIR/certs/root-ca.crt"

echo ""
echo "Root CA certificate details:"
openssl x509 -noout -text -in "$ROOT_CA_DIR/certs/root-ca.crt" | grep -A 2 "Subject:"
openssl x509 -noout -text -in "$ROOT_CA_DIR/certs/root-ca.crt" | grep -A 2 "Validity"

echo ""
echo "✓ Root CA setup complete!"
echo "  Certificate: $ROOT_CA_DIR/certs/root-ca.crt"
echo "  Private key: $ROOT_CA_DIR/private/root-ca.key"
echo ""
echo "⚠️  IMPORTANT: Store the Root CA private key offline and encrypted!"
echo ""