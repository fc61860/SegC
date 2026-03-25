$ErrorActionPreference = 'Stop'

& javac 'SpertaClient/src/client/SpertaClient.java'

if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}