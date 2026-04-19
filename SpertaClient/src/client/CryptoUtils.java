package SpertaClient.src.client;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.io.ByteArrayInputStream;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;

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

    /**
     * Verifica se a truststore JKS contem um certificado com o alias indicado.
     *
     * @param truststorePath caminho para o ficheiro JKS
     * @param truststorePass password da truststore
     * @param alias          alias a procurar (tipicamente o username)
     * @return true se o certificado existir
     */
    public static boolean hasCertInTruststore(String truststorePath, String truststorePass, String alias)
            throws Exception {
        KeyStore ks = KeyStore.getInstance("JKS");
        try (FileInputStream fis = new FileInputStream(truststorePath)) {
            ks.load(fis, truststorePass.toCharArray());
        }
        return ks.containsAlias(alias);
    }

    /**
     * Guarda um certificado na truststore JKS com o alias indicado.
     * O ficheiro e atualizado imediatamente apos a insercao.
     *
     * @param truststorePath caminho para o ficheiro JKS
     * @param truststorePass password da truststore
     * @param alias          alias com que o certificado sera guardado
     * @param cert           certificado a guardar
     */
    public static void saveCertToTruststore(String truststorePath, String truststorePass, String alias,
            Certificate cert) throws Exception {
        KeyStore ks = KeyStore.getInstance("JKS");
        try (FileInputStream fis = new FileInputStream(truststorePath)) {
            ks.load(fis, truststorePass.toCharArray());
        }
        ks.setCertificateEntry(alias, cert);
        try (FileOutputStream fos = new FileOutputStream(truststorePath)) {
            ks.store(fos, truststorePass.toCharArray());
        }
    }

    /**
     * Carrega a chave publica de um certificado guardado na truststore JKS.
     *
     * @param truststorePath caminho para o ficheiro JKS
     * @param truststorePass password da truststore
     * @param alias          alias do certificado
     * @return chave publica RSA
     */
    public static PublicKey loadPublicKeyFromTruststore(String truststorePath, String truststorePass, String alias)
            throws Exception {
        KeyStore ks = KeyStore.getInstance("JKS");
        try (FileInputStream fis = new FileInputStream(truststorePath)) {
            ks.load(fis, truststorePass.toCharArray());
        }
        Certificate cert = ks.getCertificate(alias);
        if (cert == null) {
            throw new Exception("Certificado nao encontrado na truststore: " + alias);
        }
        return cert.getPublicKey();
    }
}
