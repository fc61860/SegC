import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Base64;

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
        // Se usar JCEKS no keytool, descomenta a linha abaixo:
        // System.setProperty("javax.net.ssl.keyStoreType", "JCEKS");

        // Guardar a password-cifra (args[1]) numa variável global/estática se for precisa noutras classes depois

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

            new File(FICHEIRO_USERS).createNewFile();
            new File(FICHEIRO_CASAS).createNewFile();
            new File(FICHEIRO_ESTADOS).createNewFile();

            File usersOnlineFile = new File(FICHEIRO_CLIENTS_ONLINE);
            PrintWriter writer = new PrintWriter(usersOnlineFile);
            writer.close();
        } catch (IOException e) {
            System.err.println("Erro ao inicializar ficheiros: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Calcula o Hash (SHA-256) de um ficheiro.
     */
    public static String calculateHashFile(String caminhoFicheiro) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        FileInputStream fis = new FileInputStream(caminhoFicheiro);

        byte[] buffer = new byte[1024];
        int bytesLidos;
        
        while ((bytesLidos = fis.read(buffer)) != -1) {
            md.update(buffer, 0, bytesLidos);
        }
        fis.close();

        byte[] hashBytes = md.digest();
        return Base64.getEncoder().encodeToString(hashBytes);
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

            if (!ficheiro.exists()) { return true; }
            if (!ficheiroHash.exists()) { return false; }

            String hashGuardado = new String(Files.readAllBytes(Paths.get(ficheiroHash.getPath()))).trim();
            String hashAtual = calculateHashFile(caminhoFicheiro);

            return hashAtual.equals(hashGuardado);

        } catch (Exception e) {
            return false;
        }
    }
}