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
				String passwd = null;
				boolean correctPass = false;

					try {
						while(!correctPass) {

							user = (String) inStream.readObject();
							passwd = (String) inStream.readObject();
							File file = new File("C:\\Users\\ritai\\OneDrive\\Documentos\\GitHub\\SegC\\SpertaServer\\data\\users.txt");

							if (!file.exists()) {
								file.createNewFile();
							}

							boolean exists = false;
							Scanner sc = new Scanner(file);

							while (sc.hasNextLine()) {
								String line = sc.nextLine();
								String[] parts = line.split(":");

								if (parts[0].equals(user)) {
									exists = true;
									if (parts[1].equals(passwd)) {
										correctPass = true;
									}
									break;
								}
							}

							sc.close();

							if (!exists) {
								FileWriter fw = new FileWriter(file, true);
								fw.write(user + ":" + passwd + "\n");
								fw.close();
								//outStream.writeObject("NEW");
								outStream.flush();

								long fileSize = inStream.readLong();

								FileOutputStream fileOut = new FileOutputStream(user + ".dat");

								byte[] buffer = new byte[4096];
								int bytesRead;
								long totalRead = 0;

								while (totalRead < fileSize &&
										(bytesRead = inStream.read(buffer, 0,
												(int) Math.min(buffer.length, fileSize - totalRead))) > 0) {

									fileOut.write(buffer, 0, bytesRead);
									totalRead += bytesRead;
								}

								fileOut.close();
								System.out.println("Ficheiro recebido com sucesso!");

							}
							outStream.writeObject(correctPass);
						}
					

					if (correctPass) {
						outStream.writeObject("SEND");
						outStream.flush();

						File userFile = new File(user + ".dat");
						FileInputStream fileIn = new FileInputStream(userFile);

						long fileSize = userFile.length();

						outStream.writeLong(fileSize);
						outStream.flush();

						byte[] buffer = new byte[4096];
						int bytesRead;

						while ((bytesRead = fileIn.read(buffer)) > 0) {
							outStream.write(buffer, 0, bytesRead);
						}

						outStream.flush();
						fileIn.close();

						System.out.println("Ficheiro enviado com sucesso!");
					} else {
						outStream.writeObject("INVALID");
						outStream.flush();
					}

					// System.out.println("thread: depois de receber a password e o user");
				} catch (ClassNotFoundException e1) {
					e1.printStackTrace();
				}

				outStream.close();
				inStream.close();

				socket.close();

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}