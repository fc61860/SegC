package SpertaClient.src.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.Certificate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * Testa a capacidade do servidor de lidar com múltiplos clientes simultâneos.
 *
 * Uso:
 * java -cp SpertaClient/bin/classes SpertaClient.src.client.LoginLoadTester
 * <serverAddress> <baseUser> <password> <count> <holdSeconds> <sameUser>
 * <truststore> <truststorePass> <keystore> <keystorePass> <jarPath>
 */
public class LoginLoadTester {

    public static void main(String[] args) throws Exception {
        if (args.length != 11) {
            System.err.println("Uso: LoginLoadTester <serverAddress> <baseUser> <password> <count>" +
                    " <holdSeconds> <sameUser> <truststore> <truststorePass> <keystore> <keystorePass> <jarPath>");
            System.exit(1);
        }

        String serverAddress = args[0];
        String baseUser = args[1];
        String password = args[2];
        int count = Integer.parseInt(args[3]);
        int holdSeconds = Integer.parseInt(args[4]);
        boolean sameUser = Boolean.parseBoolean(args[5]);
        String truststore = args[6];
        String truststorePass = args[7];
        String keystore = args[8];
        String keystorePass = args[9];
        String jarPath = args[10];

        String[] addressParts = serverAddress.split(":");
        String ip = addressParts[0];
        int port = addressParts.length > 1 ? Integer.parseInt(addressParts[1]) : 22345;

        // Configurar SSL globalmente (todas as threads partilham estas propriedades)
        System.setProperty("javax.net.ssl.trustStore", truststore);
        System.setProperty("javax.net.ssl.trustStorePassword", truststorePass);
        System.setProperty("javax.net.ssl.trustStoreType", "JKS");
        System.setProperty("javax.net.ssl.keyStore", keystore);
        System.setProperty("javax.net.ssl.keyStorePassword", keystorePass);
        System.setProperty("javax.net.ssl.keyStoreType", "PKCS12");

        File jarFile = new File(jarPath);
        if (!jarFile.exists()) {
            System.err.println("ERRO: JAR nao encontrado em: " + jarPath);
            System.exit(1);
        }

        AtomicInteger successes = new AtomicInteger(0);
        AtomicInteger failures = new AtomicInteger(0);

        // Latch para disparar todas as threads ao mesmo tempo
        CountDownLatch startGun = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(count);

        System.out.printf("A lançar %d clientes simultâneos...%n", count);

        for (int i = 0; i < count; i++) {
            final String user = sameUser ? baseUser : baseUser + "_" + i;
            final int idx = i;

            Thread t = new Thread(() -> {
                try {
                    startGun.await(); // espera que todos estejam prontos
                    boolean ok = doLoginSession(ip, port, user, password, holdSeconds, jarFile);
                    if (ok) {
                        successes.incrementAndGet();
                        System.out.printf("[%3d] %-20s -> OK%n", idx, user);
                    } else {
                        failures.incrementAndGet();
                        System.out.printf("[%3d] %-20s -> FALHOU%n", idx, user);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
            t.setDaemon(true);
            t.start();
        }

        long start = System.currentTimeMillis();
        startGun.countDown(); // dispara todas ao mesmo tempo
        doneLatch.await();
        long elapsed = System.currentTimeMillis() - start;

        System.out.println("=================================================");
        System.out.printf("Clientes: %d  |  OK: %d  |  Falhou: %d  |  Tempo: %dms%n",
                count, successes.get(), failures.get(), elapsed);
        System.out.println("=================================================");
    }

    /**
     * Executa uma sessão completa: TLS handshake, attestation, login,
     * aguarda holdSeconds e desliga.
     */
    private static boolean doLoginSession(String ip, int port, String user, String pass,
            int holdSeconds, File jarFile) {
        SSLSocketFactory sslsf = (SSLSocketFactory) SSLSocketFactory.getDefault();
        try (SSLSocket socket = (SSLSocket) sslsf.createSocket(ip, port);
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            socket.startHandshake();

            // --- Attestation ---
            long nonce = in.readLong();
            byte[] nonceBytes = ByteBuffer.allocate(8).putLong(nonce).array();

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(nonceBytes);
            try (FileInputStream fis = new FileInputStream(jarFile)) {
                byte[] buf = new byte[4096];
                int n;
                while ((n = fis.read(buf)) != -1)
                    md.update(buf, 0, n);
            }
            StringBuilder hex = new StringBuilder();
            for (byte b : md.digest()) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1)
                    hex.append('0');
                hex.append(h);
            }
            out.writeObject(hex.toString().toUpperCase());
            out.flush();

            String attestResult = (String) in.readObject();
            if (attestResult.equals("NOK-ATTEST"))
                return false;

            // --- Identificação do utilizador ---
            out.writeObject(user);
            out.flush();

            String userOnline = (String) in.readObject();
            if (userOnline.equals("USERON"))
                return false; // já online

            // --- Login (password) ---
            out.writeObject(pass);
            out.flush();

            String loginResponse = (String) in.readObject();

            if (loginResponse.equals("SEND-CERT")) {
                sendCertificate(out);
                loginResponse = (String) in.readObject();
            }

            boolean loggedIn = loginResponse.equals("OK-NEW-USER") || loginResponse.equals("OK-USER");
            if (!loggedIn)
                return false;

            // --- Manter sessão por holdSeconds ---
            Thread.sleep(holdSeconds * 1000L);
            return true;

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Envia o certificado da keystore para o servidor (mesmo protocolo que
     * AuthHandler).
     */
    private static void sendCertificate(ObjectOutputStream out) throws Exception {
        String keystorePath = System.getProperty("javax.net.ssl.keyStore");
        String keystorePass = System.getProperty("javax.net.ssl.keyStorePassword");

        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream(keystorePath)) {
            ks.load(fis, keystorePass.toCharArray());
        }
        String alias = ks.aliases().nextElement();
        Certificate cert = ks.getCertificate(alias);
        byte[] certBytes = cert.getEncoded();

        out.writeLong(certBytes.length);
        out.flush();
        out.write(certBytes);
        out.flush();
    }
}
