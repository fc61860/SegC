Projeto SC

Problemas a resolver:
1. A GRANDE FALHA: Atestação do Cliente (Último parágrafo)
Lê com muita atenção o último parágrafo da página 6:

"O servidor deve atestar a aplicação cliente... Para tal, o servidor recebe o tamanho desta (vindo da própria aplicação cliente) e o valida com a informação que possui localmente sobre a aplicação."

O Problema: O teu cliente não envia o tamanho do seu próprio ficheiro executável, e o teu servidor não verifica nada disso. Isto é uma funcionalidade base de Segurança para evitar que um "hacker" crie um cliente falso para comunicar com o servidor.