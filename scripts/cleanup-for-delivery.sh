#!/bin/bash
# Limpa ficheiros gerados e compilados que não devem ir no .zip
# Mantém: código-fonte, JARs, keystores, README
# Mantém: SpertaServer/data/SpertaClient.jar e client_attestation.txt (necessários para atestação)

echo "=== Limpando para entrega ===" 

# Remove ficheiros compilados
echo "Removendo .classes..."
rm -rf SpertaServer/bin/classes/
rm -rf SpertaClient/bin/classes/

# Remove dados gerados do servidor
echo "Removendo dados do servidor..."
rm -f SpertaServer/salt.bin
rm -f SpertaServer/data/users.txt
rm -f SpertaServer/data/users.txt.hash
rm -f SpertaServer/data/casas.txt
rm -f SpertaServer/data/casas.txt.hash
rm -f SpertaServer/data/estados.txt
rm -f SpertaServer/data/estados.txt.hash
rm -f SpertaServer/data/online_users.txt
rm -rf SpertaServer/data/logs/
find SpertaServer/data -maxdepth 1 -name '*.cer' -type f -delete

# Remove chaves de secção de utilizadores
echo "Removendo chaves de secção..."
find SpertaServer/data -maxdepth 1 -name 'key.*' -type f -delete

# Remove dados do cliente
echo "Removendo dados do cliente..."
rm -rf SpertaClient/data/

echo "=== Limpeza concluída ===" 
echo "Mantém: código-fonte, JARs, keystores, README"
echo "Mantém: SpertaServer/data/SpertaClient.jar + client_attestation.txt (atestação)"
