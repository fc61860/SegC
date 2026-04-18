import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Trata uma sessao individual de cliente, incluindo attestation, autenticacao e
 * processamento de comandos do protocolo.
 */
public class ClientHandler extends Thread {
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
     * Valida a attestation do cliente com protocolo nonce + SHA-256 anti-replay.
     * O servidor gera um nonce, envia ao cliente, e compara o hash recebido
     * com SHA-256(nonce_bytes || bytes_JAR_referencia) calculado localmente.
     *
     * @param inStream  stream de entrada da ligacao
     * @param outStream stream de saida da ligacao
     * @return true se a validacao for bem-sucedida; false caso contrario
     * @throws IOException se ocorrer um erro na comunicacao
     */
    private boolean validateClient(ObjectInputStream inStream, ObjectOutputStream outStream) throws IOException {
        try {
            // Gerar nonce aleatorio
            SecureRandom sr = new SecureRandom();
            byte[] nonceBytes = new byte[8];
            sr.nextBytes(nonceBytes);
            long nonce = ByteBuffer.wrap(nonceBytes).getLong();

            // Enviar nonce ao cliente
            outStream.writeLong(nonce);
            outStream.flush();

            // Ler caminho da copia de referencia do JAR (ficheiro cifrado)
            String refJarPath;
            try {
                byte[] attBytes = SpertaServer.readDecrypted(SpertaServer.FICHEIRO_CLIENTATTESTATION);
                refJarPath = new String(attBytes, java.nio.charset.StandardCharsets.UTF_8).trim();
            } catch (Exception e) {
                sendResponse(outStream, "NOK-ATTEST");
                return false;
            }
            if (refJarPath.isEmpty()) {
                sendResponse(outStream, "NOK");
                return false;
            }

            // Ler bytes do JAR de referencia
            File refJar = new File(refJarPath);
            if (!refJar.exists()) {
                sendResponse(outStream, "NOK");
                return false;
            }
            byte[] refJarBytes = Files.readAllBytes(refJar.toPath());

            // Calcular SHA-256(nonce_bytes || bytes_JAR_referencia)
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(nonceBytes);
            md.update(refJarBytes);
            byte[] expectedHashBytes = md.digest();

            StringBuilder expectedHex = new StringBuilder();
            for (byte b : expectedHashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    expectedHex.append('0');
                expectedHex.append(hex);
            }
            String expectedHash = expectedHex.toString().toUpperCase();

            // Receber hash do cliente e comparar
            String clientHash = (String) inStream.readObject();
            if (!clientHash.equals(expectedHash)) {
                System.out.println("Atestacao falhou! Hash invalido ou cliente adulterado.");
                sendResponse(outStream, "NOK");
                return false;
            }

            sendResponse(outStream, "OK-ATTEST");
            return true;
        } catch (ClassNotFoundException | NoSuchAlgorithmException e) {
            sendResponse(outStream, "NOK-ATTEST");
            return false;
        }
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
                criarCasa(user, inStream, outStream, parts);
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
     * criacao da casa. Apos criacao bem-sucedida, recebe 6 chaves de seccao
     * cifradas com RSA e guarda-as no servidor.
     */
    private void criarCasa(String user, ObjectInputStream inStream, ObjectOutputStream outStream, String[] parts)
            throws IOException, ClassNotFoundException {
        if (!hasExactArgs(parts, 2) || parts[1].contains(";")) {
            sendResponse(outStream, "NOK");
            return;
        }

        String hm = parts[1];
        String resultado = houseManager.criarCasa(user, hm);
        sendResponse(outStream, resultado);

        if (!"OK".equals(resultado)) {
            return;
        }

        // Receber e guardar 6 chaves de seccao cifradas com a chave publica RSA do
        // owner
        String[] sections = { "E", "G", "L", "M", "P", "S" };
        for (String s : sections) {
            byte[] encryptedKey = (byte[]) inStream.readObject();
            String keyPath = "SpertaServer/data/key." + hm + "." + s + "." + user;
            Files.write(new File(keyPath).toPath(), encryptedKey);
        }

        sendResponse(outStream, "OK");
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
