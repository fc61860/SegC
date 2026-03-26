import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
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

        updatePermissions(houseManager, targetUser, houseName, permission);
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
     * persistida da casa.
     *
     * @param permissions string de permissoes atual da casa
     * @param user        utilizador a atualizar
     * @param newPerm     nova permissao a acrescentar ou substituir
     * @return string de permissoes ja atualizada
     */
    private String addPermission(String permissions, String user, String newPerm) {
        if (permissions == null || permissions.trim().isEmpty()) {
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
                    updatedUsers.add(user + ":all");
                } else if (currentPerms.equals("all")) {
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
                    }
                    updatedUsers.add(user + ":" + currentPerms);
                }
            } else {
                updatedUsers.add(entry.trim());
            }
        }

        if (!userFound) {
            updatedUsers.add(user + ":" + newPerm);
        }
        return String.join(",", updatedUsers);
    }

    /**
     * Reescreve o ficheiro de casas substituindo as permissoes do utilizador na
     * casa indicada.
     *
     * @param houseManager   gestor de casas que fornece o ficheiro persistente
     * @param user           utilizador cujas permissoes vao ser atualizadas
     * @param houseName      casa a alterar
     * @param newPermissions nova permissao a aplicar
     * @throws IOException se ocorrer um erro durante a atualizacao do ficheiro
     */
    private void updatePermissions(HouseManager houseManager, String user, String houseName, String newPermissions)
            throws IOException {
        File inputFile = houseManager.getCasasFile();
        File tempFile = File.createTempFile("permissions_", ".tmp", inputFile.getParentFile());

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile));
                BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = houseManager.splitHouseLine(line);
                if (parts[0].equals(houseName)) {
                    parts[2] = addPermission(parts[2], user, newPermissions);
                    line = String.join(";", parts);
                }

                writer.write(line);
                writer.newLine();
            }
        }

        replaceFile(tempFile, inputFile);
    }

    /**
     * Substitui o ficheiro de destino por um ficheiro temporario previamente
     * escrito.
     *
     * @param source ficheiro temporario com o novo conteudo
     * @param target ficheiro final a substituir
     * @throws IOException se a substituicao falhar
     */
    private void replaceFile(File source, File target) throws IOException {
        if (target.exists() && !target.delete()) {
            throw new IOException("Nao foi possivel substituir o ficheiro " + target.getName());
        }

        if (!source.renameTo(target)) {
            throw new IOException("Nao foi possivel renomear o ficheiro temporario para " + target.getName());
        }
    }
}
