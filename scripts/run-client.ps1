param(
    [Parameter(Mandatory = $true)]
    [string]$ServerAddress,

    [Parameter(Mandatory = $true)]
    [string]$UserName,

    [Parameter(Mandatory = $true)]
    [string]$Password
)

$ErrorActionPreference = 'Stop'

& java -jar 'SpertaClient/bin/SpertaClient.jar' $ServerAddress $UserName $Password

exit $LASTEXITCODE