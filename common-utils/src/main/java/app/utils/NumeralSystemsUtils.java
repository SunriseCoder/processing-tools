package app.utils;

public class NumeralSystemsUtils {

    public static int hexToDecInt(String hex) {
        int decimal = Integer.parseInt(hex, 16);
        return decimal;
    }
}
