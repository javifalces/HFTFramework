#!/usr/bin/env bash
#
# Converts a pasted encrypted PKCS8 Ed25519 private key (PEM) into the single-line base64 DER
# format required for binance.secretkey in application.properties.
#
# Paste the PEM text (including the -----BEGIN/END ENCRYPTED PRIVATE KEY----- lines) when
# prompted. Requires openssl to decrypt/convert the key; you will be prompted for the PEM
# passphrase by openssl itself (not stored by this script).
#
# NOTE: binance.apikey is NOT derived from this key - it is the Ed25519 API Key id Binance
# gives you after you upload the matching PUBLIC key in Binance API Management.

set -euo pipefail

if ! command -v openssl >/dev/null 2>&1; then
    echo "ERROR: openssl not found on PATH. Install it (e.g. apt install openssl / brew install openssl)." >&2
    exit 1
fi

echo "==================================================="
echo " Binance Ed25519 secretKey converter"
echo "==================================================="
echo "Paste your encrypted private key PEM below (including the"
echo "-----BEGIN ENCRYPTED PRIVATE KEY----- and -----END ENCRYPTED PRIVATE KEY-----"
echo "lines). Input stops automatically after the END line."
echo ""

tmp_dir="$(mktemp -d)"
pem_file="$tmp_dir/key.pem"
der_file="$tmp_dir/key.der"
cleanup() { rm -rf "$tmp_dir"; }
trap cleanup EXIT

: > "$pem_file"
while IFS= read -r line; do
    printf '%s\n' "$line" >> "$pem_file"
    if [[ "$line" == *"-----END ENCRYPTED PRIVATE KEY-----"* ]]; then
        break
    fi
done

echo ""
echo "You will now be prompted for the PEM passphrase by openssl..."
if ! openssl pkey -in "$pem_file" -out "$der_file" -outform DER; then
    echo "ERROR: openssl failed to decrypt/convert the private key. Check the passphrase and try again." >&2
    exit 1
fi

secret_key="$(base64 -w 0 "$der_file")"

echo ""
echo "==================================================="
echo "binance.secretkey=$secret_key"
echo "==================================================="
echo ""
echo "Paste the line above into application.properties."
echo "Set binance.apikey to the Ed25519 API Key id Binance gave you"
echo "when you uploaded the matching PUBLIC key in API Management."
echo ""
read -r -p "Press Enter to exit"
