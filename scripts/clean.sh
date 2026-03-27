#!/usr/bin/env bash
set -euo pipefail

find "SpertaServer/bin" -name '*.class' -delete 2>/dev/null || true
find "SpertaClient/bin" -name '*.class' -delete 2>/dev/null || true
rm -f "SpertaClient/bin/SpertaClient.jar"