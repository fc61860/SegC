#!/usr/bin/env bash
set -euo pipefail

output_dir="SpertaClient/bin"
mkdir -p "$output_dir"

mapfile -t sources < <(find "SpertaClient/src/client" -maxdepth 1 -name '*.java' | sort)

if [ "${#sources[@]}" -eq 0 ]; then
	echo "Nao foram encontrados ficheiros Java do cliente." >&2
	exit 1
fi

javac -d "$output_dir" "${sources[@]}"