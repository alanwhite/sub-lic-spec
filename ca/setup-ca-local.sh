#!/bin/bash
set -e

# Simplified CA setup using Docker for each OpenSSL command
# This works around shell interpretation issues

CA_DIR="/Users/alanwhite/git/sub-lic-spec/ca"
cd "$CA_DIR"

echo "=========================================="
echo "Setting up CA infrastructure"
echo "=========================================="
echo ""

# Create directory structure
echo "Creating directory structure..."
mkdir -p root-ca/{private,certs,crl,newcerts}
mkdir -p intermediate-ca/{private,certs,crl,newcerts,csr}
mkdir -p ../server/docker/license-keys
chmod 700 root-ca/private intermediate-ca/private

# Create database files
touch root-ca/index.txt intermediate-ca/index.txt
echo "1000" > root-ca/serial
echo "1000" > root-ca/crlnumber
echo "1000" > intermediate-ca/serial
echo "1000" > intermediate-ca/crlnumber

echo ""
echo "Step 1: Generating Root CA private key..."
/usr/local/bin/docker run --rm -v "$CA_DIR":/work -w /work alpine/openssl:latest \
  genrsa -aes256 -passout pass:rootcapassword -out /work/root-ca/private/root-ca.key 4096

chmod 400 root-ca/private/root-ca.key

echo "Step 2: Generating Root CA certificate..."
/usr/local/bin/docker run --rm -v "$CA_DIR":/work -w /work alpine/openssl:latest \
  req -config /work/config/root-ca.cnf \
  -key /work/root-ca/private/root-ca.key \
  -passin pass:rootcapassword \
  -new -x509 -days 7300 -sha256 -extensions v3_ca \
  -out /work/root-ca/certs/root-ca.crt \
  -subj "/C=US/ST=California/L=San Francisco/O=License Server/OU=Certificate Authority/CN=License Server Root CA"

chmod 444 root-ca/certs/root-ca.crt

echo "Step 3: Generating Intermediate CA private key..."
/usr/local/bin/docker run --rm -v "$CA_DIR":/work -w /work alpine/openssl:latest \
  genrsa -aes256 -passout pass:intermediatecapassword -out /work/intermediate-ca/private/intermediate-ca.key 4096

chmod 400 intermediate-ca/private/intermediate-ca.key

echo "Step 4: Creating Intermediate CA CSR..."
/usr/local/bin/docker run --rm -v "$CA_DIR":/work -w /work alpine/openssl:latest \
  req -config /work/config/intermediate-ca.cnf \
  -new -sha256 \
  -key /work/intermediate-ca/private/intermediate-ca.key \
  -passin pass:intermediatecapassword \
  -out /work/intermediate-ca/csr/intermediate-ca.csr \
  -subj "/C=US/ST=California/L=San Francisco/O=License Server/OU=Certificate Authority/CN=License Server Intermediate CA"

echo "Step 5: Signing Intermediate CA certificate with Root CA..."
/usr/local/bin/docker run --rm -v "$CA_DIR":/work -w /work alpine/openssl:latest \
  ca -config /work/config/root-ca.cnf \
  -extensions v3_intermediate_ca \
  -days 3650 -notext -md sha256 \
  -passin pass:rootcapassword \
  -batch \
  -in /work/intermediate-ca/csr/intermediate-ca.csr \
  -out /work/intermediate-ca/certs/intermediate-ca.crt

chmod 444 intermediate-ca/certs/intermediate-ca.crt

echo "Step 6: Creating certificate chain..."
cat intermediate-ca/certs/intermediate-ca.crt root-ca/certs/root-ca.crt > intermediate-ca/certs/ca-chain.crt

echo "Step 7: Generating license signing keys..."
/usr/local/bin/docker run --rm -v "$CA_DIR/../server/docker/license-keys":/keys -w /keys alpine/openssl:latest \
  genrsa -aes256 -passout pass:licensekeypassword -out /keys/license-signing.key 2048

chmod 400 ../server/docker/license-keys/license-signing.key

/usr/local/bin/docker run --rm -v "$CA_DIR/../server/docker/license-keys":/keys -w /keys alpine/openssl:latest \
  rsa -in /keys/license-signing.key -passin pass:licensekeypassword -pubout -out /keys/license-signing.pub

chmod 444 ../server/docker/license-keys/license-signing.pub

echo ""
echo "=========================================="
echo "✓ CA setup complete!"
echo "=========================================="
echo ""
echo "Root CA certificate: $CA_DIR/root-ca/certs/root-ca.crt"
echo "Intermediate CA certificate: $CA_DIR/intermediate-ca/certs/intermediate-ca.crt"
echo "Certificate chain: $CA_DIR/intermediate-ca/certs/ca-chain.crt"
echo "License signing keys: $CA_DIR/../server/docker/license-keys/"
echo ""
echo "⚠️  IMPORTANT:"
echo "1. Development passphrases are hardcoded (rootcapassword, etc.)"
echo "2. For production, use strong unique passphrases"
echo "3. Store Root CA private key offline"
echo ""