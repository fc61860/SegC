================================================================================
  Segurança e Confiabilidade 2025/2026 - Fase 2
================================================================================

------------------------------------------------------------------------
FICHEIROS JAR (pre-compilados)
------------------------------------------------------------------------
  SpertaServer/bin/SpertaServer.jar
  SpertaClient/bin/SpertaClient.jar

------------------------------------------------------------------------
KEYSTORES, CERTIFICADOS E TRUSTSTORES
------------------------------------------------------------------------
Localizacao: security/

Os seguintes utilizadores já estão pre-configurados:

  Ficheiro                   Tipo       Password    Alias
  -------------------------  ---------  ----------  --------
  server-keystore.p12        PKCS12     changeit    server
  client1-keystore.p12       PKCS12     changeit    client1
  client2-keystore.p12       PKCS12     changeit    client2
  client3-keystore.p12       PKCS12     changeit    client3
  client-truststore.jks      JKS        changeit    (server cert pre-carregado)

NOTA: Os ficheiros .cer (*.cer) na pasta security/ são auxiliares opcionais,
    não são necessários para o funcionamento do programa. Os certificados
    dos clientes são gerados automaticamente em cada keystore e trocados
    automaticamente durante o login e comando ADD.

    Para adicionar um novo utilizador (ex: "jorge"), apenas é necessário
    criar a sua keystore com keytool:

  # Criar keystore + par de chaves RSA 2048
  Windows (PowerShell):
  keytool -genkeypair -alias jorge -keyalg RSA -keysize 2048 `
    -storetype PKCS12 `
    -keystore security/jorge-keystore.p12 -storepass changeit `
    -dname "CN=jorge"

  Linux/macOS (Bash):
  keytool -genkeypair -alias jorge -keyalg RSA -keysize 2048 \
    -storetype PKCS12 \
    -keystore security/jorge-keystore.p12 -storepass changeit \
    -dname "CN=jorge"

  Depois, executar o cliente com o novo utilizador (tudo o resto é automático):

  Windows (PowerShell):
  .\scripts\run-client.ps1 `
    -ServerAddress "127.0.0.1:22345" `
    -Truststore "security/client-truststore.jks" `
    -PasswordTruststore "changeit" `
    -Keystore "security/jorge-keystore.p12" `
    -PasswordKeystore "changeit" `
    -UserName "jorge" `
    -Password "abc123"

  Linux/macOS (Bash):
  ./scripts/run-client.sh 127.0.0.1:22345 security/client-truststore.jks changeit security/jorge-keystore.p12 changeit jorge abc123

  O servidor recebe o certificado automaticamente no primeiro login
  e guarda-o em SpertaServer/data/jorge.cer.

  A truststore do cliente (client-truststore.jks) já contém o
  certificado do servidor. Os certificados dos restantes utilizadores
  são adicionados automaticamente durante o comando ADD.

------------------------------------------------------------------------
COMPILAR E CONFIGURAR (a partir da raiz do projeto)
------------------------------------------------------------------------

Windows (PowerShell):
  .\scripts\setup-all.ps1

Linux/macOS (Bash):
  chmod +x scripts/setup-all.sh
  ./scripts/setup-all.sh

Este script executa os seguintes passos:
  1. Limpa os dados de execução anteriores (SpertaServer/data/, SpertaClient/data/)
  2. Compila o servidor  -> SpertaServer/bin/classes/ + SpertaServer/bin/SpertaServer.jar
  3. Compila o cliente   -> SpertaClient/bin/classes/ + SpertaClient/bin/SpertaClient.jar
  4. Copia o JAR do cliente para SpertaServer/data/SpertaClient.jar (referência de atestação)
     e escreve o caminho em SpertaServer/data/client_attestation.txt

------------------------------------------------------------------------
EXECUTAR O SERVIDOR
------------------------------------------------------------------------

Usando script:

Windows (PowerShell):
  .\scripts\run-server.ps1 -PasswordCifra "segredo" -Keystore "security/server-keystore.p12" -PasswordKeystore "changeit"

  Parametro opcional: -Port <numero>  (omissao: 22345)

Linux/macOS (Bash):
  ./scripts/run-server.sh 22345 segredo security/server-keystore.p12 changeit

Ou directamente com java (JAR):

Windows (PowerShell):
  java -jar SpertaServer/bin/SpertaServer.jar 22345 segredo security/server-keystore.p12 changeit

Linux/macOS (Bash):
  java -jar SpertaServer/bin/SpertaServer.jar 22345 segredo security/server-keystore.p12 changeit

NOTA: -PasswordCifra e a password PBE usada para cifrar os ficheiros do servidor
      (casas.txt, estados.txt, etc.). Pode ser qualquer valor escolhido pelo
      administrador. Deve ser a mesma em todos os arranques para que os dados
      existentes possam ser decifrados. Após setup-all os dados são apagados,
      por isso qualquer password funciona na primeira execução.

------------------------------------------------------------------------
EXECUTAR O CLIENTE
------------------------------------------------------------------------

Usando script:

Windows (PowerShell):
  .\scripts\run-client.ps1 `
    -ServerAddress "127.0.0.1:22345" `
    -Truststore "security/client-truststore.jks" `
    -PasswordTruststore "changeit" `
    -Keystore "security/client1-keystore.p12" `
    -PasswordKeystore "changeit" `
    -UserName "client1" `
    -Password "abc123"

Linux/macOS (Bash):
  ./scripts/run-client.sh 127.0.0.1:22345 security/client-truststore.jks changeit security/client1-keystore.p12 changeit client1 abc123

Ou directamente com java (JAR):

Windows (PowerShell):
  java -jar SpertaClient/bin/SpertaClient.jar `
    127.0.0.1:22345 security/client-truststore.jks changeit `
    security/client1-keystore.p12 changeit client1 abc123

Linux/macOS (Bash):
  java -jar SpertaClient/bin/SpertaClient.jar \
    127.0.0.1:22345 security/client-truststore.jks changeit \
    security/client1-keystore.p12 changeit client1 abc123

NOTA: O <userName> deve corresponder ao alias na keystore (client1, client2, client3).
      O porto e opcional; por omissao usa 22345.
      -Password e a password de login da aplicação. Não é pre-configurada:
      o primeiro login de cada cliente regista a password escolhida.
      Após setup-all não há utilizadores registados, por isso qualquer
      password funciona na primeira ligação.

================================================================================