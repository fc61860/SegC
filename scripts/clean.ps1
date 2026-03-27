$ErrorActionPreference = 'Stop'

$classFiles = @()

if (Test-Path 'SpertaServer/bin') {
    $classFiles += Get-ChildItem 'SpertaServer/bin' -Filter '*.class' -File -Recurse
}

if (Test-Path 'SpertaClient/bin') {
    $classFiles += Get-ChildItem 'SpertaClient/bin' -Filter '*.class' -File -Recurse
}

foreach ($file in $classFiles) {
    Remove-Item $file.FullName -Force
}

if (Test-Path 'SpertaClient/bin/SpertaClient.jar') {
    Remove-Item 'SpertaClient/bin/SpertaClient.jar' -Force
}