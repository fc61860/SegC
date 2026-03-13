package SpertaClient.src.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

public class SpertaClient {

        public String user;
        public String pass;
        public static void main(String[] args) {
            SpertaClient client = new SpertaClient();

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
                
        
        System.out.println("Login efetuado com sucesso!");
        Boolean running = true;
        
        mostrarMenu();

        // ler os comandos do user
        while(running) {
            try {
                System.out.println("Comando: ");
                String input = sc.nextLine().trim();

                if(input.isEmpty()) continue;

                String[] parts = input.split(" ");
                String command = parts[0].toUpperCase();

                switch(command) {
                    case "CREATE":
                        if(parts.length == 2 && parts[1].matches("^[a-zA-Z][a-zA-Z0-9]*$")) {
                            String home = parts[1];

                            try {
                                outStream.writeObject(home);

                                String answer = (String) inStream.readObject();
                                System.out.println("Server: " + answer);
                            } catch (Exception e) {
                                System.out.println("Erro ao comunicar com o servidor.");
                            }
                            
                            //
                        } else
                            System.out.println("Formato incorreto. Tente: CREATE <hm>");
                            System.out.println("O nome da casa deve começar com uma letra!");
                        break;

                    case "ADD":
                        if(parts.length == 4) {
                            String user = parts[1];
                            String home = parts[2];
                            String sec = parts[3];
                            outStream.writeObject(user);
                            outStream.writeObject(home);
                            outStream.writeObject(sec);
                            //
                        } else
                            System.out.println("Formato incorreto. Tente: ADD <user> <hm> <s>");
                        break;

                    case "RD":
                        if(parts.length == 3) {
                            String home = parts[1];
                            String sec = parts[2];
                            outStream.writeObject(home);
                            outStream.writeObject(sec);
                            //
                        } else
                            System.out.println("Formato incorreto. Tente: RD <hm> <sec>");
                        break;

                    case "EC":
                        if(parts.length == 4 && parts[3].matches("^[0-9]+$")) {
                            String home = parts[1];
                            String disp = parts[2];
                            int valor = Integer.parseInt(parts[3]);
                            
                            if(valor == 0 || valor == 1 | (valor > 1 && valor <= 600) ) {
                                outStream.writeObject(home);
                                outStream.writeObject(disp);
                                outStream.writeInt(valor);
                            } else
                                System.out.println("Valores  possíveis de <int>: 0(desligar), 1(ligar), ]1..600] (ligar por x minutos, até 600). ");
                            
                            //
                        } else
                            System.out.println("Formato incorreto. Tente: EC <hm> <d> <int>");
                        break;

                    case "RT":
                        if(parts.length == 2) {
                            String home = parts[1];
                            outStream.writeObject(home);
                            //
                        } else
                            System.out.println("Formato incorreto. Tente: RT <hm>");
                        break;

                    case "RH":
                        if(parts.length == 3) {
                            String home = parts[1];
                            String disp = parts[2];
                            outStream.writeObject(home);
                            outStream.writeObject(disp);
                            //
                        } else
                            System.out.println("Formato incorreto. Tente: RH <hm> <d>");
                        break;

                    default:
                            System.out.println("Comando desconhecido! Tente novamente.");
                            break;

                }
            }catch (Exception e) { // Ctrl C
                System.out.println("O cliente desligou-se (Ligação terminada).");
                break; 
            }
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
        // System.out.println("À espera da proxima ordem...");
        // Scanner waitScanner = new Scanner(System.in);
        // waitScanner.nextLine();

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
        System.out.println("                      (Pressione Ctrl+C para sair)                       ");
        System.out.println("=========================================================================");
        System.out.println(" CREATE <hm>         | Criar casa <hm> (Ficas como Owner)");
        System.out.println(" ADD <user> <hm> <s> | Adicionar <user> à casa <hm>, secção <s>");
        System.out.println(" RD <hm> <s>         | Registar um Dispositivo na casa <hm>, secção <s>");
        System.out.println(" EC <hm> <d> <int>   | Enviar valor <int> ao dispositivo <d> da casa <hm>");
        System.out.println(" RT <hm>             | Receber último estado dos dispositivos da casa <hm>");
        System.out.println(" RH <hm> <d>         | Receber Histórico (.csv) do dispositivo <d>");
        System.out.println("=========================================================================");
        }
}
