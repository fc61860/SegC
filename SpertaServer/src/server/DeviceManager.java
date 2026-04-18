import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Gere o registo de dispositivos e a atualizacao dos respetivos estados e logs.
 */
public class DeviceManager {
    private static final String FICHEIRO_ESTADOS = "SpertaServer/data/estados.txt";
    private static final String DIRETORIA_LOGS = "SpertaServer/data/logs/";
    private static final Set<String> VALID_SECS = Set.of("E", "G", "L", "M", "P", "S");

    /**
     * Regista um novo dispositivo numa secao de uma casa.
     *
     * @param houseManager gestor de casas para validar a existencia e o dono
     * @param user         utilizador que faz o pedido
     * @param houseName    casa onde o dispositivo sera registado
     * @param section      secao onde o dispositivo sera colocado
     * @return OK quando o dispositivo e registado; caso contrario um dos codigos
     *         de erro do protocolo
     * @throws IOException se ocorrer um erro ao persistir o novo dispositivo
     */
    public String registarDispositivo(HouseManager houseManager, String user, String houseName, String section)
            throws IOException {
        if (!VALID_SECS.contains(section)) {
            return "NOK";
        }

        String line = houseManager.findHouseLine(houseName);
        if (line == null) {
            return "NOHM";
        }
        if (!houseManager.isOwner(line, user)) {
            return "NOPERM";
        }

        addDevice(houseManager, houseName, section);
        return "OK";
    }

    /**
     * Atualiza o valor temporal associado a um dispositivo existente.
     *
     * @param houseManager       gestor de casas para localizar a casa
     * @param permissionsManager gestor de permissoes para validar acesso ao
     *                           dispositivo
     * @param user               utilizador que faz o pedido
     * @param houseName          casa onde o dispositivo existe
     * @param device             identificador do dispositivo
     * @param rawValue           novo valor recebido no protocolo
     * @return OK quando o valor e atualizado; caso contrario um dos codigos de
     *         erro do protocolo
     * @throws IOException se ocorrer um erro ao atualizar estado ou logs
     */
    public String envioValor(HouseManager houseManager, PermissionsManager permissionsManager, String user,
            String houseName, String device, String rawValue) throws IOException {
        int value;
        try {
            value = Integer.parseInt(rawValue);
        } catch (NumberFormatException e) {
            return "NOK";
        }

        if (value < 0 || value > 600) {
            return "NOK";
        }

        if (!SpertaServer.checkIntegrityEncrypted(FICHEIRO_ESTADOS)) {
            System.err.println("NOK-INTEGRITY");
            System.exit(-1);
        }
        String line = houseManager.findHouseLine(houseName);
        if (line == null) {
            return "NOHM";
        }
        if (!permissionsManager.hasPermission(houseManager, line, user, device.substring(0, 1))) {
            return "NOPERM";
        }
        if (!deviceExistsInHouse(line, device)) {
            return "NOD";
        }

        updatePlaceTimeInHouse(houseName, device, value);
        return "OK";
    }

    /**
     * Verifica se um dispositivo esta registado numa determinada casa.
     *
     * @param houseLine linha da casa no formato persistido
     * @param device    identificador do dispositivo
     * @return true se o dispositivo existir nessa casa; false caso contrario
     */
    public boolean deviceExistsInHouse(String houseLine, String device) {
        String[] parts = new HouseManager().splitHouseLine(houseLine);
        String devicesStr = parts[3].trim();
        if (devicesStr.isEmpty()) {
            return false;
        }

        for (String currentDevice : devicesStr.split(",")) {
            if (currentDevice.trim().equals(device)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determina o identificador do proximo dispositivo para uma dada secao com base
     * nos dispositivos ja registados.
     *
     * @param devices lista persistida de dispositivos da casa
     * @param place   secao a que o novo dispositivo pertence
     * @return identificador unico para o novo dispositivo
     */
    private String nextDevice(String devices, String place) {
        if (devices == null || devices.trim().isEmpty()) {
            return place + "1";
        }

        int max = 0;
        for (String device : devices.split(",")) {
            String trimmed = device.trim();
            if (trimmed.startsWith(place)) {
                int num = Integer.parseInt(trimmed.substring(1));
                if (num > max) {
                    max = num;
                }
            }
        }
        return place + (max + 1);
    }

    /**
     * Acrescenta um novo dispositivo a casa indicada, cria o respetivo estado
     * inicial e prepara o ficheiro de log.
     *
     * @param houseManager gestor de casas que fornece o ficheiro persistente
     * @param houseName    casa a atualizar
     * @param place        secao onde o dispositivo sera registado
     * @throws IOException se ocorrer um erro ao reescrever a persistencia
     */
    private void addDevice(HouseManager houseManager, String houseName, String place) throws IOException {
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
                    String devices = parts[3].trim();
                    String newDevice = nextDevice(devices, place);
                    addDeviceWithDefaultTime(houseName, newDevice);
                    createDeviceLog(houseName, newDevice);
                    parts[3] = devices.isEmpty() ? newDevice : devices + ", " + newDevice;
                    lines.set(i, String.join(";", parts));
                    break;
                }
            }

            String newContent = String.join("\n", lines) + "\n";
            SpertaServer.writeEncrypted(houseManager.getCasasFile().getPath(),
                    newContent.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IOException("Erro ao registar dispositivo: " + e.getMessage(), e);
        }
    }

    /**
     * Atualiza o valor do dispositivo indicado no ficheiro de estados e regista a
     * alteracao no log historico.
     *
     * @param houseName casa onde o dispositivo existe
     * @param place     identificador do dispositivo a atualizar
     * @param newTime   novo valor a persistir
     * @throws IOException se ocorrer um erro ao atualizar estados ou logs
     */
    private void updatePlaceTimeInHouse(String houseName, String place, int newTime) throws IOException {
        try {
            byte[] data = SpertaServer.readDecrypted(FICHEIRO_ESTADOS);
            String content = new String(data, StandardCharsets.UTF_8);
            String[] rawLines = content.split("\\r?\\n", -1);
            List<String> lines = new ArrayList<>();
            for (String l : rawLines) {
                if (!l.isEmpty())
                    lines.add(l);
            }

            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                String[] parts = line.split(";");
                if (parts[0].trim().equals(houseName)) {
                    String[] devList = parts[1].split(",");
                    StringBuilder updatedDevices = new StringBuilder();
                    for (int j = 0; j < devList.length; j++) {
                        String[] deviceParts = devList[j].trim().split(":");
                        String deviceName = deviceParts[0];
                        String time = deviceParts[1];

                        if (deviceName.startsWith(place)) {
                            time = String.valueOf(newTime);
                            addLogEntry(houseName, deviceName, newTime);
                        }

                        updatedDevices.append(deviceName).append(":").append(time);
                        if (j < devList.length - 1) {
                            updatedDevices.append(", ");
                        }
                    }
                    lines.set(i, houseName + "; " + updatedDevices);
                    break;
                }
            }

            String newContent = String.join("\n", lines) + "\n";
            SpertaServer.writeEncrypted(FICHEIRO_ESTADOS, newContent.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IOException("Erro ao atualizar estados: " + e.getMessage(), e);
        }
    }

    /**
     * Garante que o novo dispositivo fica com estado inicial a zero no ficheiro de
     * estados.
     *
     * @param houseName casa onde o dispositivo sera registado
     * @param device    identificador do dispositivo
     * @throws IOException se ocorrer um erro ao atualizar o ficheiro de estados
     */
    private void addDeviceWithDefaultTime(String houseName, String device) throws IOException {
        try {
            byte[] data = SpertaServer.readDecrypted(FICHEIRO_ESTADOS);
            String content = new String(data, StandardCharsets.UTF_8);
            String[] rawLines = content.split("\\r?\\n", -1);
            List<String> lines = new ArrayList<>();
            for (String l : rawLines) {
                if (!l.isEmpty())
                    lines.add(l);
            }

            boolean houseFound = false;
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                String[] parts = line.split(";", -1);
                if (parts[0].trim().equals(houseName)) {
                    houseFound = true;
                    String devices = parts.length > 1 ? parts[1].trim() : "";
                    devices = devices.isEmpty() ? device + ":0" : devices + ", " + device + ":0";
                    lines.set(i, parts[0] + ";" + devices);
                    break;
                }
            }

            if (!houseFound) {
                lines.add(houseName + ";" + device + ":0");
            }

            String newContent = String.join("\n", lines) + "\n";
            SpertaServer.writeEncrypted(FICHEIRO_ESTADOS, newContent.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IOException("Erro ao atualizar estados.txt: " + e.getMessage(), e);
        }
    }

    /**
     * Cria o ficheiro CSV que vai guardar o historico de valores do dispositivo.
     *
     * @param houseName casa a que o dispositivo pertence
     * @param device    identificador do dispositivo
     * @throws IOException se o ficheiro de log nao puder ser criado
     */
    private void createDeviceLog(String houseName, String device) throws IOException {
        try {
            String logPath = DIRETORIA_LOGS + houseName + "_" + device + ".csv";
            SpertaServer.writeEncrypted(logPath, new byte[0]);
        } catch (Exception e) {
            throw new IOException("Erro ao criar log cifrado: " + e.getMessage(), e);
        }
    }

    /**
     * Acrescenta uma nova entrada temporal ao log CSV do dispositivo.
     *
     * @param houseName casa a que o dispositivo pertence
     * @param device    identificador do dispositivo
     * @param value     valor registado
     * @throws IOException se ocorrer um erro ao escrever no ficheiro de log
     */
    private void addLogEntry(String houseName, String device, int value) throws IOException {
        String fileName = DIRETORIA_LOGS + houseName + "_" + device + ".csv";
        try {
            byte[] existing = SpertaServer.readDecrypted(fileName);
            String existingContent = new String(existing, StandardCharsets.UTF_8);
            LocalDateTime agora = LocalDateTime.now();
            DateTimeFormatter formatador = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String newEntry = agora.format(formatador) + ", " + value + "\n";
            String newContent = existingContent + newEntry;
            SpertaServer.writeEncrypted(fileName, newContent.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IOException("Erro ao atualizar log: " + e.getMessage(), e);
        }
    }
}
