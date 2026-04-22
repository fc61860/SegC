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
     * Trata o comando EC, cifrando o valor e enviando-o para o servidor.
     * Protocolo:
     * a) Envia EC <hm> <d> ao servidor (notificacao, sem o valor).
     * b) Recebe a chave de seccao cifrada com a chave publica do utilizador.
     * c) Decifra a chave, cifra o valor com AES e envia o valor cifrado.
     */
    private void handleEcCommand(String[] parts, String input, ObjectOutputStream outStream,
            ObjectInputStream inStream) {
        if (parts.length != 4) {
            System.out.println("Formato incorreto. Tente: EC <hm> <d> <int>");
            return;
        }

        try {
            // Passo a: Enviar notificacao EC ao servidor (sem o valor)
            outStream.writeObject("EC " + parts[1] + " " + parts[2]);
            outStream.flush();

            // Passo b: Receber chave de seccao cifrada ou mensagem de erro
            Object response = inStream.readObject();
            if (response instanceof String) {
                System.out.println("Server: " + response);
                return;
            }

            byte[] encryptedSectionKey = (byte[]) response;

            // Passo c: Decifrar a chave de seccao e cifrar o valor para envio
            java.security.PrivateKey privateKey = CryptoUtils.loadPrivateKey(keystorePath, keystorePass);
            byte[] sectionKeyBytes = CryptoUtils.decryptWithPrivateKey(encryptedSectionKey, privateKey);
            SecretKey sectionKey = new SecretKeySpec(sectionKeyBytes, "AES");
            byte[] encryptedValue = CryptoUtils.encryptWithAES(
                    parts[3].getBytes(java.nio.charset.StandardCharsets.UTF_8), sectionKey);
            String base64EncryptedValue = java.util.Base64.getEncoder().encodeToString(encryptedValue);

            outStream.writeObject(base64EncryptedValue);
            outStream.flush();

            String answer = (String) inStream.readObject();
            System.out.println("Server: " + answer);

        } catch (Exception e) {
            System.out.println("Erro ao comunicar com o servidor durante EC.");
        }
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
            String tempFicheiro = "temp_summary.txt";
            File summaryFile = fileTransferManager.processFile(inStream, tempFicheiro);
            if (summaryFile == null)
                return;

            int numKeys = inStream.readInt();
            java.util.Map<String, javax.crypto.SecretKey> keysMap = new java.util.HashMap<>();
            java.security.PrivateKey privateKey = CryptoUtils.loadPrivateKey(keystorePath, keystorePass);

            for (int i = 0; i < numKeys; i++) {
                String sec = (String) inStream.readObject();
                int len = inStream.readInt();
                byte[] encryptedKey = new byte[len];
                inStream.readFully(encryptedKey);

                byte[] decKey = CryptoUtils.decryptWithPrivateKey(encryptedKey, privateKey);
                keysMap.put(sec, new SecretKeySpec(decKey, "AES"));
            }

            // Ler resumo, decifrar valores e guardar
            java.util.List<String> linhas = Files.readAllLines(summaryFile.toPath(),
                    java.nio.charset.StandardCharsets.UTF_8);
            java.util.List<String> linhasDecifradas = new java.util.ArrayList<>();
            System.out.println("Resumo de Leituras:");

            for (String linha : linhas) {
                if (linha.trim().isEmpty())
                    continue;
                String[] parts1 = linha.split(":", 2);
                if (parts1.length == 2) {
                    String device = parts1[0].trim();
                    String rest = parts1[1].trim();
                    String[] parts2 = rest.split(",", 2);
                    if (parts2.length == 2) {
                        String date = parts2[0].trim();
                        String b64 = parts2[1].trim();

                        String sec = device.substring(0, 1);
                        javax.crypto.SecretKey sk = keysMap.get(sec);
                        if (sk != null) {
                            try {
                                byte[] encVal = java.util.Base64.getDecoder().decode(b64);
                                byte[] decVal = CryptoUtils.decryptFile(encVal, sk);
                                String valStr = new String(decVal, java.nio.charset.StandardCharsets.UTF_8);
                                String decryptedLine = device + ": " + date + ", " + valStr;
                                System.out.println(decryptedLine);
                                linhasDecifradas.add(decryptedLine);
                            } catch (Exception ex) {
                                String errorLine = device + ": " + date + ", [Erro ao decifrar]";
                                System.out.println(errorLine);
                                linhasDecifradas.add(errorLine);
                            }
                        } else {
                            String noKeyLine = device + ": " + date + ", [Sem chave para " + sec + "]";
                            System.out.println(noKeyLine);
                            linhasDecifradas.add(noKeyLine);
                        }
                    } else {
                        System.out.println(linha);
                        linhasDecifradas.add(linha);
                    }
                } else {
                    System.out.println(linha);
                    linhasDecifradas.add(linha);
                }
            }

            String nomeFicheiro = "client_summary_" + parts[1] + ".txt";
            String outputPath = "SpertaClient/data/" + nomeFicheiro;
            Files.write(Paths.get(outputPath), linhasDecifradas, java.nio.charset.StandardCharsets.UTF_8);
            System.out.println("Resumo decifrado e guardado em: " + outputPath);

            Files.deleteIfExists(summaryFile.toPath());
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
            if (logFile == null) {
                return;
            }

            File keyFile = fileTransferManager.processKeyFile(inStream, "temp_key.bin");
            if (keyFile == null) {
                return;
            }

            byte[] encryptedKey = Files.readAllBytes(keyFile.toPath());

            // Carregar a chave de seccao
            PrivateKey privateKey = CryptoUtils.loadPrivateKey(keystorePath, keystorePass);
            byte[] sectionKeyBytes = CryptoUtils.decryptWithPrivateKey(encryptedKey, privateKey);
            SecretKey sectionKey = new SecretKeySpec(sectionKeyBytes, "AES");

            // Processar o ficheiro linha a linha
            java.util.List<String> linhas = Files.readAllLines(logFile.toPath(),
                    java.nio.charset.StandardCharsets.UTF_8);
            java.util.List<String> linhasDecifradas = new java.util.ArrayList<>();

            for (String linha : linhas) {
                if (linha.trim().isEmpty())
                    continue;
                String[] split = linha.split(",", 2);
                if (split.length == 2) {
                    String timestamp = split[0].trim();
                    String b64 = split[1].trim();
                    try {
                        byte[] encryptedVal = java.util.Base64.getDecoder().decode(b64);
                        byte[] decryptedVal = CryptoUtils.decryptFile(encryptedVal, sectionKey);
                        String valueStr = new String(decryptedVal, java.nio.charset.StandardCharsets.UTF_8);
                        linhasDecifradas.add(timestamp + ", " + valueStr);
                    } catch (Exception ex) {
                        linhasDecifradas.add(timestamp + ", [Erro ao decifrar]");
                    }
                } else {
                    linhasDecifradas.add(linha);
                }
            }

            String outputPath = "SpertaClient/data/" + nomeFicheiro;
            Files.write(Paths.get(outputPath), linhasDecifradas, java.nio.charset.StandardCharsets.UTF_8);
            System.out.println("Historico decifrado e guardado em: " + outputPath);

            // apagar os ficheiros temporarios
            try {
                Files.deleteIfExists(logFile.toPath());
                Files.deleteIfExists(keyFile.toPath());
            } catch (IOException e) {
                System.out.println("Warning: could not delete temp files: " + e.getMessage());
            }

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
     * 4. Loop por seccoes: recebe chave cifrada, re-cifra com pub key do target,
     * envia.
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
                byte[] sectionKeyBytes = CryptoUtils.decryptWithPrivateKey(
                        encryptedSectionKey, CryptoUtils.loadPrivateKey(keystorePath, keystorePass));

                PublicKey targetPubKey = CryptoUtils.loadPublicKeyFromTruststore(
                        truststorePath, truststorePass, targetUser);
                byte[] encryptedKey = CryptoUtils.encryptWithPublicKey(sectionKeyBytes, targetPubKey);

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