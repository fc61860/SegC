package SpertaClient.src.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

public class myClient {

        public String user;
        public String pass;
        public static void main(String[] args) {
            myClient client = new myClient();

            client.startClient();
        }

    public void startClient() {
        Socket clientSocket = null;

        try {
        clientSocket = new Socket("127.0.0.1", 23456);
        System.out.println("Socket iniciada!");

        ObjectOutputStream outStream = new ObjectOutputStream(clientSocket.getOutputStream());
		ObjectInputStream inStream = new ObjectInputStream(clientSocket.getInputStream());
        Boolean sucesso = false;

        Scanner sc = new Scanner(System.in);

        while(!sucesso) {
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

            outStream.writeObject(user);
            outStream.writeObject(pass);
            sucesso = (Boolean) inStream.readObject();

            if(!sucesso) {
                System.out.println("Password incorreta!");
            }

        }
                
        if(sucesso) {
            System.out.println("Login efetuado com sucesso!");
            Boolean running = true;
            
            mostrarMenu();

            // ler os comandos do user
            while(running) {
                String input = sc.nextLine().trim();

                if(input.isEmpty()) continue;

                String[] parts = input.split(" ");
                String order = parts[0].toUpperCase();
                
            }

            // File fileSend = new File("SpertaServer/data/teste.txt");
            // if(fileSend.exists()) {
            //     outStream.writeObject(fileSend.getName());
            //     outStream.writeObject(fileSend.length());

            //     FileInputStream fileIn = new FileInputStream(fileSend);
            //     byte[] buffer = new byte[1024];
            //     int bytesRead;

            //     while ((bytesRead = fileIn.read(buffer)) != -1) {
            //             outStream.write(buffer, 0, bytesRead);
            //         }

            //     outStream.flush(); // Garante que o último bocado de bytes é empurrado pelo tubo
            //     fileIn.close();
            //     System.out.println("Ficheiro enviado com sucesso!");
            // } else {
            //     System.out.println("O ficheiro não existe!");
            // }

            // try {
            //     String fileName = (String) inStream.readObject();
            //     long fileSize = (Long) inStream.readObject();
            //     FileOutputStream fileOut = new FileOutputStream("returned_" + fileName);
            //     byte[] buffer = new byte[1024];
            //     int bytesRead;
            //     long totalLido = 0;

            //     while (totalLido < fileSize && (bytesRead = inStream.read(buffer, 0, (int)Math.min(buffer.length, fileSize - totalLido))) != -1) {
            //         fileOut.write(buffer, 0, bytesRead);
            //         totalLido += bytesRead;
            //     }

            //     fileOut.close();
            // } catch (Exception e) {
            //     e.printStackTrace();
            // }

            

        } else
            System.out.println("Erro ao efetuar login!");


        System.out.println("À espera da proxima ordem...");
        Scanner waitScanner = new Scanner(System.in);
        waitScanner.nextLine();

        outStream.close();
        inStream.close();
        //clientSocket.close();

        } catch (Exception e) {
            e.printStackTrace(); 
        }


    }

    private void mostrarMenu() {
        System.out.println("\n=========================================================================");
        System.out.println("                            MENU DE COMANDOS                             ");
        System.out.println("=========================================================================");
        System.out.println(" CREATE <hm>         | Criar casa <hm> (Ficas como Owner)");
        System.out.println(" ADD <user> <hm> <s> | Adicionar <user> à casa <hm>, secção <s>");
        System.out.println(" RD <hm> <s>         | Registar um Dispositivo na casa <hm>, secção <s>");
        System.out.println(" EC <hm> <d> <int>   | Enviar valor <int> ao dispositivo <d> da casa <hm>");
        System.out.println(" RT <hm>             | Receber último estado dos dispositivos da casa <hm>");
        System.out.println(" RH <hm> <d>         | Receber Histórico (.csv) do dispositivo <d>");
        System.out.println(" EXIT                | Sair da aplicação");
        System.out.println("=========================================================================");
        System.out.print("Introduza um comando: ");
    }
}
