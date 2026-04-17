import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

        if (!SpertaServer.checkIntegrity(FICHEIRO_ESTADOS)) {
            System.err.println("NOK-INTEGRITY");
            System.exit(-1);
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

        if (!SpertaServer.checkIntegrity(FICHEIRO_ESTADOS)) {
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
        File input = houseManager.getCasasFile();
        File temp = File.createTempFile("houses_", ".tmp", input.getParentFile());

        try (BufferedReader reader = new BufferedReader(new FileReader(input));
                BufferedWriter writer = new BufferedWriter(new FileWriter(temp))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = houseManager.splitHouseLine(line);
                if (parts[0].equals(houseName)) {
                    String devices = parts[3].trim();
                    String newDevice = nextDevice(devices, place);
                    addDeviceWithDefaultTime(houseName, newDevice);
                    createDeviceLog(houseName, newDevice);
                    parts[3] = devices.isEmpty() ? newDevice : devices + ", " + newDevice;
                    line = String.join(";", parts);
                }

                writer.write(line);
                writer.newLine();
            }
        }

        replaceFile(temp, input);
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
        File inputFile = new File(FICHEIRO_ESTADOS);
        File tempFile = File.createTempFile("states_", ".tmp", inputFile.getParentFile());

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile));
                BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(";");
                if (parts[0].trim().equals(houseName)) {
                    String[] devList = parts[1].split(",");
                    StringBuilder updatedDevices = new StringBuilder();
                    for (int i = 0; i < devList.length; i++) {
                        String[] deviceParts = devList[i].trim().split(":");
                        String deviceName = deviceParts[0];
                        String time = deviceParts[1];

                        if (deviceName.startsWith(place)) {
                            time = String.valueOf(newTime);
                            addLogEntry(houseName, deviceName, newTime);
                        }

                        updatedDevices.append(deviceName).append(":").append(time);
                        if (i < devList.length - 1) {
                            updatedDevices.append(", ");
                        }
                    }
                    line = houseName + "; " + updatedDevices;
                }

                writer.write(line);
                writer.newLine();
            }
        }

        replaceFile(tempFile, inputFile);
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
        File input = new File(FICHEIRO_ESTADOS);
        // if (!input.exists()) {
        //     input.createNewFile();
        // }

        File temp = File.createTempFile("times_", ".tmp", input.getParentFile());
        boolean houseFound = false;

        try (BufferedReader reader = new BufferedReader(new FileReader(input));
                BufferedWriter writer = new BufferedWriter(new FileWriter(temp))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(";", -1);
                if (parts[0].trim().equals(houseName)) {
                    houseFound = true;
                    String devices = parts.length > 1 ? parts[1].trim() : "";
                    devices = devices.isEmpty() ? device + ":0" : devices + ", " + device + ":0";
                    line = parts[0] + ";" + devices;
                }

                writer.write(line);
                writer.newLine();
            }

            if (!houseFound) {
                writer.write(houseName + ";" + device + ":0");
                writer.newLine();
            }
        }

        replaceFile(temp, input);
    }

    /**
     * Cria o ficheiro CSV que vai guardar o historico de valores do dispositivo.
     *
     * @param houseName casa a que o dispositivo pertence
     * @param device    identificador do dispositivo
     * @throws IOException se o ficheiro de log nao puder ser criado
     */
    private void createDeviceLog(String houseName, String device) throws IOException {
        // File pastaLogs = new File(DIRETORIA_LOGS);
        // if (!pastaLogs.exists()) {
        //     pastaLogs.mkdirs();
        // }

        File logFile = new File(DIRETORIA_LOGS + houseName + "_" + device + ".csv");
        logFile.createNewFile();
        try {
            SpertaServer.saveHashFile(logFile.getPath());
        } catch (Exception e) {
            System.err.println("Erro ao criar HMAC do log: " + e.getMessage());
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
        if (!SpertaServer.checkIntegrity(fileName)) {
            System.err.println("NOK-INTEGRITY");
            System.exit(-1);
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, true))) {
            LocalDateTime agora = LocalDateTime.now();
            DateTimeFormatter formatador = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            writer.write(agora.format(formatador) + ", " + value);
            writer.newLine();
        }
        try {
            SpertaServer.saveHashFile(fileName);
        } catch (Exception e) {
            System.err.println("Erro ao atualizar HMAC do log: " + e.getMessage());
        }
    }

    /**
     * Substitui um ficheiro persistente pelo respetivo ficheiro temporario e
     * atualiza o HMAC para garantir a integridade.
     *
     * @param source ficheiro temporario com o novo conteudo
     * @param target ficheiro final a atualizar
     * @throws IOException se a operacao falhar
     */
    private void replaceFile(File source, File target) throws IOException {
        if (target.exists() && !target.delete()) {
            throw new IOException("Nao foi possivel substituir o ficheiro " + target.getName());
        }

        if (!source.renameTo(target)) {
            throw new IOException("Nao foi possivel renomear o ficheiro temporario para " + target.getName());
        }

        try {
            SpertaServer.saveHashFile(target.getPath());
        } catch (Exception e) {
            System.err.println("Erro ao atualizar o HMAC de " + target.getName() + ": " + e.getMessage());
        }
    }
}
