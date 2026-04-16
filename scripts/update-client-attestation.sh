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

# Calcula o Hash SHA-256 e converte para maiúsculas (para ficar igual ao PowerShell e ao Java)
jar_hash=$(sha256sum "$jar_path" | awk '{print toupper($1)}')
printf 'SpertaClient.jar:%s\n' "$jar_hash" > "$attestation_file"