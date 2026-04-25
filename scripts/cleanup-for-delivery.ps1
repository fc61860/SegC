# Limpa ficheiros gerados e compilados que não devem ir no .zip
# Mantém: código-fonte, JARs, keystores, README

Write-Host "=== Limpando para entrega ===" -ForegroundColor Green

# Remove ficheiros compilados
Write-Host "Removendo .classes..."
Remove-Item -Recurse -Force "SpertaServer/bin/classes/" -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force "SpertaClient/bin/classes/" -ErrorAction SilentlyContinue

# Remove dados gerados do servidor
Write-Host "Removendo dados do servidor..."
Remove-Item -Force "SpertaServer/salt.bin" -ErrorAction SilentlyContinue
Remove-Item -Force "SpertaServer/data/users.txt" -ErrorAction SilentlyContinue
Remove-Item -Force "SpertaServer/data/users.txt.hash" -ErrorAction SilentlyContinue
Remove-Item -Force "SpertaServer/data/casas.txt" -ErrorAction SilentlyContinue
Remove-Item -Force "SpertaServer/data/casas.txt.hash" -ErrorAction SilentlyContinue
Remove-Item -Force "SpertaServer/data/estados.txt" -ErrorAction SilentlyContinue
Remove-Item -Force "SpertaServer/data/estados.txt.hash" -ErrorAction SilentlyContinue
Remove-Item -Force "SpertaServer/data/online_users.txt" -ErrorAction SilentlyContinue
Remove-Item -Force "SpertaServer/data/client_attestation.txt" -ErrorAction SilentlyContinue
Remove-Item -Force "SpertaServer/data/client_attestation.txt.hash" -ErrorAction SilentlyContinue
Remove-Item -Force "SpertaServer/data/SpertaClient.jar" -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force "SpertaServer/data/logs/" -ErrorAction SilentlyContinue
Remove-Item -Force "SpertaServer/data/*.cer" -ErrorAction SilentlyContinue

# Remove certificados de utilizadores gerados
Write-Host "Removendo certificados de utilizadores..."
Get-ChildItem "SpertaServer/data/" -Name "*.cer" -ErrorAction SilentlyContinue | ForEach-Object {
    Remove-Item "SpertaServer/data/$_" -Force -ErrorAction SilentlyContinue
}

# Remove chaves de secção de utilizadores
Write-Host "Removendo chaves de secção..."
Get-ChildItem "SpertaServer/data/" -Name "key.*" -ErrorAction SilentlyContinue | ForEach-Object {
    Remove-Item "SpertaServer/data/$_" -Force -ErrorAction SilentlyContinue
}

# Remove dados do cliente
Write-Host "Removendo dados do cliente..."
Remove-Item -Recurse -Force "SpertaClient/data/" -ErrorAction SilentlyContinue

Write-Host "=== Limpeza concluída ===" -ForegroundColor Green
Write-Host ""
Write-Host "Mantém: código-fonte, JARs, keystores, README" -ForegroundColor Cyan
