package SpertaClient.src.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.security.KeyStore;
import java.util.Scanner;
import java.security.cert.Certificate;
import java.security.MessageDigest;

/**
 * Trata a attestation remota e a autenticacao do utilizador no servidor.
 */
public class AuthHandler {

    /**
     * Executa attestation, verificacao de sessao online e autenticacao do
     * utilizador.
     *
     * @param codeSourceClass classe usada para localizar o artefacto do cliente
     * @param user            nome do utilizador
     * @param pass            password inicial
     * @param sc              scanner associado ao terminal
     * @param outStream       stream de saida para o servidor
     * @param inStream        stream de entrada vindo do servidor
     * @return true se a autenticacao for concluida com sucesso; false caso
     *         contrario
     */
    public boolean authenticate(Class<?> codeSourceClass, String user, String pass, Scanner sc,
            ObjectOutputStream outStream, ObjectInputStream inStream) {
        try {
            if (!performAttestation(codeSourceClass, outStream, inStream)) {
                System.out.println("ATTESTATION FAILED");
                return false;
            }

            System.out.println("ATTESTATION OK");
            outStream.writeObject(user);
            outStream.flush();

            String userOn = (String) inStream.readObject();
            if (userOn.equals("USERON")) {
                System.out.println("USER ALREADY ONLINE");
                return false;
            }
            if (!"OK".equals(userOn)) {
                System.out.println("Login rejected by server.");
                return false;
            }

            return performLogin(user, pass, sc, outStream, inStream);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Envia ao servidor o hash SHA-256(nonce_bytes || bytes_JAR) para atestacao
     * remota com protecao anti-replay.
     */
    private boolean performAttestation(Class<?> codeSourceClass, ObjectOutputStream outStream,
            ObjectInputStream inStream) throws Exception {

        // Receber nonce do servidor
        long nonce = inStream.readLong();
        byte[] nonceBytes = ByteBuffer.allocate(8).putLong(nonce).array();

        // Localizar o proprio JAR
        File clientFile = new File(codeSourceClass.getProtectionDomain().getCodeSource().getLocation().toURI());

        // Calcular SHA-256(nonce_bytes || bytes_JAR)
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(nonceBytes);
        try (FileInputStream fis = new FileInputStream(clientFile)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                md.update(buffer, 0, bytesRead);
            }
        }

        // Converter para hash para string
        StringBuilder hexString = new StringBuilder();
        for (byte b : md.digest()) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1)
                hexString.append('0');
            hexString.append(hex);
        }
        String myHash = hexString.toString().toUpperCase();

        // Enviar hash ao servidor
        outStream.writeObject(myHash);
        outStream.flush();

        String answer = (String) inStream.readObject();
        return !answer.equals("NOK-ATTEST");
    }

    /**
     * Gere ate tres tentativas de autenticacao por password.
     */
    private boolean performLogin(String user, String pass, Scanner sc, ObjectOutputStream outStream,
            ObjectInputStream inStream) throws Exception {
        boolean success = false;
        boolean firstTry = true;
        int tries = 1;
        String currentPass = pass;

        while (!success && tries <= 3) {
            if (!firstTry) {
                System.out.println("Tentativa " + tries + "/3:");
                System.out.print("Password incorreta! Digite nova password para o user '" + user + "': ");
                currentPass = sc.nextLine();
            }

            outStream.writeObject(currentPass);
            outStream.flush();

            String respostaAuth;
            try {
                respostaAuth = (String) inStream.readObject();
            } catch (Exception e) {
                System.out.println("O servidor encerrou a ligação por excesso de tentativas.");
                break;
            }

            if (respostaAuth.equals("SEND-CERT")) {
                sendCertificate(user, outStream);// Envia o certificado para o server

                respostaAuth = (String) inStream.readObject();
            }

            System.out.println(respostaAuth);
            if (respostaAuth.equals("USERON")) {
                System.out.println("USER ALREADY ONLINE");
                break;
            }

            if (respostaAuth.equals("OK-NEW-USER") || respostaAuth.equals("OK-USER")) {
                success = true;
            }

            firstTry = false;
            tries++;
        }

        if (!success) {
            System.out.println("Tentativas esgotadas! A encerrar...");
        }

        return success;
    }

    /**
     * Extrai o certificado público da Keystore do Cliente e envia os bytes para o
     * Servidor.
     */
    private void sendCertificate(String user, ObjectOutputStream outStream) throws Exception {
        String keystorePath = System.getProperty("javax.net.ssl.keyStore");
        String keystorePass = System.getProperty("javax.net.ssl.keyStorePassword");

        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream(keystorePath)) {
            ks.load(fis, keystorePass.toCharArray());
        }

        // Usar o primeiro alias disponivel na keystore
        String alias = ks.aliases().nextElement();
        Certificate cert = ks.getCertificate(alias);
        if (cert == null) {
            throw new Exception("Certificado nao encontrado na keystore!");
        }

        byte[] certBytes = cert.getEncoded();

        outStream.writeLong(certBytes.length);
        outStream.flush();
        outStream.write(certBytes);
        outStream.flush();
    }
}
