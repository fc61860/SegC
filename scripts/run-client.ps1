param(
    [Parameter(Mandatory = $true)]
    [string]$ServerAddress,

    [Parameter(Mandatory = $true)]
    [string]$UserName,

    [Parameter(Mandatory = $true)]
    [string]$Password
)

$ErrorActionPreference = 'Stop'

& java -cp '.' 'SpertaClient.src.client.SpertaClient' $ServerAddress $UserName $Password

exit $LASTEXITCODE