import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class SpertaServer{
	public static void main(String[] args) {
		System.out.println("servidor: main");
		SpertaServer server = new SpertaServer();
		server.startServer();
	}

	public void startServer (){
		ServerSocket sSoc = null;
        
		try {
			sSoc = new ServerSocket(23456);
			
		} catch (IOException e) {
			System.err.println(e.getMessage());
			System.exit(-1);
		}
         
		while(true) {
			try {
				Socket inSoc = sSoc.accept();
				ServerThread newServerThread = new ServerThread(inSoc);
				newServerThread.start();
				//sSoc.close();
		    }
		    catch (IOException e) {
		        e.printStackTrace();
		    }
		    
		}
	}

	//Threads utilizadas para comunicacao com os clientes
	class ServerThread extends Thread {
		private Socket socket = null;

		ServerThread(Socket inSoc) {
			socket = inSoc;
			System.out.println("Novo cliente:");
			System.out.println("thread do server para cada cliente");
		}

		public void run(){
			try {
				ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream());
				ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream());

				String user = null;
				String passwd = null;
				Boolean autenticated = false;
				Boolean exists = false;
			
				try {
					user = (String)inStream.readObject();
					passwd = (String)inStream.readObject();
					System.out.println("thread: depois de receber a password e o user");
					
				}catch (ClassNotFoundException e1) {
					e1.printStackTrace();
				}

				if(user != null && passwd != null) {
					File file = new File("users.txt");
					FileOutputStream output = new FileOutputStream(file, true);
					FileInputStream input = new FileInputStream(file);
					Scanner sc = new Scanner(file);

					while (sc.hasNextLine()) {
						String user_file_line = sc.nextLine();
						String[] user_file = user_file_line.split(":", 0);

						if(user.equals(user_file[0])) {
							exists = true;
							if(passwd.equals(user_file[1])) {
								autenticated = true;
								System.out.println("User autenticado!");
							}
							else {
								autenticated = false;
								System.out.println("Password errada!");
								}
							}
						}
					
					if(!exists) {
						String newUser = "\n" + user + ":" + passwd;
						output.write(newUser.getBytes());
						System.out.println("Novo user registado com sucesso!");
						autenticated = true;
					}
					sc.close();
					input.close();
					output.close();
				}
				outStream.writeObject(autenticated);

				if(autenticated) {
					System.out.println("A espera do envio do ficheiro!");

					try{
						String fileName = (String) inStream.readObject();
						Long fileSize = (Long) inStream.readObject();

						FileOutputStream fileOut = new FileOutputStream("server_" + fileName);
						byte[] buffer = new byte[1024];
						int bytesRead;
						long totalLido = 0;

						while (totalLido < fileSize && (bytesRead = inStream.read(buffer, 0, (int)Math.min(buffer.length, fileSize - totalLido))) != -1) {
							fileOut.write(buffer, 0, bytesRead);
							totalLido += bytesRead;
						}

						fileOut.close();
						System.out.println("Ficheiro recebido e guardado como 'server_" + fileName + "' com sucesso!");

						FileInputStream fileIn = new FileInputStream("server_" + fileName);
						outStream.writeObject(fileName);
						outStream.writeObject(fileSize);
						byte[] buffer1 = new byte[1024];
						int bytesRead1;

						while ((bytesRead1 = fileIn.read(buffer1)) != -1) {
								outStream.write(buffer1, 0, bytesRead1);
							}

                		outStream.flush(); // Garante que o último bocado de bytes é empurrado pelo tubo
						
						fileIn.close();
						System.out.println("Ficheiro enviado de volta para o cliente com sucesso!");
					} catch (ClassNotFoundException e1) {
						e1.printStackTrace();
					}
				}

				outStream.close();
				inStream.close();
				socket.close();
			}

				catch (IOException e) {
				e.printStackTrace();
				}
			} 
		}
	}

