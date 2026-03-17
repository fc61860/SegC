Projeto SC

Problemas a resolver:
1. A GRANDE FALHA: Atestação do Cliente (Último parágrafo)
Lê com muita atenção o último parágrafo da página 6:

"O servidor deve atestar a aplicação cliente... Para tal, o servidor recebe o tamanho desta (vindo da própria aplicação cliente) e o valida com a informação que possui localmente sobre a aplicação."

O Problema: O teu cliente não envia o tamanho do seu próprio ficheiro executável, e o teu servidor não verifica nada disso. Isto é uma funcionalidade base de Segurança para evitar que um "hacker" crie um cliente falso para comunicar com o servidor.
2. Verificar os outputs de RT e RH e a sua respota no caso de correr tudo bem ou de não haver dados a guardar.
3. Todos os comandos estão funcionais e têm o output esperado
4. Corrigir o output nos .csv
(Opcional)
4. Corrigir a ordem de adição de dispositivos a uma casa, se possivel. EX: Ao adicionar M1 depois G1 e depois M2 o ficheiro estados.txt guarda M1, G1, M2 e ficaria melhor se guardasse M1, M2, G1