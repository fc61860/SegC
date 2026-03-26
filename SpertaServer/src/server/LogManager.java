import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Gere a exportacao de resumos e historicos de leituras dos dispositivos.
 */
public class LogManager {
    private static final String DIRETORIA_LOGS = "SpertaServer/data/logs/";

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
    public void receberHistorico(HouseManager houseManager, PermissionsManager permissionsManager,
            DeviceManager deviceManager, String user, String houseName, String device, ObjectOutputStream outStream)
            throws IOException {
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

        sendLog(houseName, device, outStream);
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

        String lastLine = null;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lastLine = line;
            }
        }
        return lastLine;
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
    }

    /**
     * Envia para o cliente o ficheiro CSV completo de um dispositivo.
     *
     * @param houseName casa a que o dispositivo pertence
     * @param device    identificador do dispositivo
     * @param outStream stream de saida para enviar o ficheiro
     * @throws IOException se ocorrer um erro ao abrir ou transmitir o log
     */
    private void sendLog(String houseName, String device, ObjectOutputStream outStream) throws IOException {
        File log = new File(DIRETORIA_LOGS + houseName + "_" + device + ".csv");
        outStream.writeObject("OK");
        outStream.flush();

        try (FileInputStream fileIn = new FileInputStream(log)) {
            outStream.writeLong(log.length());
            outStream.flush();

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fileIn.read(buffer)) > 0) {
                outStream.write(buffer, 0, bytesRead);
            }
            outStream.flush();
        }
    }
}
