Projeto Segurança e Confiabilidade

|----------------------------------- WINDOWS-------------------------------||-------------------------------------------- UBUNTU ----------------------------------|
|   Apartir do diretório raiz                                              ||   Diretório raiz, dar permissao aos scripts na primeira vez: chmod +x scripts/*.sh   |
|---------------------------------- COMPILAÇÃO ----------------------------||------------------------------------------ COMPILAÇÃO --------------------------------|
|   Servidor:                                                              ||   Servidor:                                                                          |
|       - .\scripts\compile-server.ps1                                     ||       - ./scripts/compile-server.sh                                                  |
|   Cliente:                                                               ||   Cliente:                                                                           |
|       - .\scripts\compile-client.ps1                                     ||       - ./scripts/compile-client.sh                                                  |
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

As classes .java relativas ao servidor estão na pasta SpertaServer\src\server e relativas ao cliente na pasta SpertaClient\src\client

Os ficheiros users.txt, online_users.txt, casas.txt, estados.txt e client_size.txt são guardados na pasta SpertaServer\data

E os ficheiros .csv são guardados na pasta SpertaServer\data\logs

O ficheiro client_summary_<nomedacasa>.txt criado com o comando RT <nomedacasa> é guardado na pasta SpertaClient\data
tal como o ficheiro client_log_<nomedacasa>.csv criado com o comando RH <nomedacasa>

