package SpertaClient.src.client;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

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
                handleAddCommand(parts, input, outStream, inStream);
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

            // Carregar chave publica RSA da keystore
            PublicKey pubKey = CryptoUtils.loadPublicKey(keystorePath, keystorePass);

            // Gerar e enviar chave AES-128 para cada seccao, cifrada com RSA
            String[] sections = {"E", "G", "L", "M", "P", "S"};
            for (String s : sections) {
                byte[] sectionKeyBytes = CryptoUtils.generateSectionKey();
                byte[] encryptedKey = CryptoUtils.encryptWithPublicKey(sectionKeyBytes, pubKey);
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
     * Trata o comando ADD, incluindo a troca de chaves de seccao quando necessario.
     * Suporta permissoes de seccao unica e "all" (6 seccoes).
     */
    private void handleAddCommand(String[] parts, String input, ObjectOutputStream outStream,
            ObjectInputStream inStream) {
        if (parts.length != 4) {
            System.out.println("Formato incorreto. Tente: ADD <user> <hm> <s>");
            return;
        }

        try {
            sendCommand(input, outStream);

            // Primeiro sinal: "SEND-KEY" (troca de chave) ou codigo de erro
            String response = (String) inStream.readObject();

            if (!"SEND-KEY".equals(response)) {
                System.out.println("Server: " + response);
                return;
            }

            // Loop para cada seccao (1 seccao especifica ou 6 para "all")
            while ("SEND-KEY".equals(response)) {
                // Receber chave de seccao cifrada com chave publica do owner
                byte[] encryptedSectionKey = (byte[]) inStream.readObject();
                byte[] sectionKey = CryptoUtils.decryptWithPrivateKey(
                        encryptedSectionKey, CryptoUtils.loadPrivateKey(keystorePath, keystorePass));

                // Receber marcador de certificado
                response = (String) inStream.readObject();
                if (!"SEND-CERT".equals(response)) {
                    System.out.println("Erro: esperado SEND-CERT, recebido: " + response);
                    return;
                }

                // Receber certificado do target user como byte[] serializado
                byte[] certBytes = (byte[]) inStream.readObject();
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                Certificate targetCert = cf.generateCertificate(new ByteArrayInputStream(certBytes));
                PublicKey targetPubKey = targetCert.getPublicKey();

                // Cifrar chave de seccao com chave publica do target user
                byte[] encryptedKey = CryptoUtils.encryptWithPublicKey(sectionKey, targetPubKey);

                // Enviar chave cifrada de volta ao servidor
                outStream.writeObject(encryptedKey);
                outStream.flush();

                // Ler proximo sinal: "SEND-KEY" para proxima seccao ou "OK" final (ou erro)
                response = (String) inStream.readObject();
            }

            System.out.println("Server: " + response);

        } catch (Exception e) {
            System.out.println("Erro ao processar comando ADD: " + e.getMessage());
        }
    }

    /**
     * Envia uma linha de comando completa para o servidor.
     */
    private void sendCommand(String input, ObjectOutputStream outStream) throws Exception {
        outStream.writeObject(input);
        outStream.flush();
    }
}