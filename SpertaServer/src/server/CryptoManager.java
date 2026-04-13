public class CryptoManager {
    private static String password;
    private static byte[] salt;

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
}