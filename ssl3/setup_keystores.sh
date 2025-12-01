#!/usr/bin/env bash

# Creates a self-signed server keystore (JKS) named server.keystore
# The Keystore password and key password are both set to 'password' for the example.

KEYSTORE=server.keystore
PASSWORD=password
ALIAS=server
DNAME="CN=localhost, OU=Dev, O=UBI, L=City, S=State, C=PT"

if [ -f "$KEYSTORE" ]; then
  echo "$KEYSTORE existe. Nao Vale a Pena."
  exit 0
fi

echo "Generating server keystore ($KEYSTORE) with alias '$ALIAS'..."

keytool -genkeypair \
  -alias "$ALIAS" \
  -keyalg RSA \
  -keysize 2048 \
  -keystore "$KEYSTORE" \
  -storetype JKS \
  -storepass "$PASSWORD" \
  -keypass "$PASSWORD" \
  -dname "$DNAME" \
  -validity 365

echo "Created $KEYSTORE (password: $PASSWORD, alias: $ALIAS)."
