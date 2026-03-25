#!/usr/bin/env bash
set -euo pipefail

PORT="${1:-22345}"

java -cp "SpertaServer/src/server" SpertaServer "$PORT"