#!/usr/bin/env bash
set -euo pipefail

if [ "$#" -lt 4 ]; then
    echo "Uso: ./scripts/run-login-load.sh <IP:Port> <baseUser> <password> <count> [holdSeconds=5] [sameUser=false] [truststore] [truststorePass] [keystore] [keystorePass] [jarPath]" >&2
    exit 1
fi

server_address="$1"
base_user="$2"
password="$3"
count="$4"
hold_seconds="${5:-5}"
same_user="${6:-false}"
truststore="${7:-security/client-truststore.jks}"
truststore_pass="${8:-changeit}"
keystore="${9:-security/client1-keystore.p12}"
keystore_pass="${10:-changeit}"
jar_path="${11:-SpertaClient/bin/SpertaClient.jar}"

java -cp "SpertaClient/bin/classes" SpertaClient.src.client.LoginLoadTester \
    "$server_address" "$base_user" "$password" "$count" "$hold_seconds" "$same_user" \
    "$truststore" "$truststore_pass" "$keystore" "$keystore_pass" "$jar_path"