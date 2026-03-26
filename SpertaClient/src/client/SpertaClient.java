package SpertaClient.src.client;

/**
 * SpertaClient.
 * Permite ao utilizador autenticar-se e interagir com o servidor SpertaServer,
 * executando comandos para gerir casas inteligentes, dispositivos e permissões.
 * Implementa a linha de comandos, atestação remota e transferência de
 * ficheiros.
 */
public class SpertaClient {

    /**
     * Ponto de entrada do cliente Sperta.
     * Lê argumentos de linha de comandos, processa o endereço do servidor e inicia
     * a ligação.
     * 
     * @param args Argumentos: <serverAddress> <user-id> <password>
     */
    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Erro!");
            System.out.println("Formato exigido: SpertaClient <serverAddress> <user-id> <password>");
            System.exit(-1);
        }

        String serverAddress = args[0];
        String user = args[1];
        String pass = args[2];

        String ip = serverAddress;
        int port = 22345; // Default

        if (serverAddress.contains(":")) {
            String[] parts = serverAddress.split(":");
            ip = parts[0];
            port = Integer.parseInt(parts[1]);
        }

        ClientSession session = new ClientSession(new AuthHandler(), new CommandHandler(new FileTransferManager()));
        session.start(ip, port, user, pass);
    }
}
