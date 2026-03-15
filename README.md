Projeto SC

Problemas a resolver:
1. PONTOS PERDIDOS: Arranque da Aplicação (Argumentos do Terminal)
O enunciado é muito claro na página 3 sobre como os programas devem ser iniciados na linha de comandos:

SpertaClient <serverAddress> <user-id> <password>

O Problema: O teu cliente atual ignora os argumentos (args do main) e usa um Scanner para pedir o username e a password interativamente, e o IP está cravado a "127.0.0.1".

Como corrigir: Tens de adaptar o teu main e o startClient para usarem os dados do terminal. O Scanner só deve ser usado se a password inicial (a que veio nos argumentos) estiver errada (como diz o enunciado: "O utilizador terá oportunidade de voltar a tentar outra password").

2. A GRANDE FALHA: Atestação do Cliente (Último parágrafo)
Lê com muita atenção o último parágrafo da página 6:

"O servidor deve atestar a aplicação cliente... Para tal, o servidor recebe o tamanho desta (vindo da própria aplicação cliente) e o valida com a informação que possui localmente sobre a aplicação."

O Problema: O teu cliente não envia o tamanho do seu próprio ficheiro executável, e o teu servidor não verifica nada disso. Isto é uma funcionalidade base de Segurança para evitar que um "hacker" crie um cliente falso para comunicar com o servidor.