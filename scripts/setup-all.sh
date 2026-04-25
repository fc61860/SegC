#!/usr/bin/env bash
set -euo pipefail

# ── Reset data ────────────────────────────────────────────────────────────────
echo "=== Resetting data... ==="

data_dir="SpertaServer/data"
logs_dir="$data_dir/logs"
client_data_dir="SpertaClient/data"
server_salt_file="SpertaServer/salt.bin"

mkdir -p "$data_dir" "$logs_dir" "$client_data_dir"

rm -f "$data_dir/users.txt" "$data_dir/casas.txt" "$data_dir/estados.txt" "$data_dir/online_users.txt"
rm -f "$server_salt_file"

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

if [ -f "$server_jar_path" ]; then
    echo "=== Server JAR already exists, skipping compilation ==="
else
    mkdir -p "$server_classes_dir"

    mapfile -t server_sources < <(find "SpertaServer/src/server" -type f -name '*.java' | sort)
    if [ "${#server_sources[@]}" -eq 0 ]; then
        echo "Nao foram encontrados ficheiros Java do servidor." >&2; exit 1
    fi
    javac --release 21 -d "$server_classes_dir" "${server_sources[@]}"

    rm -f "$server_jar_path"
    (cd "$server_classes_dir" && jar --create --file "../SpertaServer.jar" --main-class SpertaServer .)
fi

# ── Compile client ────────────────────────────────────────────────────────────
echo "=== Compiling client... ==="

classes_dir="SpertaClient/bin/classes"
jar_path="SpertaClient/bin/SpertaClient.jar"

if [ -f "$jar_path" ]; then
    echo "=== Client JAR already exists, skipping compilation ==="
else
    mkdir -p "$classes_dir"

    mapfile -t client_sources < <(find "SpertaClient/src/client" -type f -name '*.java' | sort)
    if [ "${#client_sources[@]}" -eq 0 ]; then
        echo "Nao foram encontrados ficheiros Java do cliente." >&2; exit 1
    fi
    javac --release 21 -d "$classes_dir" "${client_sources[@]}"
    rm -f "$jar_path"
    (cd "$classes_dir" && jar --create --file "../SpertaClient.jar" --main-class SpertaClient.src.client.SpertaClient .)
fi

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
