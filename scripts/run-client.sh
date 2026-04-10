#!/usr/bin/env bash
set -euo pipefail

if [ "$#" -ne 7 ]; then
    echo "Uso: ./scripts/run-client.sh <serverAddress> <truststore> <password-truststore> <keystore> <password-keystore> <user-id> <password>" >&2
    exit 1
fi

java -jar "SpertaClient/bin/SpertaClient.jar" "$@"