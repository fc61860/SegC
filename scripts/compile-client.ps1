$ErrorActionPreference = 'Stop'

$classesDir = 'SpertaClient/bin/classes'
$jarPath = 'SpertaClient/bin/SpertaClient.jar'

$null = New-Item -ItemType Directory -Path $classesDir -Force
$null = New-Item -ItemType Directory -Path 'SpertaClient/bin' -Force

$sources = Get-ChildItem 'SpertaClient/src/client/*.java' | ForEach-Object { $_.FullName }

if (-not $sources) {
    Write-Error 'Nao foram encontrados ficheiros Java do cliente para compilar.'
}

& javac -d $classesDir $sources

if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

if (Test-Path $jarPath) {
    Remove-Item $jarPath -Force
}

Push-Location $classesDir
& jar --create --file '..\SpertaClient.jar' --main-class 'SpertaClient.src.client.SpertaClient' .
$jarExitCode = $LASTEXITCODE
Pop-Location

if ($jarExitCode -ne 0) {
    exit $jarExitCode
}
