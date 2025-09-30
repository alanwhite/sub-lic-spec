#!/bin/bash
set -e

CA_DIR="/ca"
ROOT_CA_DIR="$CA_DIR/root-ca"
INT_CA_DIR="$CA_DIR/intermediate-ca"

echo "=========================================="
echo "Setting up Intermediate CA (Online)"
echo "=========================================="
echo ""

# Create directory structure
echo "Creating directory structure..."
mkdir -p "$INT_CA_DIR"/{private,certs,crl,newcerts,csr}
chmod 700 "$INT_CA_DIR/private"

# Create database files
touch "$INT_CA_DIR/index.txt"
echo "1000" > "$INT_CA_DIR/serial"
echo "1000" > "$INT_CA_DIR/crlnumber"

echo "Generating Intermediate CA private key (4096-bit, AES-256 encrypted)..."
# Generate intermediate CA private key
openssl genrsa -aes256 \
    -passout pass:intermediatecapassword \
    -out "$INT_CA_DIR/private/intermediate-ca.key" 4096

chmod 400 "$INT_CA_DIR/private/intermediate-ca.key"

echo "Creating Intermediate CA certificate signing request..."
# Generate intermediate CA CSR
openssl req -config "$CA_DIR/config/intermediate-ca.cnf" \
    -new -sha256 \
    -key "$INT_CA_DIR/private/intermediate-ca.key" \
    -passin pass:intermediatecapassword \
    -out "$INT_CA_DIR/csr/intermediate-ca.csr" \
    -subj "/C=US/ST=California/L=San Francisco/O=License Server/OU=Certificate Authority/CN=License Server Intermediate CA"

echo "Signing Intermediate CA certificate with Root CA (10-year validity)..."
# Sign intermediate certificate with root CA
openssl ca -config "$CA_DIR/config/root-ca.cnf" \
    -extensions v3_intermediate_ca \
    -days 3650 -notext -md sha256 \
    -passin pass:rootcapassword \
    -batch \
    -in "$INT_CA_DIR/csr/intermediate-ca.csr" \
    -out "$INT_CA_DIR/certs/intermediate-ca.crt"

chmod 444 "$INT_CA_DIR/certs/intermediate-ca.crt"

echo "Creating certificate chain file..."
# Create certificate chain file
cat "$INT_CA_DIR/certs/intermediate-ca.crt" \
    "$ROOT_CA_DIR/certs/root-ca.crt" > "$INT_CA_DIR/certs/ca-chain.crt"

chmod 444 "$INT_CA_DIR/certs/ca-chain.crt"

echo ""
echo "Verifying certificate chain..."
# Verify certificate chain
openssl verify -CAfile "$ROOT_CA_DIR/certs/root-ca.crt" \
    "$INT_CA_DIR/certs/intermediate-ca.crt"

echo ""
echo "Intermediate CA certificate details:"
openssl x509 -noout -text -in "$INT_CA_DIR/certs/intermediate-ca.crt" | grep -A 2 "Subject:"
openssl x509 -noout -text -in "$INT_CA_DIR/certs/intermediate-ca.crt" | grep -A 2 "Validity"

echo ""
echo "✓ Intermediate CA setup complete!"
echo "  Certificate: $INT_CA_DIR/certs/intermediate-ca.crt"
echo "  Chain: $INT_CA_DIR/certs/ca-chain.crt"
echo "  Private key: $INT_CA_DIR/private/intermediate-ca.key"
echo ""