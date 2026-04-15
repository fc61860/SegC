$ErrorActionPreference = 'Stop'

$jarPath = 'SpertaClient/bin/SpertaClient.jar'
$attestationFile = 'SpertaServer/data/client_size.txt'

if (-not (Test-Path $jarPath)) {
    Write-Error 'O ficheiro SpertaClient/bin/SpertaClient.jar nao existe. Compile primeiro o cliente.'
}

$null = New-Item -ItemType Directory -Path 'SpertaServer/data' -Force

if (-not (Test-Path $attestationFile)) {
    New-Item -ItemType File -Path $attestationFile -Force | Out-Null
}

$jarHash = (Get-FileHash -Path $jarPath -Algorithm SHA256).Hash
Set-Content -Path $attestationFile -Value "SpertaClient.jar:$jarHash" -Encoding ascii