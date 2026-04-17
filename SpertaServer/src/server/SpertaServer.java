import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.SSLServerSocketFactory;

/**
 * Classe principal do servidor SpertaServer.
 * Responsavel por inicializar a infraestrutura, aceitar ligacoes de clientes e
 * delegar cada sessao a um ClientHandler.
 */
public class SpertaServer {
    private static final String FICHEIRO_USERS = "SpertaServer/data/users.txt";
    private static final String FICHEIRO_CASAS = "SpertaServer/data/casas.txt";
    private static final String FICHEIRO_ESTADOS = "SpertaServer/data/estados.txt";
    private static final String FICHEIRO_CLIENTS_ONLINE = "SpertaServer/data/online_users.txt";
    private static final String DIRETORIA_LOGS = "SpertaServer/data/logs/";

    /**
     * Metodo principal. Inicia o servidor na porta indicada nos argumentos ou na
     * porta por omissao.
     *
     * @param args argumentos de linha de comandos; o primeiro argumento pode
     *             definir a porta TCP do servidor
     */
    public static void main(String[] args) {
        System.out.println("Servidor: main");
        int port = 22345;
        if (args.length != 4) {
            System.out.println("Erro!");
            System.out.println("Formato exigido: SpertaServer <port> <password-cifra> <keystore> <password-keystore>");
            System.exit(-1);
        }
        if (args.length == 4) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("Erro: Porto tem de ser número. A usar default.");
            }
        }

        String keystorePath = args[2];
        String keystorePassword = args[3];

        System.setProperty("javax.net.ssl.keyStore", keystorePath);
        System.setProperty("javax.net.ssl.keyStorePassword", keystorePassword);

        String passwordCifra = args[1];

        byte[] salt = loadOrCreateSalt();

        CryptoManager.init(passwordCifra, salt);

        System.setProperty("javax.net.ssl.keyStoreType", "PKCS12");
        SpertaServer server = new SpertaServer();
        server.startServer(port);
    }

    /**
     * Inicia o servidor na porta especificada e fica em escuta por novas ligacoes.
     *
     * @param port porta TCP onde o servidor aceita clientes
     */
    public void startServer(int port) {
        inicializarEstrutura();
        // Verificar Integridade Logo no Arranque
        if (!checkIntegrityEncrypted(FICHEIRO_CASAS) ||
                !checkIntegrity(FICHEIRO_USERS) ||
                !checkIntegrityEncrypted(FICHEIRO_ESTADOS)) {

            System.err.println("NOK-INTEGRITY");
            System.exit(-1);
        }

        UserManager userManager = new UserManager();
        HouseManager houseManager = new HouseManager();
        DeviceManager deviceManager = new DeviceManager();
        PermissionsManager permissionsManager = new PermissionsManager();
        LogManager logManager = new LogManager();

        SSLServerSocketFactory sslssf = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();

        try (ServerSocket serverSocket = sslssf.createServerSocket(port)) {
            System.out.println("Servidor à escuta na porta: " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket, userManager, houseManager, deviceManager,
                        permissionsManager, logManager);
                clientHandler.start();
            }
        } catch (IOException e) {
            System.err.println("Erro no servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Inicializa a estrutura de diretorios e ficheiros necessarios ao funcionamento
     * do servidor.
     */
    private static void inicializarEstrutura() {
        try {
            File dataDir = new File("SpertaServer/data");
            dataDir.mkdirs();

            File logsDir = new File(DIRETORIA_LOGS);
            logsDir.mkdirs();

            // Assinar os ficheiros logo no segundo em que nascem
            if (new File(FICHEIRO_USERS).createNewFile()) {
                saveHashFile(FICHEIRO_USERS);
            }
            if (new File(FICHEIRO_CASAS).createNewFile()) {
                writeEncrypted(FICHEIRO_CASAS, new byte[0]);
            }
            if (new File(FICHEIRO_ESTADOS).createNewFile()) {
                writeEncrypted(FICHEIRO_ESTADOS, new byte[0]);
            }

            File usersOnlineFile = new File(FICHEIRO_CLIENTS_ONLINE);
            PrintWriter writer = new PrintWriter(usersOnlineFile);
            writer.close();
        } catch (IOException e) {
            System.err.println("Erro ao inicializar ficheiros: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Erro fatal ao assinar os ficheiros iniciais.");
            System.exit(-1);
        }
    }

    /**
     * Calcula o HMAC SHA-256 do ficheiro usando a password secreta do servidor.
     */
    private static String calculateHashFile(String caminhoFicheiro) throws Exception {
        byte[] fileBytes = Files.readAllBytes(Paths.get(caminhoFicheiro));
        
        String secretPassword = CryptoManager.getPassword();
        
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(secretPassword.getBytes(), "HmacSHA256");
        mac.init(secretKeySpec);
        
        byte[] hmacBytes = mac.doFinal(fileBytes);
        
        // Converter para Base64 para ser fácil de guardar no ficheiro .hash
        return Base64.getEncoder().encodeToString(hmacBytes);
    }

    /**
     * Calcula o Hash de um ficheiro e grava-o no ficheiro .hash correspondente.
     * Este método é chamado sempre que um ficheiro é modificado.
     */
    public static void saveHashFile(String caminhoFicheiro) throws Exception {
        String novoHash = calculateHashFile(caminhoFicheiro);
        BufferedWriter writer = new BufferedWriter(new FileWriter(caminhoFicheiro + ".hash"));
        writer.write(novoHash);
        writer.close();
    }

    /**
     * Verifica se o Hash atual do ficheiro bate certo com a assinatura guardada.
     */
    public static boolean checkIntegrity(String caminhoFicheiro) {
        try {
            File ficheiro = new File(caminhoFicheiro);
            File ficheiroHash = new File(caminhoFicheiro + ".hash");

            if (!ficheiro.exists()) {
                return true;
            }
            if (!ficheiroHash.exists()) {
                return false;
            }

            String hashGuardado = new String(Files.readAllBytes(Paths.get(ficheiroHash.getPath()))).trim();
            String hashAtual = calculateHashFile(caminhoFicheiro);

            return hashAtual.equals(hashGuardado);

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Calcula o HMAC-SHA256 de um array de bytes usando a password secreta do servidor.
     */
    private static String calculateHashFromBytes(byte[] content) throws Exception {
        String secretPassword = CryptoManager.getPassword();
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(secretPassword.getBytes(), "HmacSHA256");
        mac.init(secretKeySpec);
        return Base64.getEncoder().encodeToString(mac.doFinal(content));
    }

    /**
     * Cifra um array de bytes (plaintext) e guarda no ficheiro indicado,
     * guardando também o HMAC do plaintext no ficheiro .hash correspondente.
     * Usado para todos os ficheiros cifrados (casas.txt, estados.txt, logs).
     */
    public static void writeEncrypted(String path, byte[] plaintext) throws Exception {
        String hash = calculateHashFromBytes(plaintext);
        byte[] encrypted = CryptoManager.encrypt(plaintext);
        Files.write(Paths.get(path), encrypted);
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(path + ".hash"))) {
            bw.write(hash);
        }
    }

    /**
     * Lê o ficheiro cifrado indicado, decifra-o, verifica o HMAC no plaintext
     * e devolve o conteúdo em claro. Termina o servidor com NOK-INTEGRITY se
     * a verificação falhar.
     */
    public static byte[] readDecrypted(String path) throws Exception {
        File file = new File(path);
        if (!file.exists()) return new byte[0];
        byte[] data = Files.readAllBytes(file.toPath());
        if (data.length == 0) return new byte[0];
        byte[] plaintext = CryptoManager.decrypt(data);
        File hashFile = new File(path + ".hash");
        if (hashFile.exists()) {
            String stored = new String(Files.readAllBytes(hashFile.toPath()), StandardCharsets.UTF_8).trim();
            if (!calculateHashFromBytes(plaintext).equals(stored)) {
                System.err.println("NOK-INTEGRITY");
                System.exit(-1);
            }
        }
        return plaintext;
    }

    /**
     * Verifica a integridade de um ficheiro cifrado sem terminar o servidor.
     * Retorna false se a verificação falhar ou se o ficheiro .hash não existir.
     */
    public static boolean checkIntegrityEncrypted(String path) {
        try {
            File file = new File(path);
            File hashFile = new File(path + ".hash");
            if (!file.exists()) return true;
            if (!hashFile.exists()) return false;
            byte[] data = Files.readAllBytes(file.toPath());
            byte[] plaintext = data.length == 0 ? new byte[0] : CryptoManager.decrypt(data);
            String stored = new String(Files.readAllBytes(hashFile.toPath()), StandardCharsets.UTF_8).trim();
            return calculateHashFromBytes(plaintext).equals(stored);
        } catch (Exception e) {
            return false;
        }
    }

    private static byte[] loadOrCreateSalt() {
        try {
            File saltFile = new File("SpertaServer/salt.bin");

            if (saltFile.exists()) {
                return Files.readAllBytes(saltFile.toPath());
            } else {
                byte[] salt = new byte[16];
                SecureRandom random = new SecureRandom();
                random.nextBytes(salt);

                Files.write(saltFile.toPath(), salt);
                return salt;
            }

        } catch (IOException e) {
            throw new RuntimeException("Erro ao carregar/gerar salt", e);
        }
    }
}