#!/usr/bin/env bash
set -euo pipefail

mapfile -t sources < <(find "SpertaServer/src/server" -maxdepth 1 -name '*.java' | sort)

if [ "${#sources[@]}" -eq 0 ]; then
    echo "Nao foram encontrados ficheiros Java do servidor." >&2
    exit 1
fi

javac "${sources[@]}"