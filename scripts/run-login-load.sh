#!/usr/bin/env bash
set -euo pipefail

if [ "$#" -lt 4 ] || [ "$#" -gt 6 ]; then
    echo "Uso: ./scripts/run-login-load.sh <IP:Port> <baseUser> <password> <count> [holdSeconds] [sameUser]" >&2
    exit 1
fi

server_address="$1"
base_user="$2"
password="$3"
count="$4"
hold_seconds="${5:-10}"
same_user="${6:-false}"

java -cp "SpertaClient/bin" SpertaClient.src.client.LoginLoadTester \
    "$server_address" "$base_user" "$password" "$count" "$hold_seconds" "$same_user"