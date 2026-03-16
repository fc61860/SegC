package SpertaClient.src.client;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

//Cliente SpertaClient

public class SpertaClient {

        public String user;
        public String pass;
        public static void main(String[] args) {
            if (args.length != 3) {
            System.out.println("Erro!");
            System.out.println("Formato exigido: SpertaClient <serverAddress> <user-id> <password>");
            System.exit(-1);
            }

            String serverAddress = args[0];
            String user = args[1];
            String pass = args[2];

            String ip = serverAddress;
            int port = 22345; // Default

            if (serverAddress.contains(":")) {
                String[] parts = serverAddress.split(":");
                ip = parts[0];
                port = Integer.parseInt(parts[1]);
            }

            SpertaClient client = new SpertaClient();
            client.startClient(ip, port, user, pass);
        }

    public void startClient(String ip, int port, String user, String pass) {
        Socket clientSocket = null;

        try {
        clientSocket = new Socket(ip, port);
        System.out.println("Socket iniciada!");

        ObjectOutputStream outStream = new ObjectOutputStream(clientSocket.getOutputStream());
		ObjectInputStream inStream = new ObjectInputStream(clientSocket.getInputStream());
        Boolean sucesso = false;

        Scanner sc = new Scanner(System.in); // Este scanner não se fecha para evitar conflitos com o teclado.

        String currentUser = user;
        String currentPass = pass;
        Boolean firstTry = true;
        int trys = 1;

        outStream.writeObject(currentUser);
        outStream.flush();

        while(!sucesso && trys <= 3) {
            if (!firstTry) {
                    System.out.println("Tentativa " + trys + "/3:");
                    System.out.print("Password incorreta! Digite nova password para o user '" + currentUser + "': ");
                    currentPass = sc.nextLine();
                }

            outStream.writeObject(currentPass);
            outStream.flush();
            String respostaAuth = (String) inStream.readObject(); 

            if(respostaAuth.equals("OK-NEW-USER") || respostaAuth.equals("OK-USER") || respostaAuth.equals("ATTESTATION OK")) {
                sucesso = true;
            } else {
                System.out.println("Resposta do Servidor: " + respostaAuth);
            }

            firstTry = false;
            trys++;
        }
        if (trys > 3) {
            System.out.println("Tentativas esgotadas! A encerrar...");
            clientSocket.close();
            return;
        }  
        
        System.out.println("Login efetuado com sucesso!");
        Boolean running = true;
        
        showMenu();

        // ler os comandos do user
        while(running) {
            try {
                System.out.println("Comando: ");
                String input = sc.nextLine().trim();

                if(input.isEmpty()) continue;

                String[] parts = input.split(" ");
                String command = parts[0].toUpperCase();

                processCommand(command, parts, input, outStream, inStream);
                
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

    private void showMenu() {
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

    private void processCommand(String command, String[] parts, String input, ObjectOutputStream outStream, ObjectInputStream inStream) {
        switch(command) {
                    case "CREATE":
                        if(parts.length == 2) {
                            //String home = parts[1];

                            try {
                                outStream.writeObject(input);
                                outStream.flush();
                                //outStream.writeObject(home);

                                String answer = (String) inStream.readObject();
                                System.out.println("Server: " + answer);
                            } catch (Exception e) {
                                System.out.println("Erro ao comunicar com o servidor.");
                            }
                            
                            //
                        } else
                            System.out.println("Formato incorreto. Tente: CREATE <hm>");
                            break;

                    case "ADD":
                        if(parts.length == 4) {
                            try {
                                outStream.writeObject(input);
                                outStream.flush();

                                String answer = (String) inStream.readObject();
                                System.out.println("Server: " + answer);
                            } catch (Exception e) {
                                System.out.println("Erro ao comunicar com o servidor.");
                            }
                            //
                        } else
                            System.out.println("Formato incorreto. Tente: ADD <user> <hm> <s>");
                        break;

                    case "RD":
                        if(parts.length == 3) {
                            try {
                                outStream.writeObject(input);
                                outStream.flush();

                                String answer = (String) inStream.readObject();
                                System.out.println("Server: " + answer);
                            } catch (Exception e) {
                                System.out.println("Erro ao comunicar com o servidor.");
                            }
                            
                            //
                        } else
                            System.out.println("Formato incorreto. Tente: RD <hm> <sec>");
                        break;

                    case "EC":
                        if(parts.length == 4 && parts[3].matches("^[0-9]+$")) {
                            int valor = Integer.parseInt(parts[3]);
                            
                            if(valor == 0 || valor == 1 | (valor > 1 && valor <= 600) ) {
                                try {
                                    outStream.writeObject(input);
                                    outStream.flush();

                                    String answer = (String) inStream.readObject();
                                    System.out.println("Server: " + answer);
                                } catch (Exception e) {
                                    System.out.println("Erro ao comunicar com o servidor.");
                                }
                            } else
                                System.out.println("Valores  possíveis de <int>: 0(desligar), 1(ligar), ]1..600] (ligar por x minutos, até 600). ");
                            
                            //
                        } else
                            System.out.println("Formato incorreto. Tente: EC <hm> <d> <int>");
                        break;

                    case "RT":
                        if(parts.length == 2) {
                            String home = parts[1];
                            try {
                                outStream.writeObject(input);
                                outStream.flush();

                                File pastaSummaries = new File("summaries");
                                if (!pastaSummaries.exists()) {
                                    pastaSummaries.mkdirs();
                                }

                                String nomeFicheiro = "summaries/cliente_summary_" + home + ".txt";
                                processFile(inStream, nomeFicheiro);

                            } catch (Exception e) {
                                System.out.println("Erro ao comunicar com o servidor.");
                            }
                            //
                        } else
                            System.out.println("Formato incorreto. Tente: RT <hm>");
                        break;

                    case "RH":
                        if(parts.length == 3) {
                            String home = parts[1];
                            String disp = parts[2];
                            try {
                                outStream.writeObject(input);
                                outStream.flush();

                                String nomeFicheiro = "cliente_log_" + home + "_" + disp + ".txt";
                                processFile(inStream, nomeFicheiro);

                            } catch (Exception e) {
                                System.out.println("Erro ao comunicar com o servidor.");
                            }
                            //
                        } else
                            System.out.println("Formato incorreto. Tente: RH <hm> <d>");
                        break;

                    default:
                            System.out.println("Comando desconhecido! Tente novamente.");
                            break;

                }
    }

    private void processFile(ObjectInputStream inStream, String nomeFicheiroLocal) {
        try {
            String status = (String) inStream.readObject();
            
            if (status.equals("OK")) {
                long fileSize = inStream.readLong();
                FileOutputStream fileOut = new FileOutputStream(nomeFicheiroLocal);
                byte[] buffer = new byte[4096];
                int bytesRead;
                long totalLido = 0;
                
                while (totalLido < fileSize && (bytesRead = inStream.read(buffer, 0, (int) Math.min(buffer.length, fileSize - totalLido))) != -1) {
                    fileOut.write(buffer, 0, bytesRead);
                    totalLido += bytesRead;
                }
                
                fileOut.close();
                System.out.println("Download concluído! Guardado como: " + nomeFicheiroLocal);
                
            } else {
                System.out.print("Erro: ");
                switch(status) {
                    case "NOK": 
                        System.out.println("Comando ou valores inválidos."); 
                        break;
                    case "NOHM": 
                        System.out.println("A casa especificada não existe."); 
                        break;
                    case "NOPERM": 
                        System.out.println("Não tem permissões para esta operação."); 
                        break;
                    case "NODATA": 
                        System.out.println("Ainda não há dados registados."); 
                        break;
                    case "NOD": 
                        System.out.println("O dispositivo não existe na casa."); 
                        break;
                    default: 
                        System.out.println(status); 
                        break;
                }
            }
        } catch (Exception e) {
            System.out.println("Erro ao tentar receber o ficheiro.");
        }
    }
}
