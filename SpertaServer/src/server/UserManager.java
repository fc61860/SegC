import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.nio.charset.StandardCharsets;

/**
 * Gere a autenticacao de utilizadores e o registo de sessoes online.
 */
public class UserManager {
    private static final int MAX_TENTATIVAS = 3;
    private static final String FICHEIRO_USERS = "SpertaServer/data/users.txt";
    private static final String FICHEIRO_CLIENTS_ONLINE = "SpertaServer/data/online_users.txt";
    private final Object userFileLock = new Object();

    /**
     * Autentica um utilizador existente ou regista um novo utilizador quando o
     * username ainda nao existe.
     *
     * @param user      username enviado pelo cliente
     * @param inStream  stream de entrada da ligacao com o cliente
     * @param outStream stream de saida da ligacao com o cliente
     * @return "OK-NEW-USER" quando o utilizador e criado, "OK-USER" quando a
     *         autenticacao de um utilizador existente termina com sucesso, ou
     *         null quando o cliente excede o numero maximo de tentativas
     * @throws IOException            se ocorrer um erro de I/O ao ler ou escrever
     *                                nos
     *                                streams ou ficheiros
     * @throws ClassNotFoundException se o objeto recebido do stream nao for
     *                                reconhecido
     */
    public String autenticarCliente(String user, ObjectInputStream inStream, ObjectOutputStream outStream)
            throws IOException, ClassNotFoundException {
        int tentativas = 0;
        String passwd;
        if (!SpertaServer.checkIntegrity(FICHEIRO_USERS)) {
            System.err.println("NOK-INTEGRITY");
            System.exit(-1);
        }
        File file = new File(FICHEIRO_USERS);
        String[] userData = findUserData(file, user); 

        if (userData == null) {
            passwd = (String) inStream.readObject();
            String existingHash = registerUserIfAbsent(file, user, passwd);

            if (existingHash == null) {
                outStream.writeObject("SEND-CERT");
                outStream.flush();

                long certSize = inStream.readLong();
                byte[] certBytes = new byte[(int) certSize];
                inStream.readFully(certBytes);

                File certFile = new File("SpertaServer/data/" + user + ".cer");
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(certFile)) {
                    fos.write(certBytes);
                }

                return "OK-NEW-USER";
            } else {
                userData = findUserData(file, user);
            }
        }

        String hashGuardado = userData[0];
        String saltGuardado = userData[1];

        while (tentativas < MAX_TENTATIVAS) {
            passwd = (String) inStream.readObject();
            
            String hashCalculado = calculateHashPass(passwd, saltGuardado);

            if (hashGuardado.equals(hashCalculado)) {
                return "OK-USER";
            }

            tentativas++;
            outStream.writeObject("WRONG-PWD-" + tentativas);
            outStream.flush();
        }

        return null;
    }

    /**
     * Verifica se um utilizador existe no ficheiro persistente de utilizadores.
     *
     * @param user username a procurar
     * @return true se o utilizador existir; false caso contrario
     * @throws FileNotFoundException se o ficheiro de utilizadores nao estiver
     *                               disponivel
     */
    public boolean userExists(String user) throws FileNotFoundException {
        if (!SpertaServer.checkIntegrity(FICHEIRO_USERS)) {
            System.err.println("NOK-INTEGRITY");
            System.exit(-1);
        }
        File file = new File(FICHEIRO_USERS);
        return findUserPassword(file, user) != null;
    }

    /**
     * Marca um utilizador como online, desde que ainda nao tenha uma sessao ativa.
     *
     * @param username utilizador a registar como online
     * @return true se o utilizador ficou online; false se ja estava online
     * @throws IOException se ocorrer um erro ao atualizar o ficheiro de sessoes
     */
    public synchronized boolean loginUser(String username) throws IOException {
        if (isUserOnline(username)) {
            return false;
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FICHEIRO_CLIENTS_ONLINE, true))) {
            writer.write(username);
            writer.newLine();
        }
        return true;
    }

    /**
     * Exponibiliza a verificacao do estado online de um utilizador.
     *
     * @param username utilizador a verificar
     * @return true se o utilizador estiver online; false caso contrario
     * @throws IOException se ocorrer um erro ao ler o ficheiro de sessoes
     */
    public synchronized boolean isUserOnlinePublic(String username) throws IOException {
        return isUserOnline(username);
    }

    /**
     * Remove um utilizador do ficheiro de sessoes online.
     *
     * @param username utilizador a remover
     * @throws IOException se ocorrer um erro ao reescrever o ficheiro de sessoes
     */
    public synchronized void logoutUser(String username) throws IOException {
        File file = new File(FICHEIRO_CLIENTS_ONLINE);
        List<String> users = new ArrayList<>();

        try (Scanner sc = new Scanner(file)) {
            while (sc.hasNextLine()) {
                String currentUser = sc.nextLine().trim();
                if (!currentUser.equals(username)) {
                    users.add(currentUser);
                }
            }
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            for (String currentUser : users) {
                writer.write(currentUser);
                writer.newLine();
            }
        }
    }

    /**
     * Verifica se um utilizador ja se encontra registado como online.
     *
     * @param username utilizador a procurar
     * @return true se o utilizador existir no ficheiro de sessoes; false caso
     *         contrario
     * @throws IOException se ocorrer um erro ao ler o ficheiro de sessoes online
     */
    private boolean isUserOnline(String username) throws IOException {
        File file = new File(FICHEIRO_CLIENTS_ONLINE);
        try (Scanner sc = new Scanner(file)) {
            while (sc.hasNextLine()) {
                if (sc.nextLine().trim().equals(username)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Garante que o ficheiro persistente de utilizadores existe antes de qualquer
     * leitura ou escrita.
     *
     * @return referencia para o ficheiro de utilizadores
     * @throws IOException se nao for possivel criar o ficheiro quando este ainda
     *                     nao existe
     */
    // private File ensureUsersFileExists() throws IOException {
    // File file = new File(FICHEIRO_USERS);
    // if (!file.exists()) {
    // file.createNewFile();
    // }
    // return file;
    // }

    /**
     * Procura a password atualmente associada a um utilizador no ficheiro
     * persistente, protegendo a leitura com o lock de registo para evitar corridas
     * com criacoes concorrentes.
     *
     * @param file ficheiro de utilizadores a consultar
     * @param user username a procurar
     * @return password associada ao utilizador, ou null se o utilizador ainda nao
     *         existir
     * @throws FileNotFoundException se o ficheiro indicado nao estiver disponivel
     */
    private String findUserPassword(File file, String user) throws FileNotFoundException {
        synchronized (userFileLock) {
            try (Scanner sc = new Scanner(file)) {
                while (sc.hasNextLine()) {
                    String[] parts = sc.nextLine().split(":", 2);
                    if (parts.length == 2 && parts[0].equals(user)) {
                        return parts[1];
                    }
                }
            }
            return null;
        }
    }

    /**
     * Procura os dados de autenticação de um utilizador.
     * @return Um array com [Hash_Guardado, Salt_Guardado], ou null se não existir.
     */
    private String[] findUserData(File file, String user) throws FileNotFoundException {
        synchronized (userFileLock) {
            try (Scanner sc = new Scanner(file)) {
                while (sc.hasNextLine()) {
                    // Agora dividimos por 3 partes: user:hash:salt
                    String[] parts = sc.nextLine().split(":", 3); 
                    if (parts.length == 3 && parts[0].equals(user)) {
                        return new String[]{parts[1], parts[2]};
                    }
                }
            }
            return null;
        }
    }

    /**
     * Regista um novo utilizador apenas se este continuar ausente no momento da
     * escrita, evitando duplicados quando existem pedidos concorrentes para o
     * mesmo username.
     *
     * @param file     ficheiro de utilizadores a atualizar
     * @param user     username a registar
     * @param password password a persistir caso o utilizador seja novo
     * @return null quando o utilizador foi criado neste pedido, ou a password ja
     *         existente quando outro thread o registou primeiro
     * @throws IOException se ocorrer um erro ao ler ou atualizar o ficheiro de
     *                     utilizadores
     */
    private String registerUserIfAbsent(File file, String user, String password) throws IOException {
        synchronized (userFileLock) {
            try (Scanner sc = new Scanner(file)) {
                while (sc.hasNextLine()) {
                    String[] parts = sc.nextLine().split(":", 3); 
                    if (parts.length == 3 && parts[0].equals(user)) {
                        return parts[1];
                    }
                }
            }

            String saltAleatorio = genSalt();
            String hashPassword = calculateHashPass(password, saltAleatorio);

            try (FileWriter fw = new FileWriter(file, true)) {
                fw.write(user + ":" + hashPassword + ":" + saltAleatorio + "\n");
            }

            try {
                SpertaServer.saveHashFile(file.getPath());
            } catch (Exception e) {
                System.err.println("Erro ao assinar novo user.");
            }

            return null;
        }
    }

    /**
     * Gera um Salt aleatório usando SecureRandom.
     */
    private String genSalt() {
        SecureRandom sr = new SecureRandom();
        byte[] saltBytes = new byte[16];
        sr.nextBytes(saltBytes);
        return Base64.getEncoder().encodeToString(saltBytes);
    }

    /**
     * Calcula o Hash SHA-256 da concatenação da Password com o Salt.
     */
    private String calculateHashPass(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            // Concatena a password e o salt (password || salt)
            String textoParaHash = password + salt; 
            byte[] hashBytes = md.digest(textoParaHash.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao calcular hash da password", e);
        }
    }
}
