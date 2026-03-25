$ErrorActionPreference = 'Stop'

$sources = Get-ChildItem 'SpertaServer/src/server/*.java' | ForEach-Object { $_.FullName }

if ($sources.Count -eq 0) {
    throw 'Nao foram encontrados ficheiros Java do servidor.'
}

& javac @sources

if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}