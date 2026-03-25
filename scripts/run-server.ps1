param(
    [int]$Port = 22345
)

$ErrorActionPreference = 'Stop'

& java -cp 'SpertaServer/src/server' 'SpertaServer' $Port

exit $LASTEXITCODE