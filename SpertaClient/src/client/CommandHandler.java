package SpertaClient.src.client;

import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Processa comandos introduzidos pelo utilizador e trata a comunicacao com o
 * servidor.
 */
public class CommandHandler {
    private final FileTransferManager fileTransferManager;

    /**
     * Cria um processador de comandos com suporte a transferencia de ficheiros.
     *
     * @param fileTransferManager componente responsavel por guardar ficheiros
     *                            recebidos do servidor
     */
    public CommandHandler(FileTransferManager fileTransferManager) {
        this.fileTransferManager = fileTransferManager;
    }

    /**
     * Processa a linha de comando introduzida pelo utilizador.
     *
     * @param input     comando completo a enviar ao servidor
     * @param outStream stream de saida para o servidor
     * @param inStream  stream de entrada vindo do servidor
     */
    public void processCommand(String input, ObjectOutputStream outStream, ObjectInputStream inStream) {
        String[] parts = input.split(" ");
        String command = parts[0];

        switch (command) {
            case "CREATE":
                handleSimpleCommand(parts, input, "Formato incorreto. Tente: CREATE <hm>", outStream, inStream, 2);
                break;
            case "ADD":
                handleSimpleCommand(parts, input, "Formato incorreto. Tente: ADD <user> <hm> <s>", outStream,
                        inStream, 4);
                break;
            case "RD":
                handleSimpleCommand(parts, input, "Formato incorreto. Tente: RD <hm> <sec>", outStream, inStream,
                        3);
                break;
            case "EC":
                handleEcCommand(parts, input, outStream, inStream);
                break;
            case "RT":
                handleRtCommand(parts, input, outStream, inStream);
                break;
            case "RH":
                handleRhCommand(parts, input, outStream, inStream);
                break;
            default:
                System.out.println("Comando desconhecido! Tente novamente.");
                break;
        }
    }

    /**
     * Trata comandos com resposta textual simples do servidor.
     */
    private void handleSimpleCommand(String[] parts, String input, String errorMessage, ObjectOutputStream outStream,
            ObjectInputStream inStream, int expectedLength) {
        if (parts.length != expectedLength) {
            System.out.println(errorMessage);
            return;
        }

        try {
            sendCommand(input, outStream);
            String answer = (String) inStream.readObject();
            System.out.println("Server: " + answer);
        } catch (Exception e) {
            System.out.println("Erro ao comunicar com o servidor.");
        }
    }

    /**
     * Trata o comando EC com validacao do valor numerico enviado ao dispositivo.
     */
    private void handleEcCommand(String[] parts, String input, ObjectOutputStream outStream,
            ObjectInputStream inStream) {
        if (parts.length != 4 || !parts[3].matches("^[0-9]+$")) {
            System.out.println("Formato incorreto. Tente: EC <hm> <d> <int>");
            return;
        }

        int valor = Integer.parseInt(parts[3]);
        if (valor < 0 || valor > 600) {
            System.out.println(
                    "Valores  possíveis de <int>: 0(desligar), 1(ligar), ]1..600] (ligar por x minutos, até 600). ");
            return;
        }

        handleSimpleCommand(parts, input, "Formato incorreto. Tente: EC <hm> <d> <int>", outStream, inStream, 4);
    }

    /**
     * Trata o comando RT e guarda localmente o resumo recebido do servidor.
     */
    private void handleRtCommand(String[] parts, String input, ObjectOutputStream outStream,
            ObjectInputStream inStream) {
        if (parts.length != 2) {
            System.out.println("Formato incorreto. Tente: RT <hm>");
            return;
        }

        try {
            sendCommand(input, outStream);

            File pastaSummaries = new File("summaries");
            if (!pastaSummaries.exists()) {
                pastaSummaries.mkdirs();
            }

            String nomeFicheiro = "cliente_summary_" + parts[1] + ".txt";
            fileTransferManager.processFile(inStream, nomeFicheiro);
        } catch (Exception e) {
            System.out.println("Erro ao comunicar com o servidor.");
        }
    }

    /**
     * Trata o comando RH e guarda localmente o historico recebido do servidor.
     */
    private void handleRhCommand(String[] parts, String input, ObjectOutputStream outStream,
            ObjectInputStream inStream) {
        if (parts.length != 3) {
            System.out.println("Formato incorreto. Tente: RH <hm> <d>");
            return;
        }

        try {
            sendCommand(input, outStream);
            String nomeFicheiro = "cliente_log_" + parts[1] + "_" + parts[2] + ".txt";
            fileTransferManager.processFile(inStream, nomeFicheiro);
        } catch (Exception e) {
            System.out.println("Erro ao comunicar com o servidor.");
        }
    }

    /**
     * Envia uma linha de comando completa para o servidor.
     */
    private void sendCommand(String input, ObjectOutputStream outStream) throws Exception {
        outStream.writeObject(input);
        outStream.flush();
    }
}