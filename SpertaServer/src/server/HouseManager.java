import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Gere a persistencia e a consulta de informacao associada a casas.
 */
public class HouseManager {
    private static final String FICHEIRO_CASAS = "SpertaServer/data/casas.txt";

    /**
     * Cria uma nova casa com o utilizador indicado como proprietario.
     *
     * @param user      utilizador que fica como dono da casa
     * @param houseName nome da casa a criar
     * @return OK quando a casa e criada; NOK quando o nome e invalido ou ja existe
     * @throws IOException se ocorrer um erro ao persistir a nova casa
     */
    public String criarCasa(String user, String houseName) throws IOException {
        if (houseName == null || houseName.contains(";")) {
            return "NOK";
        }

        if (findHouseLine(houseName) != null) {
            return "NOK";
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FICHEIRO_CASAS, true))) {
            writer.write(houseName + ";" + user + ";;");
            writer.newLine();
        }

        return "OK";
    }

    /**
     * Procura a linha persistida correspondente a uma casa.
     *
     * @param houseName nome da casa a procurar
     * @return linha completa da casa ou null se a casa nao existir
     * @throws IOException se ocorrer um erro ao ler o ficheiro de casas
     */
    public String findHouseLine(String houseName) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(FICHEIRO_CASAS))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(";");
                if (parts[0].equals(houseName)) {
                    return line;
                }
            }
        }

        return null;
    }

    /**
     * Verifica se um utilizador e o proprietario da casa representada pela linha.
     *
     * @param line     linha da casa no formato persistido
     * @param username utilizador a validar
     * @return true se o utilizador for o proprietario; false caso contrario
     */
    public boolean isOwner(String line, String username) {
        String[] parts = line.split(";");
        return parts[1].trim().equals(username);
    }

    /**
     * Verifica se um utilizador pertence a uma casa, como proprietario ou como
     * utilizador com permissoes atribuidas.
     *
     * @param line     linha da casa no formato persistido
     * @param username utilizador a procurar
     * @return true se o utilizador estiver associado a casa; false caso contrario
     */
    public boolean userExistsInHouse(String line, String username) {
        if (isOwner(line, username)) {
            return true;
        }

        String[] parts = line.split(";", -1);
        for (String userPerm : parts[2].trim().split(",")) {
            if (userPerm.trim().isEmpty()) {
                continue;
            }

            String[] userParts = userPerm.split(":");
            if (userParts.length > 0 && userParts[0].trim().equals(username)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Devolve o ficheiro que persiste a informacao das casas.
     *
     * @return ficheiro de casas
     */
    public File getCasasFile() {
        return new File(FICHEIRO_CASAS);
    }
}
