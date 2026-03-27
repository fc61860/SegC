#!/usr/bin/env bash
set -euo pipefail

jar_path="SpertaClient/bin/SpertaClient.jar"
attestation_file="SpertaServer/data/client_size.txt"

if [ ! -f "$jar_path" ]; then
	echo "O ficheiro SpertaClient/bin/SpertaClient.jar nao existe. Compile primeiro o cliente." >&2
	exit 1
fi

mkdir -p "SpertaServer/data"

if [ ! -f "$attestation_file" ]; then
	touch "$attestation_file"
fi

jar_size=$(wc -c < "$jar_path" | tr -d '[:space:]')
printf 'SpertaClient.jar:%s\n' "$jar_size" > "$attestation_file"