#!/bin/bash

# Exit immediately if a command exits with a non-zero status.
set -e

# Get the directory of the current script
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

echo "Running reset-data.sh..."
"$DIR/reset-data.sh"

echo "Running compile-server.sh..."
"$DIR/compile-server.sh"

echo "Running compile-client.sh..."
"$DIR/compile-client.sh"

echo "Running update-client-attestation.sh..."
"$DIR/update-client-attestation.sh"

echo "All tasks completed successfully!"
