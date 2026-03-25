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

/**
 * Gere a autenticacao de utilizadores e o registo de sessoes online.
 */
public class UserManager {
    private static final int MAX_TENTATIVAS = 3;
    private static final String FICHEIRO_USERS = "SpertaServer/data/users.txt";
    private static final String FICHEIRO_CLIENTS_ONLINE = "SpertaServer/data/online_users.txt";

    /**
     * Autentica um utilizador existente ou regista um novo utilizador quando o
     * username ainda nao existe.
     *
     * @param user      username enviado pelo cliente
     * @param inStream  stream de entrada da ligacao com o cliente
     * @param outStream stream de saida da ligacao com o cliente
     * @return true quando a autenticacao termina com sucesso; false quando o
     *         cliente excede o numero maximo de tentativas
     * @throws IOException            se ocorrer um erro de I/O ao ler ou escrever
     *                                nos
     *                                streams ou ficheiros
     * @throws ClassNotFoundException se o objeto recebido do stream nao for
     *                                reconhecido
     */
    public boolean autenticarCliente(String user, ObjectInputStream inStream, ObjectOutputStream outStream)
            throws IOException, ClassNotFoundException {
        int tentativas = 0;
        String passwd;

        File file = new File(FICHEIRO_USERS);
        if (!file.exists()) {
            file.createNewFile();
        }

        String correctPassword = null;
        boolean exists = false;
        try (Scanner sc = new Scanner(file)) {
            while (sc.hasNextLine()) {
                String line = sc.nextLine();
                String[] parts = line.split(":");
                if (parts[0].equals(user)) {
                    exists = true;
                    correctPassword = parts[1];
                    break;
                }
            }
        }

        if (!exists) {
            passwd = (String) inStream.readObject();
            try (FileWriter fw = new FileWriter(file, true)) {
                fw.write(user + ":" + passwd + "\n");
            }
            outStream.writeObject("OK-NEW-USER");
            outStream.flush();
            return true;
        }

        while (tentativas < MAX_TENTATIVAS) {
            passwd = (String) inStream.readObject();
            if (correctPassword.equals(passwd)) {
                outStream.writeObject("OK-USER");
                outStream.flush();
                return true;
            }

            tentativas++;
            outStream.writeObject("WRONG-PWD-" + tentativas);
            outStream.flush();
        }

        return false;
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
        File file = new File(FICHEIRO_USERS);
        try (Scanner sc = new Scanner(file)) {
            while (sc.hasNextLine()) {
                String[] parts = sc.nextLine().split(":");
                if (parts[0].equals(user)) {
                    return true;
                }
            }
        }
        return false;
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
}
