$ErrorActionPreference = 'Stop'

# ── Reset data ────────────────────────────────────────────────────────────────
Write-Host "=== Resetting data... ===" -ForegroundColor Cyan

$dataDir      = 'SpertaServer/data'
$logsDir      = Join-Path $dataDir 'logs'
$clientDataDir = 'SpertaClient/data'

foreach ($dir in @($dataDir, $logsDir, $clientDataDir)) {
    $null = New-Item -ItemType Directory -Path $dir -Force
}

foreach ($name in @('users.txt','casas.txt','estados.txt','online_users.txt')) {
    $f = Join-Path $dataDir $name
    if (Test-Path $f) { Remove-Item $f -Force }
}

Get-ChildItem $logsDir      -File                         | Remove-Item -Force
Get-ChildItem $clientDataDir -File                        | Remove-Item -Force
Get-ChildItem $dataDir -Filter 'key.*'  -File             | Remove-Item -Force
Get-ChildItem $dataDir -Filter '*.cer'  -File             | Remove-Item -Force
Get-ChildItem $dataDir -Filter '*.tmp'  -File -ErrorAction SilentlyContinue | Remove-Item -Force
Get-ChildItem $dataDir -Filter '*.hash' -File -ErrorAction SilentlyContinue | Remove-Item -Force

# ── Compile server ────────────────────────────────────────────────────────────
Write-Host "=== Compiling server... ===" -ForegroundColor Cyan

$serverOut = 'SpertaServer/bin'
$null = New-Item -ItemType Directory -Path $serverOut -Force

$serverSources = Get-ChildItem 'SpertaServer/src/server/*.java' | ForEach-Object { $_.FullName }
if (-not $serverSources) { throw 'Nao foram encontrados ficheiros Java do servidor.' }

& javac -d $serverOut @serverSources
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

# ── Compile client ────────────────────────────────────────────────────────────
Write-Host "=== Compiling client... ===" -ForegroundColor Cyan

$classesDir = 'SpertaClient/bin/classes'
$jarPath    = 'SpertaClient/bin/SpertaClient.jar'
$null = New-Item -ItemType Directory -Path $classesDir -Force
$null = New-Item -ItemType Directory -Path 'SpertaClient/bin' -Force

$clientSources = Get-ChildItem 'SpertaClient/src/client/*.java' | ForEach-Object { $_.FullName }
if (-not $clientSources) { throw 'Nao foram encontrados ficheiros Java do cliente.' }

& javac -d $classesDir @clientSources
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

if (Test-Path $jarPath) { Remove-Item $jarPath -Force }

Push-Location $classesDir
& jar --create --file '..\SpertaClient.jar' --main-class 'SpertaClient.src.client.SpertaClient' .
$jarExit = $LASTEXITCODE
Pop-Location
if ($jarExit -ne 0) { exit $jarExit }

# ── Update client attestation ─────────────────────────────────────────────────
Write-Host "=== Updating client attestation... ===" -ForegroundColor Cyan

$refJarPath      = Join-Path $dataDir 'SpertaClient.jar'
$attestationFile = Join-Path $dataDir 'client_attestation.txt'

if (-not (Test-Path $jarPath)) {
    throw "O ficheiro $jarPath nao existe. Compile primeiro o cliente."
}

Copy-Item -Path $jarPath -Destination $refJarPath -Force
Set-Content -Path $attestationFile -Value $refJarPath -Encoding ascii

$hashFile = $attestationFile + '.hash'
if (Test-Path $hashFile) { Remove-Item $hashFile -Force }

Write-Host "=== All tasks completed successfully! ===" -ForegroundColor Green
