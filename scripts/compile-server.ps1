$ErrorActionPreference = 'Stop'

$outputDir = 'SpertaServer/bin'
$null = New-Item -ItemType Directory -Path $outputDir -Force

$sources = Get-ChildItem 'SpertaServer/src/server/*.java' | ForEach-Object { $_.FullName }

if ($sources.Count -eq 0) {
    throw 'Nao foram encontrados ficheiros Java do servidor.'
}

& javac -d $outputDir @sources

if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}