package SpertaClient.src.client;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.net.ConnectException;

/**
 * Coordena a ligacao do cliente ao servidor, incluindo autenticacao e ciclo de
 * comandos interativo.
 */
public class ClientSession {
    private final AuthHandler authHandler;
    private final CommandHandler commandHandler;

    /**
     * Cria uma nova sessao de cliente com os componentes necessarios para
     * autenticacao e processamento de comandos.
     *
     * @param authHandler    componente responsavel por attestation e autenticacao
     * @param commandHandler componente responsavel pelo envio dos comandos do
     *                       protocolo
     */
    public ClientSession(AuthHandler authHandler, CommandHandler commandHandler) {
        this.authHandler = authHandler;
        this.commandHandler = commandHandler;
    }

    /**
     * Inicia a sessao do cliente na ligacao indicada.
     *
     * @param ip   endereco IP do servidor
     * @param port porto TCP do servidor
     * @param user nome do utilizador
     * @param pass password inicial do utilizador
     */
    public void start(String ip, int port, String user, String pass) {
        SSLSocketFactory sslsf = (SSLSocketFactory) SSLSocketFactory.getDefault();
        try (SSLSocket clientSocket = (SSLSocket) sslsf.createSocket(ip, port);
                ObjectOutputStream outStream = new ObjectOutputStream(clientSocket.getOutputStream());
                ObjectInputStream inStream = new ObjectInputStream(clientSocket.getInputStream())) {
            // failFast
            clientSocket.startHandshake();
            Scanner sc = new Scanner(System.in);
            //apartir daqui nao funciona por causa do hash
            if (!authHandler.authenticate(SpertaClient.class, user, pass, sc, outStream, inStream)) {
                return;
            }

            System.out.println("Login efetuado com sucesso!");
            showMenu();

            while (true) {
                try {
                    System.out.println("Comando: ");
                    String input = sc.nextLine().trim();

                    if (input.isEmpty()) {
                        continue;
                    }

                    commandHandler.processCommand(input, outStream, inStream);
                } catch (Exception e) {
                    System.out.println("O cliente desligou-se (Ligação terminada).");
                    break;
                }
            }
        } catch (ConnectException e) {
            System.out.println("Nao foi possivel ligar ao servidor. Servidor indisponivel ou endereço incorreto.");
        } catch (Exception e) {
            System.out.println("Erro inesperado no cliente.");
            e.printStackTrace();
        }
    }

    /**
     * Mostra o menu de comandos disponiveis ao utilizador.
     */
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
}