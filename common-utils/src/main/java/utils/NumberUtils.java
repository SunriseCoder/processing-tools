package utils;

public class NumberUtils {

    public static String generateNumberByMaxNumber(int number, int maxNumber) {
        int length = MathUtils.ceilToInt(Math.log10(maxNumber));
        String result = generateNumberByLength(number, length);
        return result;
    }

    public static String generateNumberByLength(int number, int length) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.valueOf(number));

        while (sb.length() < length) {
            sb.insert(0, "0");
        }

        return sb.toString();
    }
}
