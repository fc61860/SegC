$ErrorActionPreference = 'Stop'

$classFiles = Get-ChildItem 'SpertaServer/src/server' -Filter '*.class' -File
$classFiles += Get-ChildItem 'SpertaClient/src/client' -Filter '*.class' -File

foreach ($file in $classFiles) {
    Remove-Item $file.FullName -Force
}