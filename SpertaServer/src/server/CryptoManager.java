import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.KeySpec;

public class CryptoManager {
    private static String password;
    private static byte[] salt;

    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 128; // AES-128
    private static final int IV_LENGTH = 16;

    public static void init(String passwordInput, byte[] saltInput) {
        password = passwordInput;
        salt = saltInput;
    }

    public static String getPassword() {
        return password;
    }

    public static byte[] getSalt() {
        return salt;
    }

    /**
     * Deriva uma chave AES-128 a partir da password e do salt usando PBKDF2WithHmacSHA256.
     */
    private static SecretKey deriveKey() throws Exception {
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * Cifra um array de bytes com AES/CBC/PKCS5Padding.
     * O IV aleatório é prefixado ao criptograma devolvido.
     *
     * @param plaintext bytes a cifrar
     * @return IV (16 bytes) + criptograma
     */
    public static byte[] encrypt(byte[] plaintext) throws Exception {
        SecretKey key = deriveKey();
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        byte[] iv = new byte[IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
        byte[] ciphertext = cipher.doFinal(plaintext);
        byte[] result = new byte[IV_LENGTH + ciphertext.length];
        System.arraycopy(iv, 0, result, 0, IV_LENGTH);
        System.arraycopy(ciphertext, 0, result, IV_LENGTH, ciphertext.length);
        return result;
    }

    /**
     * Decifra um array de bytes produzido por encrypt().
     * Extrai o IV dos primeiros 16 bytes e decifra o restante.
     *
     * @param data IV (16 bytes) + criptograma
     * @return plaintext decifrado
     */
    public static byte[] decrypt(byte[] data) throws Exception {
        if (data.length <= IV_LENGTH) {
            throw new IllegalArgumentException("Dados cifrados demasiado curtos");
        }
        byte[] iv = new byte[IV_LENGTH];
        System.arraycopy(data, 0, iv, 0, IV_LENGTH);
        byte[] ciphertext = new byte[data.length - IV_LENGTH];
        System.arraycopy(data, IV_LENGTH, ciphertext, 0, ciphertext.length);
        SecretKey key = deriveKey();
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
        return cipher.doFinal(ciphertext);
    }

    /**
     * Gera uma chave AES-128 aleatória para uso como chave de secção.
     *
     * @return bytes da chave AES-128
     */
    public static byte[] generateSectionKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128);
        return keyGen.generateKey().getEncoded();
    }

    /**
     * Cifra dados com uma chave pública RSA (RSA/ECB/PKCS1Padding).
     *
     * @param data   bytes a cifrar
     * @param pubKey chave pública RSA do destinatário
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
     * @param privKey chave privada RSA do destinatário
     * @return plaintext
     */
    public static byte[] decryptWithPrivateKey(byte[] data, PrivateKey privKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, privKey);
        return cipher.doFinal(data);
    }
}