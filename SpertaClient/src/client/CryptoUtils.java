package SpertaClient.src.client;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

/**
 * Utilitarios de criptografia para o cliente Sperta.
 * Concentra operacoes RSA e AES usadas nos comandos CREATE e ADD.
 */
public class CryptoUtils {

    /**
     * Gera uma chave AES-128 aleatoria para uso como chave de seccao.
     *
     * @return bytes da chave AES-128
     */
    public static byte[] generateSectionKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128);
        return keyGen.generateKey().getEncoded();
    }

    /**
     * Cifra dados com uma chave publica RSA (RSA/ECB/PKCS1Padding).
     *
     * @param data   bytes a cifrar
     * @param pubKey chave publica RSA do destinatario
     * @return criptograma RSA
     */
    public static byte[] encryptWithPublicKey(byte[] data, PublicKey pubKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, pubKey);
        return cipher.doFinal(data);
    }

    /**
     * Decifra dados com uma chave privada RSA (RSA/ECB/PKCS1Padding).
     *
     * @param data    criptograma RSA
     * @param privKey chave privada RSA do cliente
     * @return plaintext
     */
    public static byte[] decryptWithPrivateKey(byte[] data, PrivateKey privKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, privKey);
        return cipher.doFinal(data);
    }

    /**
     * Carrega a chave publica RSA a partir de uma keystore PKCS12.
     *
     * @param keystorePath caminho para o ficheiro .p12
     * @param keystorePass password da keystore
     * @return chave publica RSA
     */
    public static PublicKey loadPublicKey(String keystorePath, String keystorePass) throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream(keystorePath)) {
            ks.load(fis, keystorePass.toCharArray());
        }
        String alias = ks.aliases().nextElement();
        Certificate cert = ks.getCertificate(alias);
        return cert.getPublicKey();
    }

    /**
     * Carrega a chave privada RSA a partir de uma keystore PKCS12.
     *
     * @param keystorePath caminho para o ficheiro .p12
     * @param keystorePass password da keystore
     * @return chave privada RSA
     */
    public static PrivateKey loadPrivateKey(String keystorePath, String keystorePass) throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream(keystorePath)) {
            ks.load(fis, keystorePass.toCharArray());
        }
        String alias = ks.aliases().nextElement();
        return (PrivateKey) ks.getKey(alias, keystorePass.toCharArray());
    }

    public static byte[] decryptFile(byte[] encryptedData, SecretKey key) throws Exception {
        // 1. Extract IV (first 16 bytes)
        byte[] iv = Arrays.copyOfRange(encryptedData, 0, 16);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        // 2. Extract ciphertext
        byte[] ciphertext = Arrays.copyOfRange(encryptedData, 16, encryptedData.length);

        // 3. Setup cipher
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);

        // 4. Decrypt
        return cipher.doFinal(ciphertext);
    }

}
