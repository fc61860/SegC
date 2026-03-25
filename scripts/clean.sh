#!/usr/bin/env bash
set -euo pipefail

find "SpertaServer/src/server" -maxdepth 1 -name '*.class' -delete
find "SpertaClient/src/client" -maxdepth 1 -name '*.class' -delete