#!/usr/bin/env bash
set -euo pipefail

# ── Reset data ────────────────────────────────────────────────────────────────
echo "=== Resetting data... ==="

data_dir="SpertaServer/data"
logs_dir="$data_dir/logs"
client_data_dir="SpertaClient/data"

mkdir -p "$data_dir" "$logs_dir" "$client_data_dir"

rm -f "$data_dir/users.txt" "$data_dir/casas.txt" "$data_dir/estados.txt" "$data_dir/online_users.txt"

find "$data_dir" -maxdepth 1 -name 'key.*'  -type f -delete
find "$data_dir" -maxdepth 1 -name '*.cer'  -type f -delete
find "$data_dir" -maxdepth 1 -name '*.tmp'  -type f -delete
find "$data_dir" -maxdepth 1 -name '*.hash' -type f -delete
find "$logs_dir" -maxdepth 1 -type f -delete
find "$client_data_dir" -maxdepth 1 -type f -delete

# ── Compile server ────────────────────────────────────────────────────────────
echo "=== Compiling server... ==="

server_classes_dir="SpertaServer/bin/classes"
server_jar_path="SpertaServer/bin/SpertaServer.jar"
rm -rf "$server_classes_dir"
mkdir -p "$server_classes_dir"
find "SpertaServer/bin" -maxdepth 1 -type f -name '*.class' -delete
find "SpertaServer/src" -type f -name '*.class' -delete

mapfile -t server_sources < <(find "SpertaServer/src/server" -type f -name '*.java' | sort)
if [ "${#server_sources[@]}" -eq 0 ]; then
    echo "Nao foram encontrados ficheiros Java do servidor." >&2; exit 1
fi
javac -d "$server_classes_dir" "${server_sources[@]}"

rm -f "$server_jar_path"
(cd "$server_classes_dir" && jar --create --file "../SpertaServer.jar" --main-class SpertaServer .)

# ── Compile client ────────────────────────────────────────────────────────────
echo "=== Compiling client... ==="

classes_dir="SpertaClient/bin/classes"
jar_path="SpertaClient/bin/SpertaClient.jar"
rm -rf "$classes_dir"
mkdir -p "$classes_dir"
find "SpertaClient/bin" -maxdepth 1 -type f -name '*.class' -delete
find "SpertaClient/src" -type f -name '*.class' -delete

mapfile -t client_sources < <(find "SpertaClient/src/client" -type f -name '*.java' | sort)
if [ "${#client_sources[@]}" -eq 0 ]; then
    echo "Nao foram encontrados ficheiros Java do cliente." >&2; exit 1
fi
javac -d "$classes_dir" "${client_sources[@]}"
rm -f "$jar_path"
( cd "$classes_dir" && jar --create --file "../SpertaClient.jar" --main-class SpertaClient.src.client.SpertaClient . )

# ── Update client attestation ─────────────────────────────────────────────────
echo "=== Updating client attestation... ==="

ref_jar_path="$data_dir/SpertaClient.jar"
attestation_file="$data_dir/client_attestation.txt"

if [ ! -f "$jar_path" ]; then
    echo "O ficheiro $jar_path nao existe." >&2; exit 1
fi
cp "$jar_path" "$ref_jar_path"
printf '%s\n' "$ref_jar_path" > "$attestation_file"
rm -f "${attestation_file}.hash"

echo "=== All tasks completed successfully! ==="
