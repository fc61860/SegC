#!/usr/bin/env bash
set -euo pipefail

if [ "$#" -ne 3 ]; then
    echo "Uso: ./scripts/run-client.sh <IP:Port> <userName> <password>" >&2
    exit 1
fi

java -jar "SpertaClient/bin/SpertaClient.jar" "$1" "$2" "$3"