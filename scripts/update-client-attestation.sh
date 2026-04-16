#!/usr/bin/env bash
set -euo pipefail

jar_path="SpertaClient/bin/SpertaClient.jar"
ref_jar_path="SpertaServer/data/SpertaClient.jar"
attestation_file="SpertaServer/data/client_attestation.txt"

if [ ! -f "$jar_path" ]; then
	echo "O ficheiro SpertaClient/bin/SpertaClient.jar nao existe. Compile primeiro o cliente." >&2
	exit 1
fi

mkdir -p "SpertaServer/data"

# Copiar o JAR para a pasta do servidor como copia de referencia
cp "$jar_path" "$ref_jar_path"

# Guardar o caminho da copia de referencia no ficheiro de attestation
printf '%s\n' "$ref_jar_path" > "$attestation_file"