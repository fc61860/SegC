#!/usr/bin/env bash
set -euo pipefail

output_dir="SpertaServer/bin"
mkdir -p "$output_dir"

mapfile -t sources < <(find "SpertaServer/src/server" -maxdepth 1 -name '*.java' | sort)

if [ "${#sources[@]}" -eq 0 ]; then
    echo "Nao foram encontrados ficheiros Java do servidor." >&2
    exit 1
fi

javac -d "$output_dir" "${sources[@]}"