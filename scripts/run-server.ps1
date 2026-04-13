param(
    [int]$Port = 22345,

    [Parameter(Mandatory = $true)]
    [string]$PasswordCifra,

    [Parameter(Mandatory = $true)]
    [string]$Keystore,

    [Parameter(Mandatory = $true)]
    [string]$PasswordKeystore
)

$ErrorActionPreference = 'Stop'

# Usamos as aspas para garantir que os argumentos são passados corretamente
& java -cp 'SpertaServer/bin' 'SpertaServer' $Port $PasswordCifra $Keystore $PasswordKeystore

exit $LASTEXITCODE