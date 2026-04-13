#!/usr/bin/env bash
set -euo pipefail

if [ "$#" -ne 4 ]; then
    echo "Uso: ./scripts/run-server.sh <port> <password-cifra> <keystore> <password-keystore>" >&2
    exit 1
fi

java -cp "SpertaServer/bin" SpertaServer "$@"