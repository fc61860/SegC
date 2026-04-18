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
     * Lê argumentos de linha de comandos, processa o endereço do servidor e inicia a ligação.
     * @param args Argumentos: <serverAddress> <truststore> <password-truststore> <keystore> <password-keystore> <user-id> <password>
     */
    public static void main(String[] args) {
        if (args.length != 7) {
            System.out.println("Erro!");
            System.out.println("Formato exigido: SpertaClient <serverAddress> <truststore> <password-truststore> <keystore> <password-keystore> <user-id> <password>");
            System.exit(-1);
        }

        String serverAddress = args[0];
        String truststorePath = args[1];
        String truststorePass = args[2];
        String keystorePath = args[3];
        String keystorePass = args[4];
        String user = args[5];
        String pass = args[6];

        String ip = serverAddress;
        int port = 22345; 

        if (serverAddress.contains(":")) {
            String[] parts = serverAddress.split(":");
            ip = parts[0];
            port = Integer.parseInt(parts[1]);
        }

        // Verificar a identidade do Servidor
        System.setProperty("javax.net.ssl.trustStore", truststorePath);
        System.setProperty("javax.net.ssl.trustStorePassword", truststorePass);
        
        // Configurar a Keystore do Cliente
        System.setProperty("javax.net.ssl.keyStore", keystorePath);
        System.setProperty("javax.net.ssl.keyStorePassword", keystorePass);
        // Se usar JCEKS, descomenta:
        // System.setProperty("javax.net.ssl.keyStoreType", "JCEKS");

        ClientSession session = new ClientSession(new AuthHandler(), new CommandHandler(new FileTransferManager(), keystorePath, keystorePass));
        session.start(ip, port, user, pass);
    }
}
