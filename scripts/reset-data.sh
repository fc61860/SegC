#!/usr/bin/env bash
set -euo pipefail

data_dir="SpertaServer/data"
logs_dir="$data_dir/logs"
client_data_dir="SpertaClient/data"

mkdir -p "$data_dir" "$logs_dir" "$client_data_dir"

rm -f "$data_dir/users.txt"
rm -f "$data_dir/casas.txt"
rm -f "$data_dir/estados.txt"
rm -f "$data_dir/online_users.txt"

find "$logs_dir" -maxdepth 1 -type f -delete
find "$client_data_dir" -maxdepth 1 -type f -delete
find "$data_dir" -maxdepth 1 -name '*.tmp' -type f -delete
find "$data_dir" -maxdepth 1 -name '*.hash' -type f -delete