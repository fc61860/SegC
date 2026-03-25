Projeto Segurança e Confiabilidade

|----------------------------------- WINDOWS-------------------------------||-------------------------------------------- UBUNTU --------------------------------------|
|   Apartir do diretório raiz                                              ||   Diretório raiz, dar permissao aos scripts na primeira vez: chmod +x scripts/*.sh       |
|---------------------------------- COMPILAÇÃO ----------------------------||------------------------------------------ COMPILAÇÃO ------------------------------------|
|   Servidor:                                                              ||   Servidor:                                                                              |
|       - .\scripts\compile-server.ps1                                     ||       - ./scripts/compile-server.sh                                                      |
|   Cliente:                                                               ||   Cliente:                                                                               |
|       - .\scripts\compile-client.ps1                                     ||       - ./scripts/compile-client.sh                                                      |
|----------------------------------- EXECUÇÃO -----------------------------||------------------------------------------ EXECUÇÃO --------------------------------------|
|   Servidor:                                                              ||   Servidor:                                                                              |
|       - .\scripts\run-server.ps1                                         ||       - ./scripts/run-server.sh                                                          |
|   Cliente:                                                               ||   Cliente:                                                                               |
|       - .\scripts\run-client.ps1 <IP:Port> <userName> <password>         ||       - ./scripts/run-client.sh <IP:Port> <userName> <password>                          |
|----------------------------------- LIMPEZA ------------------------------||------------------------------------------- LIMPEZA --------------------------------------|
|   Windows PowerShell:                                                    ||   Ubuntu Bash:                                                                           |
|       - .\scripts\clean.ps1                                              ||       - ./scripts/clean.sh                                                               |
|--------------------------------------------------------------------------||------------------------------------------------------------------------------------------|

Testes Passados
- Autenticação
- Operação CREATE
- Operação RD
- Operação EC
- Operação ADD

Tenho de dividir o código da parte do cliente
