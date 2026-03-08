package SpertaClient.src.client;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

public class myClient {
        public static void main(String[] args) {
            myClient client = new myClient();
            Scanner sc = new Scanner(System.in);
            String user = "";
            String pass = "";

            while (user.trim().isEmpty()) {
                System.out.print("Digite o seu username: ");
                user = sc.nextLine();
            }

            while (pass.trim().isEmpty()) {
                System.out.print("Digite a sua passe: ");
                pass = sc.nextLine();
            }

            client.startClient(user, pass);
            sc.close();
        }

    public void startClient(String user, String pass) {
        Socket clientSocket = null;

        try {
        clientSocket = new Socket("127.0.0.1", 23456);
        System.out.println("Socket iniciada!");

        ObjectOutputStream outStream = new ObjectOutputStream(clientSocket.getOutputStream());
		ObjectInputStream inStream = new ObjectInputStream(clientSocket.getInputStream());

        outStream.writeObject(user);
        outStream.writeObject(pass);

        Boolean sucesso = (Boolean) inStream.readObject();
        if(sucesso) {
            System.out.println("Login efetuado com sucesso!");

            File fileSend = new File("teste.txt");
            if(fileSend.exists()) {
                outStream.writeObject(fileSend.getName());
                outStream.writeObject(fileSend.length());

                FileInputStream fileIn = new FileInputStream(fileSend);
                byte[] buffer = new byte[1024];
                int bytesRead;

                while ((bytesRead = fileIn.read(buffer)) != -1) {
                        outStream.write(buffer, 0, bytesRead);
                    }

                outStream.flush(); // Garante que o último bocado de bytes é empurrado pelo tubo
                fileIn.close();
                System.out.println("Ficheiro enviado com sucesso!");
            } else {
                System.out.println("O ficheiro não existe!");
            }

            try {
                String fileName = (String) inStream.readObject();
                long fileSize = (Long) inStream.readObject();
                FileOutputStream fileOut = new FileOutputStream("returned_" + fileName);
                byte[] buffer = new byte[1024];
                int bytesRead;
                long totalLido = 0;

                while (totalLido < fileSize && (bytesRead = inStream.read(buffer, 0, (int)Math.min(buffer.length, fileSize - totalLido))) != -1) {
                    fileOut.write(buffer, 0, bytesRead);
                    totalLido += bytesRead;
                }

                fileOut.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else
            System.out.println("Erro ao efetuar login!");

        outStream.close();
        inStream.close();
        //clientSocket.close();

        } catch (Exception e) {
            e.printStackTrace(); 
        }


    }
}
