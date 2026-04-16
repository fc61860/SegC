$ErrorActionPreference = 'Stop'

$jarPath = 'SpertaClient/bin/SpertaClient.jar'
$refJarPath = 'SpertaServer/data/SpertaClient.jar'
$attestationFile = 'SpertaServer/data/client_attestation.txt'

if (-not (Test-Path $jarPath)) {
    Write-Error 'O ficheiro SpertaClient/bin/SpertaClient.jar nao existe. Compile primeiro o cliente.'
}

$null = New-Item -ItemType Directory -Path 'SpertaServer/data' -Force

# Copiar o JAR para a pasta do servidor como copia de referencia
Copy-Item -Path $jarPath -Destination $refJarPath -Force

# Guardar o caminho da copia de referencia no ficheiro de attestation
Set-Content -Path $attestationFile -Value $refJarPath -Encoding ascii