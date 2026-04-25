import java.util.regex.Pattern;

/**
 * Validacao centralizada de identificadores vindos da rede/comandos.
 */
public final class ValidationUtils {
    private static final Pattern USER_OR_HOUSE = Pattern.compile("^[A-Za-z0-9_-]{1,64}$");
    private static final Pattern DEVICE_ID = Pattern.compile("^[EGLMPS][1-9][0-9]*$");

    private ValidationUtils() {
    }

    public static boolean isValidUserOrHouse(String value) {
        return value != null && USER_OR_HOUSE.matcher(value).matches();
    }

    public static boolean isValidSection(String value) {
        if (value == null) {
            return false;
        }
        return "E".equals(value) || "G".equals(value) || "L".equals(value) || "M".equals(value) || "P".equals(value)
                || "S".equals(value) || "all".equals(value);
    }

    public static boolean isValidDeviceId(String value) {
        return value != null && DEVICE_ID.matcher(value).matches();
    }
}
