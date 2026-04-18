import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Gere atribuicoes e verificacoes de permissoes sobre secoes de uma casa.
 */
public class PermissionsManager {
    private static final Set<String> VALID_PERMS = Set.of("E", "G", "L", "M", "P", "S", "all");

    /**
     * Atribui uma permissao a um utilizador numa casa, validando primeiro a
     * existencia dos intervenientes e a autorizacao do utilizador que executa o
     * pedido.
     *
     * @param userManager  gestor de utilizadores para validacao do alvo
     * @param houseManager gestor de casas para validar a casa e o proprietario
     * @param actorUser    utilizador que faz o pedido
     * @param targetUser   utilizador que recebe a permissao
     * @param houseName    casa onde a permissao e atribuida
     * @param permission   permissao a atribuir
     * @return OK quando a atribuicao e realizada; caso contrario um dos codigos de
     *         erro do protocolo
     * @throws IOException se ocorrer um erro ao atualizar a persistencia
     */
    public String adicionarUtilizador(UserManager userManager, HouseManager houseManager, String actorUser,
            String targetUser, String houseName, String permission) throws IOException {
        if (!VALID_PERMS.contains(permission)) {
            return "NOK";
        }

        if (actorUser.equals(targetUser)) {
            return "NOK";
        }

        String line = houseManager.findHouseLine(houseName);
        if (line == null) {
            return "NOHM";
        }
        if (!userManager.userExists(targetUser)) {
            return "NOUSER";
        }
        if (!houseManager.isOwner(line, actorUser)) {
            return "NOPERM";
        }

        updatePermissions(houseManager, houseName, targetUser, permission);
        return "OK";
    }

    /**
     * Verifica se um utilizador tem acesso a uma determinada secao de uma casa.
     *
     * @param houseManager gestor de casas para validar a propriedade
     * @param line         linha da casa no formato persistido
     * @param username     utilizador a verificar
     * @param place        secao pretendida
     * @return true se o utilizador tiver permissao; false caso contrario
     */
    public boolean hasPermission(HouseManager houseManager, String line, String username, String place) {
        if (houseManager.isOwner(line, username)) {
            return true;
        }

        String[] parts = houseManager.splitHouseLine(line);
        String permissions = parts[2].trim();
        for (String userPerm : permissions.split(",")) {
            if (userPerm.trim().isEmpty()) {
                continue;
            }

            String[] userParts = userPerm.split(":", -1);
            if (userParts.length < 2 || !userParts[0].trim().equals(username)) {
                continue;
            }

            String permPlaces = userParts[1].trim();
            if (permPlaces.equals("all")) {
                return true;
            }

            for (String p : permPlaces.split("\\|")) {
                if (p.trim().equals(place)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Atualiza a string de permissoes de um utilizador dentro da representacao
     * persistida da casa. Tambem cria os ficheiros de chaves de seccao necessarios
     * baseado no tipo de permissao.
     *
     * @param permissions string de permissoes atual da casa
     * @param user        utilizador a atualizar
     * @param newPerm     nova permissao a acrescentar ou substituir
     * @param houseName   nome da casa (usado para criar ficheiros de chaves)
     * @return string de permissoes ja atualizada
     */
    private String addPermission(String permissions, String user, String newPerm, String houseName) {
        if (permissions == null || permissions.trim().isEmpty()) {
            // Criar ficheiros de chaves para este utilizador
            createSectionKeyFiles(houseName, user, newPerm);
            return user + ":" + newPerm;
        }

        List<String> updatedUsers = new ArrayList<>();
        boolean userFound = false;
        for (String entry : permissions.split(",")) {
            if (entry.trim().isEmpty()) {
                continue;
            }

            String[] parts = entry.split(":", -1);
            String currentUser = parts[0].trim();
            String currentPerms = parts.length > 1 ? parts[1].trim() : "";

            if (currentUser.equals(user)) {
                userFound = true;
                if (newPerm.equals("all")) {
                    createSectionKeyFiles(houseName, user, "all");
                    updatedUsers.add(user + ":all");
                } else if (currentPerms.equals("all")) {
                    //so a nova
                    deleteKeyFilesExcept(houseName, user, newPerm);
                    updatedUsers.add(user + ":" + newPerm);
                } else {
                    boolean exists = false;
                    for (String perm : currentPerms.split("\\|")) {
                        if (perm.equals(newPerm)) {
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) {
                        currentPerms = currentPerms.isEmpty() ? newPerm : currentPerms + "|" + newPerm;
                        createSectionKeyFiles(houseName, user, newPerm);
                    }
                    updatedUsers.add(user + ":" + currentPerms);
                }
            } else {
                updatedUsers.add(entry.trim());
            }
        }

        if (!userFound) {
            createSectionKeyFiles(houseName, user, newPerm);
            updatedUsers.add(user + ":" + newPerm);
        }
        return String.join(",", updatedUsers);
    }

    /**
     * Cria ficheiros vazios de chaves de seccao baseado no tipo de permissao.
     * Se a permissao for "all", cria ficheiros para todas as 6 secoes.
     * Se for uma seccao especifica, cria ficheiro apenas para essa seccao.
     * Falhas silenciosas se os ficheiros nao puderem ser criados.
     *
     * @param houseName  nome da casa
     * @param user       utilizador
     * @param permission permissao ("all" ou seccao especifica como "E", "G", etc)
     */
    private void createSectionKeyFiles(String houseName, String user, String permission) {
        try {
            String[] sections = "all".equals(permission) ? new String[] { "E", "G", "L", "M", "P", "S" }
                    : new String[] { permission };

            for (String section : sections) {
                File keyFile = new File("SpertaServer/data/key." + houseName + "." + section + "." + user);
                if (!keyFile.exists()) {
                    // Criar ficheiro vazio que sera preenchido com a chave cifrada depois
                    Files.write(keyFile.toPath(), new byte[0]);
                }
            }
        } catch (Exception e) {
            // Silenciamente ignorar erros na criacao de ficheiros
            System.err
                    .println("Aviso: Nao foi possivel criar ficheiros de chaves para " + user + ": " + e.getMessage());
        }
    }

    /**
     * Atualiza a string de permissoes de um utilizador dentro da representacao
     * persistida da casa.
     *
     * @param houseManager   gestor de casas que fornece o ficheiro persistente
     * @param houseName      nome da casa
     * @param targetUser     utilizador cujas permissoes vao ser atualizadas
     * @param newPermissions nova permissao a aplicar
     * @throws IOException se ocorrer um erro durante a atualizacao do ficheiro
     */
    private void updatePermissions(HouseManager houseManager, String houseName, String targetUser,
            String newPermissions)
            throws IOException {
        try {
            byte[] data = SpertaServer.readDecrypted(houseManager.getCasasFile().getPath());
            String content = new String(data, StandardCharsets.UTF_8);
            String[] rawLines = content.split("\\r?\\n", -1);
            List<String> lines = new ArrayList<>();
            for (String l : rawLines) {
                if (!l.isEmpty())
                    lines.add(l);
            }

            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                String[] parts = houseManager.splitHouseLine(line);
                if (parts[0].equals(houseName)) {
                    parts[2] = addPermission(parts[2], targetUser, newPermissions, houseName);
                    lines.set(i, String.join(";", parts));
                    break;
                }
            }

            String newContent = String.join("\n", lines) + "\n";
            SpertaServer.writeEncrypted(houseManager.getCasasFile().getPath(),
                    newContent.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IOException("Erro ao atualizar permissoes: " + e.getMessage(), e);
        }
    }

    /**
     * Carrega a chave de seccao cifrada para um utilizador especifico.
     *
     * @param houseName nome da casa
     * @param section   seccao
     * @param user      utilizador
     * @return bytes da chave cifrada ou null se nao existir
     * @throws Exception se ocorrer erro ao ler o ficheiro
     */
    public byte[] loadSectionKeyForUser(String houseName, String section, String user) throws Exception {
        File keyFile = new File("SpertaServer/data/key." + houseName + "." + section + "." + user);
        if (!keyFile.exists()) {
            return null;
        }
        return Files.readAllBytes(keyFile.toPath());
    }

    /**
     * Guarda a chave de seccao cifrada para um utilizador especifico.
     *
     * @param houseName    nome da casa
     * @param section      seccao
     * @param user         utilizador
     * @param encryptedKey chave cifrada
     * @throws Exception se ocorrer erro ao escrever o ficheiro
     */
    public void saveSectionKeyForUser(String houseName, String section, String user, byte[] encryptedKey)
            throws Exception {
        File keyFile = new File("SpertaServer/data/key." + houseName + "." + section + "." + user);
        Files.write(keyFile.toPath(), encryptedKey);
    }

    /**
     * Obtem o certificado de um utilizador.
     *
     * @param userManager gestor de utilizadores
     * @param user        utilizador
     * @return certificado ou null se nao existir
     * @throws Exception se ocorrer erro ao carregar o certificado
     */
    public Certificate getUserCertificate(UserManager userManager, String user) throws Exception {
        return userManager.loadUserCertificate(user);
    }

    private static void deleteKeyFilesExcept(String houseName, String user, String keepSection) {
        String[] sections = { "E", "G", "L", "M", "P", "S" };

        for (String section : sections) {
            // Skip the section we want to keep
            if (section.equals(keepSection)) {
                continue;
            }

            File keyFile = new File("SpertaServer/data/key." + houseName + "." + section + "." + user);

            if (keyFile.exists()) {
                boolean deleted = keyFile.delete();
                if (!deleted) {
                    System.out.println("Failed to delete: " + keyFile.getName());
                }
            }
        }
    }
}
