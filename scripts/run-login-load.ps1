param(
    [Parameter(Mandatory = $true)]
    [string]$ServerAddress,

    [Parameter(Mandatory = $true)]
    [string]$BaseUser,

    [Parameter(Mandatory = $true)]
    [string]$Password,

    [Parameter(Mandatory = $true)]
    [int]$Count,

    [int]$HoldSeconds = 10,

    [bool]$SameUser = $false
)

$ErrorActionPreference = 'Stop'

& java -cp 'SpertaClient/bin' 'SpertaClient.src.client.LoginLoadTester' `
    $ServerAddress $BaseUser $Password $Count $HoldSeconds $SameUser

exit $LASTEXITCODE