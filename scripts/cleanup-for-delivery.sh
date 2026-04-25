#!/bin/bash
# Limpa ficheiros gerados e compilados que não devem ir no .zip
# Mantém: código-fonte, JARs, keystores, README

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
rm -f SpertaServer/data/client_attestation.txt
rm -f SpertaServer/data/client_attestation.txt.hash
rm -f SpertaServer/data/SpertaClient.jar
rm -rf SpertaServer/data/logs/
rm -f SpertaServer/data/*.cer

# Remove chaves de secção de utilizadores
echo "Removendo chaves de secção..."
rm -f SpertaServer/data/key.*

# Remove dados do cliente
echo "Removendo dados do cliente..."
rm -rf SpertaClient/data/

echo "=== Limpeza concluída ==="
echo "Mantém: código-fonte, JARs, keystores, README"
