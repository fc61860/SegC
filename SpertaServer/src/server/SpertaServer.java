import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Classe principal do servidor SpertaServer.
 * Responsável por inicializar o servidor, aceitar conexões de clientes e gerir
 * a autenticação, comandos e persistência de dados.
 */
public class SpertaServer {
	private static final int MAX_TENTATIVAS = 3;
	private static final String FICHEIRO_USERS = "SpertaServer/data/users.txt";
	private static final String FICHEIRO_CASAS = "SpertaServer/data/casas.txt";
	private static final String FICHEIRO_ESTADOS = "SpertaServer/data/estados.txt";
	private static final String FICHEIRO_CLIENTSIZE = "SpertaServer/data/client_size.txt";

	private static final String DIRETORIA_LOGS = "SpertaServer/data/logs/";

	private static final Set<String> VALID_PERMS = Set.of("E", "G", "L", "M", "P", "S", "all");
	private static final Set<String> VALID_SECS = Set.of("E", "G", "L", "M", "P", "S");

	/**
	 * Método principal. Inicia o servidor na porta especificada (ou default).
	 * 
	 * @param args Argumentos de linha de comando (opcional: porta)
	 */
	public static void main(String[] args) {
		System.out.println("Servidor: main");
		int port = 22345; // Default

		if (args.length == 1) {
			try {
				port = Integer.parseInt(args[0]);
			} catch (NumberFormatException e) {
				System.out.println("Erro: Porto tem de ser número. A usar default.");
			}
		}

		SpertaServer server = new SpertaServer();
		server.startServer(port); // Passa a variável
	}

	/**
	 * Inicia o servidor na porta especificada e aceita conexões de clientes.
	 * 
	 * @param port Porta TCP para escutar.
	 */
	public void startServer(int port) {
		inicializarEstrutura();
		ServerSocket sSoc = null;

		try {
			sSoc = new ServerSocket(port);
			System.out.println("Servidor à escuta na porta: " + port);
		} catch (IOException e) {
			System.err.println(e.getMessage());
			System.exit(-1);
		}

		while (true) {
			try {
				Socket inSoc = sSoc.accept();
				ServerThread newServerThread = new ServerThread(inSoc);
				newServerThread.start();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
		// sSoc.close();
	}

	/**
	 * Inicializa a estrutura de diretórios e ficheiros necessários ao funcionamento
	 * do servidor.
	 */
	private static void inicializarEstrutura() {
		try {

			File dataDir = new File("SpertaServer/data");
			dataDir.mkdirs();

			File logsDir = new File(DIRETORIA_LOGS);
			logsDir.mkdirs();

			File usersFile = new File(FICHEIRO_USERS);
			usersFile.createNewFile();

			File casasFile = new File(FICHEIRO_CASAS);
			casasFile.createNewFile();

			File estadosFile = new File(FICHEIRO_ESTADOS);
			estadosFile.createNewFile();

		} catch (IOException e) {
			System.err.println("Erro ao inicializar ficheiros: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Thread responsável por tratar a comunicação com um cliente.
	 */
	class ServerThread extends Thread {

		private Socket socket = null;

		ServerThread(Socket inSoc) {
			socket = inSoc;
			System.out.println("thread do server para cada cliente");
		}

		/**
		 * Executa a lógica de comunicação com o cliente: atestação, autenticação e
		 * processamento de comandos.
		 */
		public void run() {
			try {
				ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream());
				ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream());
				long clientSize = inStream.readLong();
				BufferedReader reader = new BufferedReader(new FileReader(FICHEIRO_CLIENTSIZE));

				String line = reader.readLine(); // "SpertaClient:4096"
				String[] parts = line.split(":");

				long expectedSize = Long.parseLong(parts[1]);

				reader.close();
				if (clientSize != expectedSize) {
					outStream.writeObject("NOK");
					outStream.close();
					inStream.close();
					socket.close();
					return;
				}

				outStream.writeObject("OK");
				String user = null;
				boolean validClient = false;
				try {
					user = (String) inStream.readObject();
					validClient = autenticarCliente(user, inStream, outStream);

				} catch (ClassNotFoundException e1) {
					e1.printStackTrace();
				}
				if (validClient) {
					try {
						while (true) {
							processCommand(user, inStream, outStream);

						}
						// Ctrl-C
					} catch (EOFException e) {
						System.out.println("Client disconnected.");
					} catch (IOException e) {
						System.out.println("Connection lost.");
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					} finally {
						socket.close();
					}
				}

				outStream.close();
				inStream.close();

				socket.close();

			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		/**
		 * Autentica o cliente, permitindo criação de novo utilizador ou validação de
		 * password.
		 * 
		 * @param user      Nome de utilizador
		 * @param inStream  Stream de entrada do cliente
		 * @param outStream Stream de saída para o cliente
		 * @return true se autenticado com sucesso, false caso contrário
		 * @throws IOException
		 * @throws ClassNotFoundException
		 */
		private static boolean autenticarCliente(String user, ObjectInputStream inStream,
				ObjectOutputStream outStream) throws IOException, ClassNotFoundException {
			int tentativas = 0;

			String passwd = null;

			File file = new File(FICHEIRO_USERS);
			if (!file.exists())
				file.createNewFile();

			Scanner sc = new Scanner(file);
			String correctPassword = null;
			boolean exists = false;
			while (sc.hasNextLine()) {
				String line = sc.nextLine();
				String[] parts = line.split(":");
				if (parts[0].equals(user)) {
					exists = true;
					correctPassword = parts[1];
					break;
				}
			}
			sc.close();

			// novo utilizador
			if (!exists) {
				passwd = (String) inStream.readObject();
				FileWriter fw = new FileWriter(file, true);
				fw.write(user + ":" + passwd + "\n");
				fw.close();
				outStream.writeObject("OK-NEW-USER");
				outStream.flush();
				return true;
			}

			// autenticação
			while (tentativas < MAX_TENTATIVAS) {
				passwd = (String) inStream.readObject();
				if (correctPassword.equals(passwd)) {
					outStream.writeObject("ATTESTATION OK");
					outStream.flush();
					return true;
				} else {
					tentativas++;
					if (tentativas > 0) {
						outStream.writeObject("WRONG-PWD-" + tentativas);
						outStream.flush();
					} else {
						outStream.writeObject("USER-BLOCKED");
						outStream.flush();
					}
				}
			}
			return false;
		}

		/**
		 * Processa comandos recebidos do cliente e executa a ação correspondente.
		 * 
		 * @param user      Utilizador autenticado
		 * @param inStream  Stream de entrada do cliente
		 * @param outStream Stream de saída para o cliente
		 * @throws IOException
		 * @throws ClassNotFoundException
		 */
		private static void processCommand(String user, ObjectInputStream inStream, ObjectOutputStream outStream)
				throws IOException, ClassNotFoundException {

			String message = (String) inStream.readObject();

			String[] parts = message.split(" ");
			String command = parts[0];
			switch (command) {

				case "CREATE":
					criarCasa(user, outStream, parts);
					break;
				case "ADD":
					adicionarUtilizador(user, outStream, parts);
					break;
				case "RD":
					registarDispositivo(user, outStream, parts);
					break;
				case "EC":
					envioValor(user, outStream, parts);
					break;
				case "RT":
					receberTemp(user, outStream, parts);
					break;
				case "RH":
					receberHistorico(user, outStream, parts);
					break;
				default:
					// isto nao e suposto acontecer com um SpertaClient valido, mas assim o server
					// proteje-se dos outros
					outStream.writeObject("NOCOMMAND");
			}
		}

		/**
		 * Cria uma nova casa associada ao utilizador.
		 * 
		 * @param user      Utilizador
		 * @param outStream Stream de saída para o cliente
		 * @param parts     Argumentos do comando
		 * @throws IOException
		 * @throws ClassNotFoundException
		 */
		private static void criarCasa(String user, ObjectOutputStream outStream, String[] parts)
				throws IOException, ClassNotFoundException {
			// teste de input
			// nao deixar clientes meterem ; no nome da sua casa
			if (parts.length != 2 || parts[1].contains(";")) {
				outStream.writeObject("NOK");
				return;
			}

			// Casa ja existe
			if (findHouseLine(FICHEIRO_CASAS, parts[1]) != null) {
				outStream.writeObject("NOK");
			} else {
				BufferedWriter writer = new BufferedWriter(new FileWriter(FICHEIRO_CASAS, true));

				String newLine = parts[1] + ";" + user + ";;";

				writer.write(newLine);
				writer.newLine();

				writer.close();
				// so confirmar se tudo correr bem
				outStream.writeObject("OK");
			}
		}

		/**
		 * Adiciona um utilizador a uma casa com permissões específicas.
		 * 
		 * @param user      Utilizador que executa o comando
		 * @param outStream Stream de saída para o cliente
		 * @param parts     Argumentos do comando
		 * @throws IOException
		 * @throws ClassNotFoundException
		 */
		private static void adicionarUtilizador(String user, ObjectOutputStream outStream, String[] parts)
				throws IOException, ClassNotFoundException {

			// teste de input
			// divisao tem de ser uma das default
			if (parts.length != 4 || !VALID_PERMS.contains(parts[3])) {
				outStream.writeObject("NOK");
				return;
			}
			if (user.equals(parts[1])) {
				outStream.writeObject("NOK");
				return;
			}
			String line = findHouseLine(FICHEIRO_CASAS, parts[2]);
			if (line == null) {
				outStream.writeObject("NOHM");
			} else if (!userExists(parts[1])) {
				outStream.writeObject("NOUSER");
			} else if (!isOwner(line, user)) {
				outStream.writeObject("NOPERM");
				// caso base
			} else {
				updatePermissions(parts[1], parts[2], parts[3]);
				outStream.writeObject("OK");
			}

		}

		/**
		 * Regista um novo dispositivo numa casa.
		 * 
		 * @param user      Utilizador
		 * @param outStream Stream de saída para o cliente
		 * @param parts     Argumentos do comando
		 * @throws IOException
		 * @throws ClassNotFoundException
		 */
		private static void registarDispositivo(String user, ObjectOutputStream outStream, String[] parts)
				throws IOException, ClassNotFoundException {
			// teste de input
			// divisao tem de ser uma das default
			if (parts.length != 3 || !VALID_SECS.contains(parts[2])) {
				outStream.writeObject("NOK");
				return;
			}
			String line = findHouseLine(FICHEIRO_CASAS, parts[1]);
			if (line == null) {
				outStream.writeObject("NOHM");
			} else if (!isOwner(line, user)) {
				outStream.writeObject("NOPERM");
			} else {
				addDevice(parts[1], parts[2]);
				outStream.writeObject("OK");
			}
		}

		/**
		 * Regista um valor para um dispositivo numa casa.
		 * 
		 * @param user      Utilizador
		 * @param outStream Stream de saída para o cliente
		 * @param parts     Argumentos do comando
		 * @throws IOException
		 * @throws ClassNotFoundException
		 */
		private static void envioValor(String user, ObjectOutputStream outStream, String[] parts)
				throws IOException, ClassNotFoundException {
			// teste de input
			if (parts.length != 4) {
				outStream.writeObject("NOK");
				return;
			}

			int value;
			// verifica o valor
			try {
				value = Integer.parseInt(parts[3]);
			} catch (NumberFormatException e) {
				outStream.writeObject("NOK");
				return;
			}
			// [0, 600[
			if (value < 0 || value > 600) {
				outStream.writeObject("NOK");
				return;
			}

			String line = findHouseLine(FICHEIRO_CASAS, parts[1]);
			if (line == null) {
				outStream.writeObject("NOHM");
			} else if (!hasPermission(line, user, parts[2].substring(0, 1))) {
				outStream.writeObject("NOPERM");
			} else if (!deviceExistsInHouse(line, parts[2])) {
				outStream.writeObject("NOD");
			} else {
				updatePlaceTimeInHouse(parts[1], parts[2], value);
				outStream.writeObject("OK");
			}
		}

		/**
		 * Envia ao cliente os estados recentes dos dispositivos de uma casa.
		 * 
		 * @param user      Utilizador
		 * @param outStream Stream de saída para o cliente
		 * @param parts     Argumentos do comando
		 * @throws IOException
		 * @throws ClassNotFoundException
		 */
		private static void receberTemp(String user, ObjectOutputStream outStream, String[] parts)
				throws IOException, ClassNotFoundException {
			// teste de input
			if (parts.length != 2) {
				outStream.writeObject("NOK");
				return;
			}
			String line = findHouseLine(FICHEIRO_CASAS, parts[1]);
			if (line == null) {
				outStream.writeObject("NOHM");
			} else if (!userExistInHouse(line, user)) {
				outStream.writeObject("NOPERM");
			} else {
				sendRecentDeviceStatesFromLine(line, user, outStream);
			}

		}

		/**
		 * Envia ao cliente o histórico de um dispositivo de uma casa.
		 * 
		 * @param user      Utilizador
		 * @param outStream Stream de saída para o cliente
		 * @param parts     Argumentos do comando
		 * @throws IOException
		 * @throws ClassNotFoundException
		 */
		private static void receberHistorico(String user, ObjectOutputStream outStream, String[] parts)
				throws IOException, ClassNotFoundException {
			// teste de input
			if (parts.length != 3) {
				outStream.writeObject("NOK");
				return;
			}
			String line = findHouseLine(FICHEIRO_CASAS, parts[1]);
			if (line == null) {
				outStream.writeObject("NOHM");
			} else if (!hasPermission(line, user, parts[2].substring(0, 1))) {
				outStream.writeObject("NOPERM");
			} else if (!deviceExistsInHouse(line, parts[2])) {
				outStream.writeObject("NOD");
			} else {
				sendLog(parts[1], parts[2], outStream);
			}
		}

		/**
		 * Procura e devolve a linha correspondente a uma casa num ficheiro.
		 * 
		 * @param filePath  Caminho do ficheiro
		 * @param houseName Nome da casa
		 * @return Linha correspondente à casa, ou null se não existir
		 * @throws IOException
		 */
		private static String findHouseLine(String filePath, String houseName) throws IOException {
			BufferedReader br = new BufferedReader(new FileReader(filePath));
			String line;

			while ((line = br.readLine()) != null) {
				String[] parts = line.split(";");

				if (parts[0].equals(houseName)) {
					br.close();
					return line;
				}
			}

			br.close();
			return null;
		}

		/**
		 * Verifica se o utilizador é o proprietário da casa.
		 * 
		 * @param line     Linha da casa
		 * @param username Nome do utilizador
		 * @return true se for proprietário, false caso contrário
		 */
		private static boolean isOwner(String line, String username) {
			String[] parts = line.split(";");
			String owner = parts[1].trim();
			return owner.equals(username);
		}

		/**
		 * Verifica se o utilizador tem permissão para aceder a uma divisão.
		 * 
		 * @param line     Linha da casa
		 * @param username Nome do utilizador
		 * @param place    Divisão
		 * @return true se tiver permissão, false caso contrário
		 */
		private static boolean hasPermission(String line, String username, String place) {
			if (isOwner(line, username)) {
				return true;
			}
			String[] parts = line.split(";", -1);

			String permissions = parts[2].trim();

			// Split users
			String[] users = permissions.split(",");

			for (String userPerm : users) {
				String[] userParts = userPerm.split(":");

				String user = userParts[0].trim();

				if (user.equals(username)) {

					String permPlaces = userParts[1].trim();

					if (permPlaces.equals("all")) {
						return true;
					}

					String[] places = permPlaces.split("\\|");

					for (String p : places) {
						if (p.trim().equals(place)) {
							return true;
						}
					}
				}
			}
			return false;
		}

		/**
		 * Verifica se um utilizador existe no ficheiro de utilizadores.
		 * 
		 * @param user Nome do utilizador
		 * @return true se existir, false caso contrário
		 * @throws FileNotFoundException
		 */
		private static boolean userExists(String user) throws FileNotFoundException {
			File file = new File(FICHEIRO_USERS);

			Scanner sc = new Scanner(file);
			while (sc.hasNextLine()) {
				String line = sc.nextLine();
				String[] parts = line.split(":");
				if (parts[0].equals(user)) {
					sc.close();
					return true;
				}
			}
			sc.close();
			return false;
		}

		/**
		 * Adiciona uma permissão a um utilizador na string de permissões.
		 * 
		 * @param permissions String de permissões existente
		 * @param user        Nome do utilizador
		 * @param newPerm     Nova permissão a adicionar
		 * @return String de permissões atualizada
		 */
		private static String addPermission(String permissions, String user, String newPerm) {
			// Se não há permissões nenhumas ou tem all, é só meter a letra da nova
			// permissao ou retirar o all e meter so a nova
			if (permissions == null || permissions.trim().isEmpty() || permissions.equals("all")) {
				return user + ":" + newPerm;
			}

			String[] users = permissions.split(",");
			List<String> updatedUsers = new ArrayList<>();
			boolean userFound = false;

			for (String u : users) {
				if (u.trim().isEmpty())
					continue; // Ignora lixo ou espaços em branco

				String[] parts = u.split(":");
				// Se a estrutura estiver mal formada, guarda como está e avança
				if (parts.length != 2) {
					updatedUsers.add(u.trim());
					continue;
				}

				String currentUser = parts[0].trim();
				String currentPerms = parts[1].trim();

				if (currentUser.equals(user)) {
					userFound = true; // Encontrámos o utilizador!

					// Se ele já for "all" ou se lhe formos dar "all", fica "all"
					if (currentPerms.equals("all") || newPerm.equals("all")) {
						updatedUsers.add(user + ":all");
					} else {
						// Verifica se a nova permissão já lá está
						String[] perms = currentPerms.split("\\|");
						boolean exists = false;
						for (String p : perms) {
							if (p.equals(newPerm)) {
								exists = true;
								break;
							}
						}
						if (!exists) {
							currentPerms += "|" + newPerm; // Acrescenta a nova
						}
						updatedUsers.add(user + ":" + currentPerms);
					}
				} else {
					updatedUsers.add(u.trim()); // É outro utilizador, mantemos igual
				}
			}

			// 2. Se passámos por todos e o utilizador não existia na lista, acrescentamos
			// agora!
			if (!userFound) {
				updatedUsers.add(user + ":" + newPerm);
			}

			// Junta tudo de novo separado por vírgulas.
			return String.join(",", updatedUsers);
		}

		/**
		 * Atualiza as permissões de um utilizador numa casa.
		 * 
		 * @param user           Nome do utilizador
		 * @param houseName      Nome da casa
		 * @param newPermissions Permissões a adicionar
		 * @throws IOException
		 */
		public static void updatePermissions(String user, String houseName, String newPermissions)
				throws IOException {

			File inputFile = new File(FICHEIRO_CASAS);
			File tempFile = new File("temp.txt");

			BufferedReader reader = new BufferedReader(new FileReader(inputFile));
			BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));

			String line;

			while ((line = reader.readLine()) != null) {

				String[] parts = line.split(";", -1);

				if (parts[0].equals(houseName)) {
					// modifica permissões
					String permissions = parts[2];

					permissions = addPermission(permissions, user, newPermissions);

					parts[2] = permissions;

					line = String.join(";", parts);
				}

				writer.write(line);
				writer.newLine();
			}

			reader.close();
			writer.close();

			inputFile.delete();
			tempFile.renameTo(inputFile);
		}

		/**
		 * Calcula o próximo identificador de dispositivo para uma divisão.
		 * 
		 * @param devices String com dispositivos existentes
		 * @param place   Divisão
		 * @return Novo identificador de dispositivo
		 */
		private static String nextDevice(String devices, String place) {
			String[] devs = devices.split(",");
			int max = 0;

			for (String d : devs) {
				d = d.trim();

				if (d.startsWith(place)) {
					int num = Integer.parseInt(d.substring(1));
					if (num > max) {
						max = num;
					}
				}
			}

			return place + (max + 1);
		}

		/**
		 * Adiciona um novo dispositivo a uma casa e cria o respetivo log.
		 * 
		 * @param houseName Nome da casa
		 * @param place     Divisão
		 * @throws IOException
		 */
		private static void addDevice(String houseName, String place) throws IOException {

			File input = new File(FICHEIRO_CASAS);
			File temp = new File("temp.txt");

			BufferedReader reader = new BufferedReader(new FileReader(input));
			BufferedWriter writer = new BufferedWriter(new FileWriter(temp));

			String line;

			while ((line = reader.readLine()) != null) {

				String[] parts = line.split(";", -1);

				if (parts[0].equals(houseName)) {

					String devices = parts[3].trim();

					String newDevice = nextDevice(devices, place);
					// isto ta aqui dentro, se falhar falham os tres mas sinceramente e para o
					// melhor
					addDeviceWithDefaultTime(houseName, newDevice);
					createDeviceLog(houseName, newDevice);
					if (!devices.isEmpty()) {
						devices = devices + ", " + newDevice;
					} else {
						devices = newDevice;
					}

					parts[3] = devices;

					line = String.join(";", parts);
				}

				writer.write(line);
				writer.newLine();
			}

			reader.close();
			writer.close();

			input.delete();
			temp.renameTo(input);
		}

		/**
		 * Atualiza o tempo de um dispositivo numa casa.
		 * 
		 * @param houseName Nome da casa
		 * @param place     Divisão/dispositivo
		 * @param newTime   Novo valor de tempo
		 * @throws IOException
		 */
		private static void updatePlaceTimeInHouse(String houseName, String place, int newTime)
				throws IOException {

			File inputFile = new File(FICHEIRO_ESTADOS);
			File tempFile = new File("temp.txt");

			BufferedReader reader = new BufferedReader(new FileReader(inputFile));
			BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));

			String line;

			while ((line = reader.readLine()) != null) {

				String[] parts = line.split(";");

				if (parts[0].trim().equals(houseName)) {

					String devices = parts[1];
					String[] devList = devices.split(",");
					StringBuilder updatedDevices = new StringBuilder();

					for (int i = 0; i < devList.length; i++) {

						String dev = devList[i].trim();
						String[] deviceParts = dev.split(":");

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

					line = houseName + "; " + updatedDevices.toString();
				}

				writer.write(line);
				writer.newLine();
			}

			reader.close();
			writer.close();

			inputFile.delete();
			tempFile.renameTo(inputFile);
		}

		/**
		 * Adiciona um dispositivo a uma casa com tempo inicial 0.
		 * 
		 * @param houseName Nome da casa
		 * @param device    Nome do dispositivo
		 * @throws IOException
		 */
		private static void addDeviceWithDefaultTime(String houseName, String device) throws IOException {
			File input = new File(FICHEIRO_ESTADOS);

			if (!input.exists()) {
				input.createNewFile();
			}

			File temp = new File("temp_times.txt");
			BufferedReader reader = new BufferedReader(new FileReader(input));
			BufferedWriter writer = new BufferedWriter(new FileWriter(temp));

			String line;
			boolean houseFound = false;

			while ((line = reader.readLine()) != null) {
				String[] parts = line.split(";", -1);
				String devices = null;

				if (parts[0].trim().equals(houseName)) {
					houseFound = true;
					if (parts.length > 1) {
						devices = parts[1].trim();
					} else
						devices = "";

					if (!devices.isEmpty()) {
						devices = devices + ", " + device + ":0";
					} else {
						devices = device + ":0";
					}

					line = parts[0] + ";" + devices;
				}

				writer.write(line);
				writer.newLine();
			}

			if (!houseFound) {
				writer.write(houseName + ";" + device + ":0");
				writer.newLine();
			}

			reader.close();
			writer.close();

			input.delete();
			temp.renameTo(input);
		}

		/**
		 * Cria o ficheiro de log para um dispositivo.
		 * 
		 * @param houseName Nome da casa
		 * @param device    Nome do dispositivo
		 * @throws IOException
		 */
		private static void createDeviceLog(String houseName, String device) throws IOException {
			File pastaLogs = new File(DIRETORIA_LOGS);
			if (!pastaLogs.exists()) {
				pastaLogs.mkdirs();
			}
			String fileName = DIRETORIA_LOGS + houseName + "_" + device + ".csv";
			File logFile = new File(fileName);

			logFile.createNewFile();
			// isto e mais safe mas nao faz sentido, mas isto e um server nao convem crachar
			// if (!logFile.exists()) {
			// logFile.createNewFile();
			// }
		}

		/**
		 * Adiciona uma entrada ao log de um dispositivo.
		 * 
		 * @param houseName Nome da casa
		 * @param device    Nome do dispositivo
		 * @param value     Valor a registar
		 * @throws IOException
		 */
		private static void addLogEntry(String houseName, String device, int value) throws IOException {

			String fileName = DIRETORIA_LOGS + houseName + "_" + device + ".csv";

			BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, true)); // append mode

			LocalDateTime agora = LocalDateTime.now();
			DateTimeFormatter formatador = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
			String dataHora = agora.format(formatador);

			writer.write(dataHora + ", " + value);
			writer.newLine();

			writer.close();
		}

		/**
		 * Verifica se o utilizador existe na casa (como proprietário ou com
		 * permissões).
		 * 
		 * @param line     Linha da casa
		 * @param username Nome do utilizador
		 * @return true se existir, false caso contrário
		 */
		private static boolean userExistInHouse(String line, String username) {
			if (isOwner(line, username)) {
				return true;
			}
			String[] parts = line.split(";", -1);

			String permissions = parts[2].trim();

			// Split users
			String[] users = permissions.split(",");

			for (String userPerm : users) {
				String[] userParts = userPerm.split(":");

				String user = userParts[0].trim();

				if (user.equals(username)) {

					return true;
				}
			}
			return false;
		}

		/**
		 * Devolve a última linha de um ficheiro.
		 * 
		 * @param fileName Nome do ficheiro
		 * @return Última linha, ou null se ficheiro não existir
		 * @throws IOException
		 */
		private static String getLastLine(String fileName) throws IOException {
			File f = new File(fileName);
			if (!f.exists())
				return null;

			String lastLine = null;
			BufferedReader reader = new BufferedReader(new FileReader(f));
			String line;

			while ((line = reader.readLine()) != null) {
				lastLine = line;
			}

			reader.close();
			return lastLine;
		}

		/**
		 * Envia ao cliente os estados mais recentes dos dispositivos a que tem acesso.
		 * 
		 * @param line      Linha da casa
		 * @param user      Nome do utilizador
		 * @param outStream Stream de saída para o cliente
		 * @throws IOException
		 */
		private static void sendRecentDeviceStatesFromLine(String line, String user, ObjectOutputStream outStream)
				throws IOException {
			Boolean isOwner = isOwner(line, user);
			String[] parts = line.split(";", -1);
			String houseName = parts[0].trim();
			String permissions = parts[2].trim();
			String devicesStr = parts[3].trim();
			boolean allAccess = false;
			Set<Character> permissionSet = new HashSet<>();
			String[] users = permissions.split(",");
			if (!permissions.isEmpty() && !isOwner) {
				for (String u : users) {
					String[] uparts = u.split(":");
					String uname = uparts[0].trim();
					String perms = uparts[1].trim();

					if (uname.equals(user)) {
						if (perms.equals("all")) {
							allAccess = true;
							break;
						} else {
							String[] permss = permissions.split("\\|");

							for (String p : permss) {
								permissionSet.add(p.charAt(0));
							}
						}
					}
				}
			}
			List<String> devices = new ArrayList<>();

			if (!devicesStr.isEmpty()) {
				// Get devices the user can access
				String[] devList = devicesStr.split(",");
				for (String d : devList) {
					d = d.trim();
					if (allAccess || isOwner(line, user)) {
						devices.add(d);
					} else {
						char devicePrefix = d.charAt(0);

						if (permissionSet.contains(devicePrefix)) {
							devices.add(d);
						}
					}
				}
			}
			// Read last value of each device
			boolean hasData = false;
			File summary = new File(houseName + "_" + user + "_summary.txt");
			BufferedWriter writer = new BufferedWriter(new FileWriter(summary));

			for (String device : devices) {
				String logFile = DIRETORIA_LOGS + houseName + "_" + device + ".csv";
				String lastLine = getLastLine(logFile);

				if (lastLine != null && !lastLine.trim().isEmpty()) {
					writer.write(device + ":" + lastLine);
					writer.newLine();
					hasData = true;
				}
			}

			writer.close();

			if (!hasData) {
				outStream.writeObject("NODATA");
			} else {
				outStream.writeObject("OK");
				outStream.flush();
				FileInputStream fileIn = new FileInputStream(summary);

				long fileSize = summary.length();

				outStream.writeLong(fileSize);
				outStream.flush();

				byte[] buffer = new byte[4096];
				int bytesRead;

				while ((bytesRead = fileIn.read(buffer)) > 0) {
					outStream.write(buffer, 0, bytesRead);
				}

				outStream.flush();
				fileIn.close();
				summary.delete();
			}
		}

		/**
		 * Verifica se um dispositivo existe numa casa.
		 * 
		 * @param houseLine Linha da casa
		 * @param device    Nome do dispositivo
		 * @return true se existir, false caso contrário
		 */
		private static boolean deviceExistsInHouse(String houseLine, String device) {

			String[] parts = houseLine.split(";", -1);
			String devicesStr = parts[3].trim();
			String[] devList = devicesStr.split(",");

			for (String d : devList) {
				if (d.trim().equals(device)) {
					return true;
				}
			}
			return false;
		}

		/**
		 * Envia o ficheiro de log de um dispositivo ao cliente.
		 * 
		 * @param houseName Nome da casa
		 * @param device    Nome do dispositivo
		 * @param outStream Stream de saída para o cliente
		 * @throws IOException
		 */
		private static void sendLog(String houseName, String device, ObjectOutputStream outStream) throws IOException {
			outStream.writeObject("OK");
			outStream.flush();
			File log = new File(DIRETORIA_LOGS + houseName + "_" + device + ".csv");
			FileInputStream fileIn = new FileInputStream(log);

			long fileSize = log.length();

			outStream.writeLong(fileSize);
			outStream.flush();

			byte[] buffer = new byte[4096];
			int bytesRead;

			while ((bytesRead = fileIn.read(buffer)) > 0) {
				outStream.write(buffer, 0, bytesRead);
			}

			outStream.flush();
			fileIn.close();
		}

	}
}