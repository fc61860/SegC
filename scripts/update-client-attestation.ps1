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

$jarSize = (Get-Item $jarPath).Length
Set-Content -Path $attestationFile -Value "SpertaClient.jar:$jarSize" -Encoding ascii