package SpertaClient.src.client;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.KeyStore;
import java.security.PublicKey;
import java.security.cert.Certificate;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;

/**
 * Processa comandos introduzidos pelo utilizador e trata a comunicacao com o
 * servidor.
 */
public class CommandHandler {
    private final FileTransferManager fileTransferManager;
    private final String keystorePath;
    private final String keystorePass;

    /**
     * Cria um processador de comandos com suporte a transferencia de ficheiros.
     *
     * @param fileTransferManager componente responsavel por guardar ficheiros
     *                            recebidos do servidor
     * @param keystorePath        caminho para a keystore do cliente (PKCS12)
     * @param keystorePass        password da keystore
     */
    public CommandHandler(FileTransferManager fileTransferManager, String keystorePath, String keystorePass) {
        this.fileTransferManager = fileTransferManager;
        this.keystorePath = keystorePath;
        this.keystorePass = keystorePass;
    }

    /**
     * Processa a linha de comando introduzida pelo utilizador.
     *
     * @param input     comando completo a enviar ao servidor
     * @param outStream stream de saida para o servidor
     * @param inStream  stream de entrada vindo do servidor
     */
    public void processCommand(String input, ObjectOutputStream outStream, ObjectInputStream inStream) {
        String[] parts = input.split(" ");
        String command = parts[0];

        switch (command) {
            case "CREATE":
                handleCreateCommand(parts, input, outStream, inStream);
                break;
            case "ADD":
                handleSimpleCommand(parts, input, "Formato incorreto. Tente: ADD <user> <hm> <s>", outStream,
                        inStream, 4);
                break;
            case "RD":
                handleSimpleCommand(parts, input, "Formato incorreto. Tente: RD <hm> <sec>", outStream, inStream,
                        3);
                break;
            case "EC":
                handleEcCommand(parts, input, outStream, inStream);
                break;
            case "RT":
                handleRtCommand(parts, input, outStream, inStream);
                break;
            case "RH":
                handleRhCommand(parts, input, outStream, inStream);
                break;
            default:
                System.out.println("Comando desconhecido! Tente novamente.");
                break;
        }
    }

    /**
     * Trata comandos com resposta textual simples do servidor.
     */
    private void handleSimpleCommand(String[] parts, String input, String errorMessage, ObjectOutputStream outStream,
            ObjectInputStream inStream, int expectedLength) {
        if (parts.length != expectedLength) {
            System.out.println(errorMessage);
            return;
        }

        try {
            sendCommand(input, outStream);
            String answer = (String) inStream.readObject();
            System.out.println("Server: " + answer);
        } catch (Exception e) {
            System.out.println("Erro ao comunicar com o servidor.");
        }
    }

    /**
     * Trata o comando EC, deixando a validacao do valor para o servidor.
     */
    private void handleEcCommand(String[] parts, String input, ObjectOutputStream outStream,
            ObjectInputStream inStream) {
        if (parts.length != 4) {
            System.out.println("Formato incorreto. Tente: EC <hm> <d> <int>");
            return;
        }

        handleSimpleCommand(parts, input, "Formato incorreto. Tente: EC <hm> <d> <int>", outStream, inStream, 4);
    }

    /**
     * Trata o comando RT e guarda localmente o resumo recebido do servidor.
     */
    private void handleRtCommand(String[] parts, String input, ObjectOutputStream outStream,
            ObjectInputStream inStream) {
        if (parts.length != 2) {
            System.out.println("Formato incorreto. Tente: RT <hm>");
            return;
        }

        try {
            sendCommand(input, outStream);
            String nomeFicheiro = "client_summary_" + parts[1] + ".txt";
            fileTransferManager.processFile(inStream, nomeFicheiro);
        } catch (Exception e) {
            System.out.println("Erro ao comunicar com o servidor.");
        }
    }

    /**
     * Trata o comando RH e guarda localmente o historico recebido do servidor.
     */
    private void handleRhCommand(String[] parts, String input, ObjectOutputStream outStream,
            ObjectInputStream inStream) {
        if (parts.length != 3) {
            System.out.println("Formato incorreto. Tente: RH <hm> <d>");
            return;
        }

        try {
            sendCommand(input, outStream);
            String nomeFicheiro = "client_log_" + parts[1] + "_" + parts[2] + ".csv";
            fileTransferManager.processFile(inStream, nomeFicheiro);
        } catch (Exception e) {
            System.out.println("Erro ao comunicar com o servidor.");
        }
    }

    /**
     * Trata o comando CREATE: cria a casa no servidor e envia 6 chaves de secção
     * cifradas com a chave pública RSA do owner.
     */
    private void handleCreateCommand(String[] parts, String input, ObjectOutputStream outStream,
            ObjectInputStream inStream) {
        if (parts.length != 2) {
            System.out.println("Formato incorreto. Tente: CREATE <hm>");
            return;
        }

        try {
            sendCommand(input, outStream);
            String response = (String) inStream.readObject();
            System.out.println("Server: " + response);

            if (!"OK".equals(response)) {
                return;
            }

            // Carregar chave pública RSA da keystore
            PublicKey pubKey = loadPublicKey();

            // Gerar e enviar chave AES-128 para cada secção, cifrada com RSA
            String[] sections = {"E", "G", "L", "M", "P", "S"};
            for (String s : sections) {
                KeyGenerator keyGen = KeyGenerator.getInstance("AES");
                keyGen.init(128);
                byte[] sectionKeyBytes = keyGen.generateKey().getEncoded();

                Cipher rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                rsaCipher.init(Cipher.ENCRYPT_MODE, pubKey);
                byte[] encryptedKey = rsaCipher.doFinal(sectionKeyBytes);

                outStream.writeObject(encryptedKey);
                outStream.flush();
            }

            String finalResponse = (String) inStream.readObject();
            System.out.println("Server: " + finalResponse);

        } catch (Exception e) {
            System.out.println("Erro ao criar casa.");
        }
    }

    /**
     * Carrega a chave pública RSA a partir da keystore PKCS12 do cliente.
     */
    private PublicKey loadPublicKey() throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream(keystorePath)) {
            ks.load(fis, keystorePass.toCharArray());
        }
        String alias = ks.aliases().nextElement();
        Certificate cert = ks.getCertificate(alias);
        return cert.getPublicKey();
    }

    /**
     * Envia uma linha de comando completa para o servidor.
     */
    private void sendCommand(String input, ObjectOutputStream outStream) throws Exception {
        outStream.writeObject(input);
        outStream.flush();
    }
}