package SpertaClient.src.client;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * Processa comandos introduzidos pelo utilizador e trata a comunicacao com o
 * servidor.
 */
public class CommandHandler {
    private final FileTransferManager fileTransferManager;
    private final String keystorePath;
    private final String keystorePass;
    private final String truststorePath;
    private final String truststorePass;

    /**
     * Cria um processador de comandos com suporte a transferencia de ficheiros.
     *
     * @param fileTransferManager componente responsavel por guardar ficheiros
     *                            recebidos do servidor
     * @param keystorePath        caminho para a keystore do cliente (PKCS12)
     * @param keystorePass        password da keystore
     * @param truststorePath      caminho para a truststore do cliente (JKS)
     * @param truststorePass      password da truststore
     */
    public CommandHandler(FileTransferManager fileTransferManager, String keystorePath, String keystorePass,
            String truststorePath, String truststorePass) {
        this.fileTransferManager = fileTransferManager;
        this.keystorePath = keystorePath;
        this.keystorePass = keystorePass;
        this.truststorePath = truststorePath;
        this.truststorePass = truststorePass;
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
            File logFile = fileTransferManager.processFile(inStream, "temp_log.csv");
            File keyFile = fileTransferManager.processKeyFile(inStream, "temp_key.bin");

            byte[] encryptedFile = Files.readAllBytes(logFile.toPath());
            byte[] encryptedKey = Files.readAllBytes(keyFile.toPath());

            // apagar os ficheiros
            try {
                Files.deleteIfExists(logFile.toPath());
                Files.deleteIfExists(keyFile.toPath());
            } catch (IOException e) {
                System.out.println("Warning: could not delete temp files: " + e.getMessage());
            }
            System.out.println(encryptedFile.length);
            // nao sei se devia tar aqui
            PrivateKey privateKey = CryptoUtils.loadPrivateKey(keystorePath, keystorePass);
            byte[] aesKeyBytes = CryptoUtils.decryptWithPrivateKey(encryptedKey, privateKey);
            SecretKey aesKey = new SecretKeySpec(aesKeyBytes, "AES");
            System.out.println(aesKeyBytes.length);
            byte[] plaintext = CryptoUtils.decryptFile(encryptedFile, aesKey);

            Files.write(Paths.get("decrypted_" + nomeFicheiro), plaintext);
        } catch (Exception e) {
            System.out.println("Erro ao comunicar com o servidor.");
            e.printStackTrace();
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
            // so um dos prints e fica o final
            // System.out.println("Server: " + response);

            if (!"OK".equals(response)) {
                return;
            }

            // Carregar chave publica RSA da keystore
            PublicKey pubKey = CryptoUtils.loadPublicKey(keystorePath, keystorePass);

            // Gerar e enviar chave AES-128 para cada seccao, cifrada com RSA
            String[] sections = { "E", "G", "L", "M", "P", "S" };
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
     * Trata o comando ADD:
     * 1. Servidor valida e responde OK ou erro.
     * 2. Cliente verifica truststore -> envia NEED-CERT ou HAVE-CERT.
     * 3. Se NEED-CERT: recebe certificado do servidor e guarda na truststore.
     * 4. Loop por seccoes: recebe chave cifrada, re-cifra com pub key do target, envia.
     */
    private void handleAddCommand(String[] parts, String input, ObjectOutputStream outStream,
            ObjectInputStream inStream) {
        if (parts.length != 4) {
            System.out.println("Formato incorreto. Tente: ADD <user> <hm> <s>");
            return;
        }

        String targetUser = parts[1];

        try {
            sendCommand(input, outStream);

            // Passo 1: receber resultado da validacao de permissoes
            String response = (String) inStream.readObject();
            if (!"OK".equals(response)) {
                System.out.println("Server: " + response);
                return;
            }

            // Passo 2: verificar se temos o certificado do target na truststore
            boolean hasCert = CryptoUtils.hasCertInTruststore(truststorePath, truststorePass, targetUser);
            if (!hasCert) {
                outStream.writeObject("NEED-CERT");
                outStream.flush();

                // Passo 3: receber certificado e guardar na truststore
                Object certResponse = inStream.readObject();
                if (certResponse instanceof String) {
                    System.out.println("Server: " + certResponse);
                    return;
                }
                byte[] certBytes = (byte[]) certResponse;
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                Certificate cert = cf.generateCertificate(new ByteArrayInputStream(certBytes));
                CryptoUtils.saveCertToTruststore(truststorePath, truststorePass, targetUser, cert);
            } else {
                outStream.writeObject("HAVE-CERT");
                outStream.flush();
            }

            // Passo 4: loop de troca de chaves de seccao
            response = (String) inStream.readObject();
            while ("SEND-KEY".equals(response)) {
                byte[] encryptedSectionKey = (byte[]) inStream.readObject();
                byte[] sectionKey = CryptoUtils.decryptWithPrivateKey(
                        encryptedSectionKey, CryptoUtils.loadPrivateKey(keystorePath, keystorePass));

                PublicKey targetPubKey = CryptoUtils.loadPublicKeyFromTruststore(
                        truststorePath, truststorePass, targetUser);
                byte[] encryptedKey = CryptoUtils.encryptWithPublicKey(sectionKey, targetPubKey);

                outStream.writeObject(encryptedKey);
                outStream.flush();

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