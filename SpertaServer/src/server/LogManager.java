import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Gere a exportacao de resumos e historicos de leituras dos dispositivos.
 */
public class LogManager {
    private static final String DIRETORIA_LOGS = "SpertaServer/data/logs/";
    private static final String DIRETORIA_DATA = "SpertaServer/data/";

    /**
     * Envia ao cliente um resumo com a ultima leitura disponivel de cada
     * dispositivo a que o utilizador tem acesso.
     *
     * @param houseManager gestor de casas para localizar a casa e o acesso do
     *                     utilizador
     * @param user         utilizador que pede o resumo
     * @param houseName    casa alvo do pedido
     * @param outStream    stream de saida para enviar o resultado ao cliente
     * @throws IOException se ocorrer um erro ao ler os ficheiros ou enviar o
     *                     conteudo
     */
    public void receberTemp(HouseManager houseManager, String user, String houseName, ObjectOutputStream outStream)
            throws IOException {
        if (!ValidationUtils.isValidUserOrHouse(user) || !ValidationUtils.isValidUserOrHouse(houseName)) {
            outStream.writeObject("NOK");
            outStream.flush();
            return;
        }
        String line = houseManager.findHouseLine(houseName);
        if (line == null) {
            outStream.writeObject("NOHM");
            outStream.flush();
            return;
        }
        if (!houseManager.userExistsInHouse(line, user)) {
            outStream.writeObject("NOPERM");
            outStream.flush();
            return;
        }

        sendRecentDeviceStatesFromLine(houseManager, line, user, outStream);
    }

    /**
     * Envia ao cliente o historico completo de um dispositivo especifico.
     *
     * @param houseManager       gestor de casas para localizar a casa
     * @param permissionsManager gestor de permissoes para validar o acesso ao
     *                           dispositivo
     * @param deviceManager      gestor de dispositivos para validar a existencia do
     *                           dispositivo
     * @param user               utilizador que faz o pedido
     * @param houseName          casa alvo do pedido
     * @param device             identificador do dispositivo
     * @param outStream          stream de saida para enviar o ficheiro ao cliente
     * @throws IOException se ocorrer um erro ao ler ou enviar o ficheiro
     */
    public void receberHistorico(UserManager userManager, HouseManager houseManager,
            PermissionsManager permissionsManager,
            DeviceManager deviceManager, String user, String houseName, String device, ObjectOutputStream outStream)
            throws IOException {
        if (!ValidationUtils.isValidUserOrHouse(user) || !ValidationUtils.isValidUserOrHouse(houseName)
                || !ValidationUtils.isValidDeviceId(device)) {
            outStream.writeObject("NOK");
            outStream.flush();
            return;
        }
        String line = houseManager.findHouseLine(houseName);
        if (line == null) {
            outStream.writeObject("NOHM");
            outStream.flush();
            return;
        }
        if (!permissionsManager.hasPermission(houseManager, line, user, device.substring(0, 1))) {
            outStream.writeObject("NOPERM");
            outStream.flush();
            return;
        }
        if (!deviceManager.deviceExistsInHouse(line, device)) {
            outStream.writeObject("NOD");
            outStream.flush();
            return;
        }

        sendLog(userManager, permissionsManager, houseName, device, user, outStream);
    }

    /**
     * Le a ultima linha de um ficheiro de log, que corresponde ao valor mais
     * recente disponivel.
     *
     * @param fileName caminho do ficheiro a ler
     * @return ultima linha do ficheiro ou null se o ficheiro nao existir
     * @throws IOException se ocorrer um erro na leitura
     */
    private String getLastLine(String fileName) throws IOException {
        File file = new File(fileName);
        if (!file.exists()) {
            return null;
        }
        try {
            if (!SpertaServer.checkIntegrity(fileName)) {
                System.err.println("NOK-INTEGRITY");
                System.exit(-1);
            }
            byte[] data = Files.readAllBytes(Paths.get(fileName));
            if (data.length == 0)
                return null;
            String content = new String(data, StandardCharsets.UTF_8);
            String[] lines = content.split("\\r?\\n");
            for (int i = lines.length - 1; i >= 0; i--) {
                if (!lines[i].trim().isEmpty()) {
                    return lines[i].trim();
                }
            }
            return null;
        } catch (Exception e) {
            throw new IOException("Erro ao ler log: " + e.getMessage(), e);
        }
    }

    /**
     * Constroi e envia um resumo temporario com a ultima leitura de cada
     * dispositivo acessivel ao utilizador.
     *
     * @param houseManager gestor de casas para validar propriedade e acessos
     * @param line         linha persistida da casa
     * @param user         utilizador que pediu o resumo
     * @param outStream    stream de saida para envio do ficheiro temporario
     * @throws IOException se ocorrer um erro ao gerar ou enviar o resumo
     */
    private void sendRecentDeviceStatesFromLine(HouseManager houseManager, String line, String user,
            ObjectOutputStream outStream) throws IOException {
        boolean owner = houseManager.isOwner(line, user);
        String[] parts = houseManager.splitHouseLine(line);
        String houseName = parts[0].trim();
        String permissions = parts[2].trim();
        String devicesStr = parts[3].trim();
        boolean allAccess = false;
        Set<Character> permissionSet = new HashSet<>();

        if (!permissions.isEmpty() && !owner) {
            for (String entry : permissions.split(",")) {
                String[] userParts = entry.split(":");
                if (userParts.length < 2 || !userParts[0].trim().equals(user)) {
                    continue;
                }

                String perms = userParts[1].trim();
                if (perms.equals("all")) {
                    allAccess = true;
                } else {
                    for (String perm : perms.split("\\|")) {
                        if (!perm.isEmpty()) {
                            permissionSet.add(perm.charAt(0));
                        }
                    }
                }
                break;
            }
        }

        List<String> devices = new ArrayList<>();
        if (!devicesStr.isEmpty()) {
            for (String deviceEntry : devicesStr.split(",")) {
                String device = deviceEntry.trim();
                if (allAccess || owner || permissionSet.contains(device.charAt(0))) {
                    devices.add(device);
                }
            }
        }

        boolean hasData = false;
        File summary = File.createTempFile(houseName + "_" + user + "_summary_", ".txt");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(summary))) {
            for (String device : devices) {
                String lastLine = getLastLine(DIRETORIA_LOGS + houseName + "_" + device + ".csv");
                if (lastLine != null && !lastLine.trim().isEmpty()) {
                    writer.write(device + ":" + lastLine);
                    writer.newLine();
                    hasData = true;
                }
            }
        }

        if (!hasData) {
            outStream.writeObject("NODATA");
            outStream.flush();
            summary.delete();
            return;
        }

        outStream.writeObject("OK");
        outStream.flush();

        try (FileInputStream fileIn = new FileInputStream(summary)) {
            outStream.writeLong(summary.length());
            outStream.flush();

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fileIn.read(buffer)) > 0) {
                outStream.write(buffer, 0, bytesRead);
            }
            outStream.flush();
        } finally {
            summary.delete();
        }

        // Send section keys
        String[] allSections = { "E", "G", "L", "M", "P", "S" };
        int numKeys = 0;
        for (String s : allSections) {
            File keyFile = new File(buildKeyPath(houseName, s, user));
            if (keyFile.exists() && keyFile.length() > 0) {
                numKeys++;
            }
        }

        outStream.writeInt(numKeys);
        outStream.flush();

        for (String s : allSections) {
            File keyFile = new File(buildKeyPath(houseName, s, user));
            if (keyFile.exists() && keyFile.length() > 0) {
                if (!SpertaServer.checkIntegrity(keyFile.getPath())) {
                    System.err.println("NOK-INTEGRITY");
                    System.exit(-1);
                }
                outStream.writeObject(s);
                byte[] keyBytes = Files.readAllBytes(keyFile.toPath());
                outStream.writeInt(keyBytes.length);
                outStream.write(keyBytes);
                outStream.flush();
            }
        }
    }

    /**
     * Envia para o cliente o ficheiro CSV completo de um dispositivo.
     *
     * @param houseName casa a que o dispositivo pertence
     * @param device    identificador do dispositivo
     * @param outStream stream de saida para enviar o ficheiro
     * @throws IOException se ocorrer um erro ao abrir ou transmitir o log
     */
    private void sendLog(UserManager userManager, PermissionsManager permissionsManager, String houseName,
            String device, String user, ObjectOutputStream outStream) throws IOException {
        String logPath = DIRETORIA_LOGS + houseName + "_" + device + ".csv";
        File log = new File(logPath);

        if (!log.exists() || log.length() == 0) {
            outStream.writeObject("NODATA");
            outStream.flush();
            return;
        }

        try {
            if (!SpertaServer.checkIntegrity(logPath)) {
                System.err.println("NOK-INTEGRITY");
                System.exit(-1);
            }
            byte[] logBytes = Files.readAllBytes(Paths.get(logPath));

            if (logBytes.length == 0) {
                outStream.writeObject("NODATA");
                outStream.flush();
                return;
            }

            outStream.writeObject("OK");
            outStream.flush();

            outStream.writeLong(logBytes.length);
            outStream.flush();

            outStream.write(logBytes);
            outStream.flush();

            sendSectionKeyFile(houseName, device.charAt(0), user, outStream);
        } catch (Exception e) {
            throw new IOException("Erro ao enviar log: " + e.getMessage(), e);
        }
    }

    private void sendSectionKeyFile(String houseName, char section, String user, ObjectOutputStream outStream)
            throws IOException {
        String keyFilePath = buildKeyPath(houseName, String.valueOf(section), user);
        File keyFile = new File(keyFilePath);

        if (!keyFile.exists() || keyFile.length() == 0) {
            outStream.writeObject("NODATA_KEY");
            outStream.flush();
            return;
        }
        if (!SpertaServer.checkIntegrity(keyFile.getPath())) {
            System.err.println("NOK-INTEGRITY");
            System.exit(-1);
        }
        byte[] keyBytes = Files.readAllBytes(keyFile.toPath());

        outStream.writeObject("OK_KEY");
        outStream.flush();

        outStream.writeLong(keyBytes.length);
        outStream.flush();

        outStream.write(keyBytes);
        outStream.flush();

    }

    private static String buildKeyPath(String houseName, String section, String user) {
        return DIRETORIA_DATA + "key." + houseName + "." + section + "." + user;
    }
}
