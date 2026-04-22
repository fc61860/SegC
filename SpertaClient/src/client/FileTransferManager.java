package SpertaClient.src.client;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;

/**
 * Gere a rececao e a escrita local de ficheiros enviados pelo servidor.
 */
public class FileTransferManager {
    private static final String DIRETORIA_DATA = "SpertaClient/data/";

    /**
     * Processa a rececao de um ficheiro enviado pelo servidor.
     *
     * @param inStream          stream de entrada do servidor
     * @param nomeFicheiroLocal nome do ficheiro local a guardar
     */
    public File processFile(ObjectInputStream inStream, String nomeFicheiroLocal) {
        try {
            ensureDataDirectory();
            String status = (String) inStream.readObject();

            if (!status.equals("OK")) {
                System.out.println(status);
                return null;
            }

            long fileSize = inStream.readLong();
            System.out.println("Server: " + status + ", " + fileSize + " (long), seguido de " + fileSize
                    + " bytes de dados.");

            try (FileOutputStream fileOut = new FileOutputStream(DIRETORIA_DATA + nomeFicheiroLocal)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                long totalLido = 0;

                while (totalLido < fileSize && (bytesRead = inStream.read(buffer, 0,
                        (int) Math.min(buffer.length, fileSize - totalLido))) != -1) {
                    fileOut.write(buffer, 0, bytesRead);
                    totalLido += bytesRead;
                }
            }

            System.out.println("Guardado como: " + DIRETORIA_DATA + nomeFicheiroLocal);
            return new File(DIRETORIA_DATA + nomeFicheiroLocal);
        } catch (Exception e) {
            System.out.println("Erro ao tentar receber o ficheiro.");
            return null;
        }
    }

    /**
     * Garante a existencia da diretoria local onde os ficheiros recebidos sao
     * guardados.
     */
    private void ensureDataDirectory() {
        File dataDir = new File(DIRETORIA_DATA);
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
    }

    public File processKeyFile(ObjectInputStream inStream, String nomeFicheiroLocal) {
        try {
            String keyStatus = (String) inStream.readObject();

            if (!keyStatus.equals("OK_KEY")) {
                System.out.println(keyStatus);
                return null;
            }

            long keySize = inStream.readLong();
            System.out.println("Server: " + keyStatus + ", " + keySize + " bytes.");

            String keyFileName = "key_" + nomeFicheiroLocal;

            try (FileOutputStream keyOut = new FileOutputStream(DIRETORIA_DATA + keyFileName)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                long totalLido = 0;

                while (totalLido < keySize && (bytesRead = inStream.read(
                        buffer, 0, (int) Math.min(buffer.length, keySize - totalLido))) != -1) {
                    keyOut.write(buffer, 0, bytesRead);
                    totalLido += bytesRead;
                }
            }

            System.out.println("Key guardada como: " + DIRETORIA_DATA + keyFileName);
            return new File(DIRETORIA_DATA + keyFileName);
        } catch (Exception e) {
            System.out.println("Erro ao tentar receber o ficheiro.");
            return null;
        }

    }
}