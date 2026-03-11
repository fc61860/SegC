
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

//Servidor myServer

public class SpertaServer {
	private static final int MAX_TENTATIVAS = 3;
	private static final String FICHEIRO_USERS = "users.txt";

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
							processCommand(inStream, outStream);

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

		private void processCommand(ObjectInputStream inStream, ObjectOutputStream outStream)
				throws IOException, ClassNotFoundException {

			String message = (String) inStream.readObject();

			String[] parts = message.split(" ");
			String command = parts[0];
			switch (command) {

				case "CREATE":
					criarCasa(outStream, parts);
					break;
				case "ADD":
					adicionarUtilizador(inStream, outStream, parts);
					break;
				case "RD":
					registarDispositivo(inStream, outStream, parts);
					break;
				case "EC":
					envioValor(inStream, outStream, parts);
					break;
				case "RT":
					receberTemp(inStream, outStream, parts);
					break;
				case "RH":
					receberHistorico(inStream, outStream, parts);
					break;

			}
		}
	}
}