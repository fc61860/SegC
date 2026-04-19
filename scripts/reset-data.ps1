$ErrorActionPreference = 'Stop'

$dataDir = 'SpertaServer/data'
$logsDir = Join-Path $dataDir 'logs'
$clientDataDir = 'SpertaClient/data'

$filesToReset = @(
    (Join-Path $dataDir 'users.txt'),
    (Join-Path $dataDir 'casas.txt'),
    (Join-Path $dataDir 'estados.txt'),
    (Join-Path $dataDir 'online_users.txt')
)

foreach ($file in $filesToReset) {
    if (Test-Path $file) {
        Remove-Item -Path $file -Force
    }
}

if (Test-Path $logsDir) {
    Get-ChildItem $logsDir -File | Remove-Item -Force
} else {
    New-Item -ItemType Directory -Path $logsDir -Force | Out-Null
}

if (Test-Path $clientDataDir) {
    Get-ChildItem $clientDataDir -File | Remove-Item -Force
} else {
    New-Item -ItemType Directory -Path $clientDataDir -Force | Out-Null
}

# Remover ficheiros de chaves de seccao, certificados e hashes
Get-ChildItem $dataDir -Filter 'key.*' -File | Remove-Item -Force
Get-ChildItem $dataDir -Filter '*.cer' -File | Remove-Item -Force
Get-ChildItem $dataDir -Filter '*.hash' -File | Remove-Item -Force

Get-ChildItem $dataDir -Filter '*.tmp' -File -ErrorAction SilentlyContinue | Remove-Item -Force
Get-ChildItem $dataDir -Filter '*.hash' -File -ErrorAction SilentlyContinue | Remove-Item -Force