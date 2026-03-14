
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
import java.util.List;
import java.util.Scanner;
import java.util.Set;

//Servidor myServer

public class SpertaServer {
	private static final int MAX_TENTATIVAS = 3;
	private static final String FICHEIRO_USERS = "users.txt";
	private static final String FICHEIRO_CASAS = "casas.txt";
	private static final String FICHEIRO_ESTADOS = "estados.txt";
	private static final String DIRETORIA_LOGS = "logs/";
	private static final Set<String> VALID_PERMS = Set.of("E", "G", "L", "M", "P", "S", "all");
	private static final Set<String> VALID_SECS = Set.of("E", "G", "L", "M", "P", "S");

	public static void main(String[] args) {
		System.out.println("servidor: main");
		SpertaServer server = new SpertaServer();
		server.startServer();
	}

	public void startServer() {
		ServerSocket sSoc = null;

		try {
			sSoc = new ServerSocket(23456);
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

	// Threads utilizadas para comunicacao com os clientes
	class ServerThread extends Thread {

		private Socket socket = null;

		ServerThread(Socket inSoc) {
			socket = inSoc;
			System.out.println("thread do server para cada cliente");
		}

		public void run() {
			try {
				ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream());
				ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream());

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

		private static boolean autenticarCliente(String user, ObjectInputStream inStream,
				ObjectOutputStream outStream) throws IOException, ClassNotFoundException {
			int tentativas = MAX_TENTATIVAS;

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
				FileWriter fw = new FileWriter(file, true);
				fw.write(user + ":" + passwd + "\n");
				fw.close();
				outStream.writeObject("OK-NEW-USER");
				outStream.flush();
				return true;
			}

			// autenticação
			while (tentativas > 0) {
				passwd = (String) inStream.readObject();
				if (correctPassword.equals(passwd)) {
					outStream.writeObject("ATTESTATION OK");
					outStream.flush();
					return true;
				} else {
					tentativas--;
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

			}
		}

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

		private static void adicionarUtilizador(String user, ObjectOutputStream outStream, String[] parts)
				throws IOException, ClassNotFoundException {

			// teste de input
			// divisao tem de ser uma das default
			if (parts.length != 4 || !VALID_PERMS.contains(parts[3])) {
				outStream.writeObject("NOK");
				return;
			}

			String line = findHouseLine(FICHEIRO_CASAS, parts[1]);
			if (line != null) {
				outStream.writeObject("NOHM");
			} else if (!userExists(parts[2])) {
				outStream.writeObject("NOUSER");
			} else if (!isOwner(line, user)) {
				outStream.writeObject("NOPERM");
				// caso base
			} else {
				// esta funcao e bem grande e nao sei se funciona
				updatePermissions(parts[2], parts[3], parts[4]);
				outStream.writeObject("OK");
			}

		}

		private static void registarDispositivo(String user, ObjectOutputStream outStream, String[] parts)
				throws IOException, ClassNotFoundException {
			// teste de input
			// divisao tem de ser uma das default
			if (parts.length != 3 || !VALID_SECS.contains(parts[3])) {
				outStream.writeObject("NOK");
				return;
			}
			String line = findHouseLine(FICHEIRO_CASAS, parts[1]);
			if (line != null) {
				outStream.writeObject("NOHM");
			} else if (!isOwner(line, user)) {
				outStream.writeObject("NOPERM");
			} else {
				addDevice(parts[1], parts[2]);
				outStream.writeObject("OK");
			}
		}

		private static void envioValor(String user, ObjectOutputStream outStream, String[] parts)
				throws IOException, ClassNotFoundException {
			// teste de input
			if (parts.length != 4) {
				outStream.writeObject("NOK");
				return;
			}

			int value;
			try {
				value = Integer.parseInt(parts[3]);
			} catch (NumberFormatException e) {
				outStream.writeObject("NOK");
				return;
			}
			// ]1, 600[
			if (value <= 1 || value > 600) {
				outStream.writeObject("NOK");
				return;
			}

			String line = findHouseLine(FICHEIRO_CASAS, parts[1]);
			if (line != null) {
				outStream.writeObject("NOHM");
			} else if (!hasPermission(line, user, parts[2].substring(0, 1))) {
				outStream.writeObject("NOPERM");
			} else {
				updatePlaceTimeInHouse(parts[1], parts[2], value);
				outStream.writeObject("OK");
			}
		}

		private static void receberTemp(String user, ObjectOutputStream outStream, String[] parts)
				throws IOException, ClassNotFoundException {
			// teste de input
			if (parts.length != 2) {
				outStream.writeObject("NOK");
				return;
			}
			String line = findHouseLine(FICHEIRO_CASAS, parts[1]);
			if (line != null) {
				outStream.writeObject("NOHM");
			} else if (!userExistInHouse(line, user)) {
				outStream.writeObject("NOPERM");
			} else {
				sendRecentDeviceStatesFromLine(line, user, outStream);
			}

		}

		private static void receberHistorico(String user, ObjectOutputStream outStream, String[] parts)
				throws IOException, ClassNotFoundException {
			// teste de input
			if (parts.length != 3) {
				outStream.writeObject("NOK");
				return;
			}
			String line = findHouseLine(FICHEIRO_CASAS, parts[1]);
			if (line != null) {
				outStream.writeObject("NOHM");
			} else if (!hasPermission(line, user, parts[2].substring(0, 1))) {
				outStream.writeObject("NOPERM");
			} else if (deviceExistsInHouse(line, parts[2])) {
				outStream.writeObject("NOD");
			} else {
				sendLog(parts[1], parts[2], outStream);
			}
		}

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

		private static boolean isOwner(String line, String username) {
			String[] parts = line.split(";");
			String owner = parts[1].trim();
			return owner.equals(username);
		}

		private static boolean hasPermission(String line, String username, String place) {
			if (isOwner(line, username)) {
				return true;
			}
			String[] parts = line.split(";");

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

		private static String addPermission(String permissions, String user, String newPerm) {

			String[] users = permissions.split(",");
			StringBuilder result = new StringBuilder();

			for (int i = 0; i < users.length; i++) {

				String[] parts = users[i].split(":");
				String currentUser = parts[0].trim();
				String currentPerms = parts[1].trim();

				if (currentUser.equals(user)) {
					if (currentPerms.equals("all")) {
						result.append(users[i]);
					} else {
						String[] perms = currentPerms.split("\\|");
						boolean exists = false;

						for (String p : perms) {
							if (p.equals(newPerm)) {
								exists = true;
								break;
							}
						}

						if (!exists) {
							currentPerms = currentPerms + "|" + newPerm;
						}

						result.append(user).append(":").append(currentPerms);
					}
				} else {
					result.append(users[i].trim());
				}

				if (i < users.length - 1) {
					result.append(",");
				}
			}

			return result.toString();
		}

		public static void updatePermissions(String user, String houseName, String newPermissions)
				throws IOException {

			File inputFile = new File(FICHEIRO_CASAS);
			File tempFile = new File("temp.txt");

			BufferedReader reader = new BufferedReader(new FileReader(inputFile));
			BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));

			String line;

			while ((line = reader.readLine()) != null) {

				String[] parts = line.split(";");

				if (parts[0].equals(houseName)) {
					// modify permissions
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

		private static void addDevice(String houseName, String place) throws IOException {

			File input = new File(FICHEIRO_CASAS);
			File temp = new File("temp.txt");

			BufferedReader reader = new BufferedReader(new FileReader(input));
			BufferedWriter writer = new BufferedWriter(new FileWriter(temp));

			String line;

			while ((line = reader.readLine()) != null) {

				String[] parts = line.split(";");

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

		private static void addDeviceWithDefaultTime(String houseName, String device) throws IOException {

			File input = new File(FICHEIRO_ESTADOS);
			File temp = new File("temp_times.txt");

			BufferedReader reader = new BufferedReader(new FileReader(input));
			BufferedWriter writer = new BufferedWriter(new FileWriter(temp));

			String line;

			while ((line = reader.readLine()) != null) {

				String[] parts = line.split(";");

				if (parts[0].trim().equals(houseName)) {

					String devices = parts[1].trim();

					if (!devices.isEmpty()) {
						devices = devices + ", " + device + ":0";
					} else {
						devices = device + ":0";
					}

					line = parts[0] + "; " + devices;
				}

				writer.write(line);
				writer.newLine();
			}

			reader.close();
			writer.close();

			input.delete();
			temp.renameTo(input);
		}

		private static void createDeviceLog(String houseName, String device) throws IOException {

			String fileName = DIRETORIA_LOGS + houseName + "_" + device + ".csv";
			File logFile = new File(fileName);

			logFile.createNewFile();
			// isto e mais safe mas nao faz sentido, mas isto e um server nao convem crachar
			// if (!logFile.exists()) {
			// logFile.createNewFile();
			// }
		}

		private static void addLogEntry(String houseName, String device, int value) throws IOException {

			String fileName = DIRETORIA_LOGS + houseName + "_" + device + ".csv";

			BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, true)); // append mode

			writer.write(value);
			writer.newLine();

			writer.close();
		}

		private static boolean userExistInHouse(String line, String username) {
			if (isOwner(line, username)) {
				return true;
			}
			String[] parts = line.split(";");

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

		private static void sendRecentDeviceStatesFromLine(String line, String user, ObjectOutputStream outStream)
				throws IOException {

			String[] parts = line.split(";");
			String houseName = parts[0].trim();
			String permissions = parts[2].trim();
			String devicesStr = parts[3].trim();

			List<String> devices = new ArrayList<>();
			boolean allAccess = false;

			String[] users = permissions.split(",");
			for (String u : users) {
				String[] uparts = u.split(":");
				String uname = uparts[0].trim();
				String perms = uparts[1].trim();

				if (uname.equals(user)) {
					if (perms.equals("all")) {
						allAccess = true;
						break;
					}
				}
			}

			// Get devices the user can access
			String[] devList = devicesStr.split(",");
			for (String d : devList) {
				d = d.trim();
				if (allAccess || isOwner(line, user)) {
					devices.add(d);
				} else {
					for (String u : users) {
						String[] uparts = u.split(":");
						String uname = uparts[0].trim();
						String perms = uparts[1].trim();

						if (uname.equals(user)) {
							String[] userPerms = perms.split("\\|");
							for (String p : userPerms) {
								if (d.startsWith(p)) {
									devices.add(d);
								}
							}
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
			}
		}

		private static boolean deviceExistsInHouse(String houseLine, String device) {

			String[] parts = houseLine.split(";");

			String devicesStr = parts[3].trim();
			String[] devList = devicesStr.split(",");

			for (String d : devList) {
				if (d.trim().equals(device)) {
					return true;
				}
			}
			return false;
		}

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