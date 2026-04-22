param(
    [Parameter(Mandatory = $true)]
    [string]$ServerAddress,

    [Parameter(Mandatory = $true)]
    [string]$BaseUser,

    [Parameter(Mandatory = $true)]
    [string]$Password,

    [Parameter(Mandatory = $true)]
    [int]$Count,

    [int]$HoldSeconds = 5,

    [bool]$SameUser = $false,

    [string]$Truststore     = 'security/client-truststore.jks',
    [string]$TruststorePass = 'changeit',
    [string]$Keystore       = 'security/client1-keystore.p12',
    [string]$KeystorePass   = 'changeit',
    [string]$JarPath        = 'SpertaClient/bin/SpertaClient.jar'
)

$ErrorActionPreference = 'Stop'

& java -cp 'SpertaClient/bin/classes' 'SpertaClient.src.client.LoginLoadTester' `
    $ServerAddress $BaseUser $Password $Count $HoldSeconds $SameUser `
    $Truststore $TruststorePass $Keystore $KeystorePass $JarPath

exit $LASTEXITCODE