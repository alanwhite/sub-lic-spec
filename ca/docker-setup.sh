#!/bin/bash
set -e

# Setup CA infrastructure using Docker (no OpenSSL needed on host!)

echo "=========================================="
echo "Setting up CA infrastructure using Docker"
echo "=========================================="
echo ""

# Use OpenSSL container to generate CA files
docker run --rm -v "$(pwd)":/ca -w /ca alpine/openssl:latest /bin/sh -c '
  # Install bash for script execution
  apk add --no-cache bash

  echo "Running CA setup scripts..."

  # Run CA setup scripts
  cd scripts
  bash 01-setup-root-ca.sh
  bash 02-setup-intermediate-ca.sh
  bash 03-generate-license-keys.sh
'

echo ""
echo "=========================================="
echo "CA setup complete!"
echo "=========================================="
echo ""
echo "Root CA certificate: $(pwd)/root-ca/certs/root-ca.crt"
echo "Intermediate CA certificate: $(pwd)/intermediate-ca/certs/intermediate-ca.crt"
echo "Certificate chain: $(pwd)/intermediate-ca/certs/ca-chain.crt"
echo "License signing keys: $(pwd)/../server/docker/license-keys/"
echo ""
echo "IMPORTANT:"
echo "1. Store Root CA private key offline and encrypted"
echo "2. Backup all CA keys to secure location"
echo "3. Never commit CA keys to version control"
echo ""