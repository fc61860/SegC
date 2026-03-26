$ErrorActionPreference = 'Stop'

$sources = Get-ChildItem 'SpertaClient/src/client/*.java' | ForEach-Object { $_.FullName }

if (-not $sources) {
    Write-Error 'Nao foram encontrados ficheiros Java do cliente para compilar.'
}

& javac $sources

if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}