import java.io.BufferedReader;
import java.io.EOFException;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Trata uma sessao individual de cliente, incluindo attestation, autenticacao e
 * processamento de comandos do protocolo.
 */
public class ClientHandler extends Thread {
    private static final String FICHEIRO_CLIENTSIZE = "SpertaServer/data/client_size.txt";

    private final Socket socket;
    private final UserManager userManager;
    private final HouseManager houseManager;
    private final DeviceManager deviceManager;
    private final PermissionsManager permissionsManager;
    private final LogManager logManager;

    /**
     * Cria um handler para uma ligacao de cliente ja aceite pelo servidor.
     *
     * @param socket             socket associado ao cliente
     * @param userManager        gestor de utilizadores
     * @param houseManager       gestor de casas
     * @param deviceManager      gestor de dispositivos
     * @param permissionsManager gestor de permissoes
     * @param logManager         gestor de logs e historicos
     */
    public ClientHandler(Socket socket, UserManager userManager, HouseManager houseManager, DeviceManager deviceManager,
            PermissionsManager permissionsManager, LogManager logManager) {
        this.socket = socket;
        this.userManager = userManager;
        this.houseManager = houseManager;
        this.deviceManager = deviceManager;
        this.permissionsManager = permissionsManager;
        this.logManager = logManager;
        System.out.println("thread do server para cada cliente");
    }

    /**
     * Executa o ciclo de vida completo da sessao do cliente ate a ligacao terminar.
     */
    @Override
    public void run() {
        String user = null;
        boolean loggedIn = false;

        try (ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream())) {
            if (!validateClient(inStream, outStream)) {
                return;
            }

            user = (String) inStream.readObject();
            if (userManager.isUserOnlinePublic(user)) {
                sendResponse(outStream, "USERON");
                return;
            }

            sendResponse(outStream, "OK");

            String authResponse = userManager.autenticarCliente(user, inStream, outStream);
            if (authResponse == null) {
                return;
            }

            if (!userManager.loginUser(user)) {
                sendResponse(outStream, "USERON");
                return;
            }

            sendResponse(outStream, authResponse);
            loggedIn = true;

            while (true) {
                processCommand(user, inStream, outStream);
            }
        } catch (EOFException e) {
            System.out.println("Client disconnected.");
        } catch (IOException e) {
            System.out.println("Connection lost.");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (loggedIn && user != null) {
                try {
                    userManager.logoutUser(user);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Valida a attestation do cliente comparando o tamanho enviado com o valor
     * esperado pelo servidor.
     *
     * @param inStream  stream de entrada da ligacao
     * @param outStream stream de saida da ligacao
     * @return true se a validacao for bem-sucedida; false caso contrario
     * @throws IOException se ocorrer um erro na leitura do tamanho ou no envio da
     *                     resposta
     */
    private boolean validateClient(ObjectInputStream inStream, ObjectOutputStream outStream) throws IOException {
        long clientSize = inStream.readLong();

        try (BufferedReader reader = new BufferedReader(new FileReader(FICHEIRO_CLIENTSIZE))) {
            String line = reader.readLine();
            if (line == null || !line.contains(":")) {
                sendResponse(outStream, "NOK");
                return false;
            }

            String[] parts = line.split(":");
            long expectedSize = Long.parseLong(parts[1]);
            if (clientSize != expectedSize) {
                sendResponse(outStream, "NOK");
                return false;
            }
        }

        sendResponse(outStream, "OK");
        return true;
    }

    /**
     * Le o proximo comando enviado pelo cliente e delega o tratamento para o
     * respetivo helper de protocolo.
     *
     * @param user      utilizador autenticado da sessao
     * @param inStream  stream de entrada da ligacao
     * @param outStream stream de saida da ligacao
     * @throws IOException            se ocorrer um erro durante a comunicacao
     * @throws ClassNotFoundException se o objeto recebido nao tiver o tipo esperado
     */
    private void processCommand(String user, ObjectInputStream inStream, ObjectOutputStream outStream)
            throws IOException, ClassNotFoundException {
        String message = (String) inStream.readObject();
        String[] parts = message.split(" ");
        String command = parts[0];

        switch (command) {
            case "CREATE":
                criarCasa(user, outStream, parts);
                break;
            case "ADD":
                adicionarUtilizador(user, outStream, parts);
                break;
            case "RD":
                registarDispositivo(user, outStream, parts);
                break;
            case "EC":
                envioValor(user, outStream, parts);
                break;
            case "RT":
                receberTemp(user, outStream, parts);
                break;
            case "RH":
                receberHistorico(user, outStream, parts);
                break;
            default:
                sendResponse(outStream, "NOCOMMAND");
        }
    }

    /**
     * Trata o comando CREATE, validando o numero de argumentos e delegando a
     * criacao da casa.
     */
    private void criarCasa(String user, ObjectOutputStream outStream, String[] parts) throws IOException {
        if (!hasExactArgs(parts, 2) || parts[1].contains(";")) {
            sendResponse(outStream, "NOK");
            return;
        }

        sendResponse(outStream, houseManager.criarCasa(user, parts[1]));
    }

    /**
     * Trata o comando ADD e envia ao cliente o resultado da atribuicao de
     * permissoes.
     */
    private void adicionarUtilizador(String user, ObjectOutputStream outStream, String[] parts)
            throws IOException {
        if (!hasExactArgs(parts, 4)) {
            sendResponse(outStream, "NOK");
            return;
        }

        sendResponse(outStream,
                permissionsManager.adicionarUtilizador(userManager, houseManager, user, parts[1], parts[2], parts[3]));
    }

    /**
     * Trata o comando RD para registo de novos dispositivos numa casa.
     */
    private void registarDispositivo(String user, ObjectOutputStream outStream, String[] parts)
            throws IOException {
        if (!hasExactArgs(parts, 3)) {
            sendResponse(outStream, "NOK");
            return;
        }

        sendResponse(outStream, deviceManager.registarDispositivo(houseManager, user, parts[1], parts[2]));
    }

    /**
     * Trata o comando EC para atualizacao do valor de um dispositivo.
     */
    private void envioValor(String user, ObjectOutputStream outStream, String[] parts) throws IOException {
        if (!hasExactArgs(parts, 4)) {
            sendResponse(outStream, "NOK");
            return;
        }

        sendResponse(outStream,
                deviceManager.envioValor(houseManager, permissionsManager, user, parts[1], parts[2], parts[3]));
    }

    /**
     * Trata o comando RT e delega o envio do resumo de leituras ao LogManager.
     */
    private void receberTemp(String user, ObjectOutputStream outStream, String[] parts) throws IOException {
        if (!hasExactArgs(parts, 2)) {
            sendResponse(outStream, "NOK");
            return;
        }

        logManager.receberTemp(houseManager, user, parts[1], outStream);
    }

    /**
     * Trata o comando RH e delega o envio do historico de um dispositivo.
     */
    private void receberHistorico(String user, ObjectOutputStream outStream, String[] parts) throws IOException {
        if (!hasExactArgs(parts, 3)) {
            sendResponse(outStream, "NOK");
            return;
        }

        logManager.receberHistorico(houseManager, permissionsManager, deviceManager, user, parts[1], parts[2],
                outStream);
    }

    /**
     * Verifica se a mensagem do protocolo tem exatamente o numero esperado de
     * tokens.
     */
    private boolean hasExactArgs(String[] parts, int expectedLength) {
        return parts.length == expectedLength;
    }

    /**
     * Envia uma resposta textual simples ao cliente atraves do stream de objetos.
     */
    private void sendResponse(ObjectOutputStream outStream, String response) throws IOException {
        outStream.writeObject(response);
        outStream.flush();
    }

}
