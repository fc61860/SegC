param(
    [Parameter(Mandatory = $true)]
    [string]$ServerAddress,

    [Parameter(Mandatory = $true)]
    [string]$Truststore,

    [Parameter(Mandatory = $true)]
    [string]$PasswordTruststore,

    [Parameter(Mandatory = $true)]
    [string]$Keystore,

    [Parameter(Mandatory = $true)]
    [string]$PasswordKeystore,

    [Parameter(Mandatory = $true)]
    [string]$UserName,

    [Parameter(Mandatory = $true)]
    [string]$Password
)

$ErrorActionPreference = 'Stop'

& java -cp 'SpertaClient/bin/classes' 'SpertaClient.src.client.SpertaClient' $ServerAddress $Truststore $PasswordTruststore $Keystore $PasswordKeystore $UserName $Password

exit $LASTEXITCODE