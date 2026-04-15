Projeto Segurança e Confiabilidade

|----------------------------------- WINDOWS-------------------------------||-------------------------------------------- UBUNTU ----------------------------------|
|   Apartir do diretório raiz                                              ||   Diretório raiz, dar permissao aos scripts na primeira vez: chmod +x scripts/*.sh   |
|---------------------------------- COMPILAÇÃO ----------------------------||------------------------------------------ COMPILAÇÃO --------------------------------|
|   Servidor:                                                              ||   Servidor:                                                                          |
|       - .\scripts\compile-server.ps1                                     ||       - ./scripts/compile-server.sh                                                  |
|   Cliente:                                                               ||   Cliente:                                                                           |
|       - .\scripts\compile-client.ps1                                     ||       - ./scripts/compile-client.sh                                                  |
|       - .\scripts\update-client-attestation.ps1                          ||       - ./scripts/update-client-attestation.sh                                       |
|----------------------------------- EXECUÇÃO -----------------------------||------------------------------------------ EXECUÇÃO ----------------------------------|
|   Servidor:                                                              ||   Servidor:                                                                          |
|       - .\scripts\run-server.ps1                                         ||       - ./scripts/run-server.sh                                                      |
|   Cliente:                                                               ||   Cliente:                                                                           |
|       - .\scripts\run-client.ps1 <serverAddress> <userName> <password>   ||       - ./scripts/run-client.sh <serverAddress> <userName> <password>                |
|------------------------------ LIMPEZA (.class) --------------------------||------------------------------------- LIMPEZA (.class) -------------------------------|
|   Windows PowerShell:                                                    ||   Ubuntu Bash:                                                                       |
|       - .\scripts\clean.ps1                                              ||       - ./scripts/clean.sh                                                           |
|---------------------------------- RESET-DATA ----------------------------||----------------------------------------- RESET-DATA ---------------------------------|
|   Windows PowerShell:                                                    ||   Ubuntu Bash:                                                                       |
|       - .\scripts\reset-data.ps1                                         ||       - ./scripts/reset-data.sh                                                      |
|--------------------------------------------------------------------------||--------------------------------------------------------------------------------------|

O script compile-client apenas compila o cliente e gera o ficheiro SpertaClient\bin\SpertaClient.jar
O script update-client-attestation atualiza o ficheiro SpertaServer\data\client_size.txt com o nome e o tamanho do SpertaClient.jar

As classes .java relativas ao servidor estão na pasta SpertaServer\src\server e relativas ao cliente na pasta SpertaClient\src\client

Os ficheiros .class do cliente são gerados na pasta SpertaClient\bin\classes e o ficheiro SpertaClient.jar é gerado na pasta SpertaClient\bin

Os ficheiros users.txt, online_users.txt, casas.txt, estados.txt e client_size.txt são guardados na pasta SpertaServer\data
E os ficheiros .csv são guardados na pasta SpertaServer\data\logs

O ficheiro client_summary_<nomedacasa>.txt criado com o comando RT <nomedacasa> é guardado na pasta SpertaClient\data
tal como o ficheiro client_log_<nomedacasa>.csv criado com o comando RH <nomedacasa>

Para gerar de novo os certificados:

1. Correr keytool commands em /security
2. Password: changeit

# WINDOWS
Para ligar o server: .\scripts\run-server.ps1 -Port 22345 -PasswordCifra "minhaCifraSecreta" -Keystore "security/server-keystore.p12" -PasswordKeystore "changeit"
Para ligar o cliente: .\scripts\run-client.ps1 -ServerAddress "127.0.0.1:22345" -Truststore "security/client-truststore.jks" -PasswordTruststore "changeit" -Keystore "security/client1-keystore.p12" -PasswordKeystore "changeit" -UserName "client1" -Password "MinhaPass123"