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

# Voltámos a usar o -jar agora que já funciona!
& java -jar "SpertaClient/bin/SpertaClient.jar" $ServerAddress $Truststore $PasswordTruststore $Keystore $PasswordKeystore $UserName $Password

exit $LASTEXITCODE