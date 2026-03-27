param(
    [int]$Port = 22345
)

$ErrorActionPreference = 'Stop'

& java -cp 'SpertaServer/bin' 'SpertaServer' $Port

exit $LASTEXITCODE