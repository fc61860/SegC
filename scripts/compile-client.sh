#!/usr/bin/env bash
set -euo pipefail

classes_dir="SpertaClient/bin/classes"
jar_path="SpertaClient/bin/SpertaClient.jar"
mkdir -p "$classes_dir"

mapfile -t sources < <(find "SpertaClient/src/client" -maxdepth 1 -name '*.java' | sort)

if [ "${#sources[@]}" -eq 0 ]; then
	echo "Nao foram encontrados ficheiros Java do cliente." >&2
	exit 1
fi

javac -d "$classes_dir" "${sources[@]}"
rm -f "$jar_path"

(
	cd "$classes_dir"
	jar --create --file "../SpertaClient.jar" --main-class SpertaClient.src.client.SpertaClient .
)