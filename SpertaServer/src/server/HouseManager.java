import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

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
        synchronized (StorageLocks.DATA_LOCK) {
            if (findHouseLine(houseName) != null) {
                return "NOK";
            }
            try {
                byte[] existing = SpertaServer.readDecrypted(FICHEIRO_CASAS);
                String existingContent = new String(existing, StandardCharsets.UTF_8);
                String newLine = houseName + ";" + user + ";;";
                byte[] newContent = (existingContent + newLine + "\n").getBytes(StandardCharsets.UTF_8);
                SpertaServer.writeEncrypted(FICHEIRO_CASAS, newContent);
            } catch (Exception e) {
                System.err.println("Erro ao cifrar casas.txt: " + e.getMessage());
                return "NOK";
            }
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
        synchronized (StorageLocks.DATA_LOCK) {
            try {
                byte[] content = SpertaServer.readDecrypted(FICHEIRO_CASAS);
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(new ByteArrayInputStream(content), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] parts = splitHouseLine(line);
                        if (parts[0].equals(houseName)) {
                            return line;
                        }
                    }
                }
            } catch (Exception e) {
                throw new IOException("Erro ao ler casas.txt: " + e.getMessage(), e);
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
        String[] parts = splitHouseLine(line);
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

        String[] parts = splitHouseLine(line);
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

    /**
     * Divide uma linha de casa garantindo sempre os campos esperados para nome,
     * dono, permissoes e dispositivos, mesmo quando existem registos antigos com
     * menos separadores.
     *
     * @param line linha persistida da casa
     * @return array normalizado com quatro posicoes
     */
    String[] splitHouseLine(String line) {
        String[] rawParts = line.split(";", -1);
        String[] normalizedParts = new String[] { "", "", "", "" };

        for (int i = 0; i < rawParts.length && i < normalizedParts.length; i++) {
            normalizedParts[i] = rawParts[i];
        }

        return normalizedParts;
    }
}
