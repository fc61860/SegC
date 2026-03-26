$ErrorActionPreference = 'Stop'

$dataDir = 'SpertaServer/data'
$logsDir = Join-Path $dataDir 'logs'
$clientDataDir = 'SpertaClient/data'
$summariesDir = 'summaries'

$filesToReset = @(
    (Join-Path $dataDir 'users.txt'),
    (Join-Path $dataDir 'casas.txt'),
    (Join-Path $dataDir 'estados.txt'),
    (Join-Path $dataDir 'online_users.txt')
)

foreach ($file in $filesToReset) {
    if (-not (Test-Path $file)) {
        New-Item -ItemType File -Path $file -Force | Out-Null
    }

    Set-Content -Path $file -Value $null
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

if (Test-Path $summariesDir) {
    Get-ChildItem $summariesDir -File | Remove-Item -Force
} else {
    New-Item -ItemType Directory -Path $summariesDir -Force | Out-Null
}

Get-ChildItem $dataDir -Filter '*.tmp' -File -ErrorAction SilentlyContinue | Remove-Item -Force