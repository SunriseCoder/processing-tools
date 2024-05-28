package app.utils;

public class StringUtils {

    public static String trimEndSymbols(String text, String exclusion) {
        if (text == null || text.isEmpty() || exclusion == null || exclusion.isEmpty()) {
            return text;
        }

        while (text.endsWith(exclusion)) {
            text = text.substring(0, text.length() - exclusion.length());
        }

        return text;
    }

    public static String toCamelCase(String string) {
        StringBuilder sb = new StringBuilder();

        String previousSymbol = null;
        for (int i = 0; i < string.length(); i++) {
            String currentSymbol = string.substring(i, i + 1);
            if (!" ".equals(currentSymbol) && (" ".equals(previousSymbol) || previousSymbol == null)) {
                sb.append(currentSymbol.toUpperCase());
            } else {
                sb.append(currentSymbol.toLowerCase());
            }
            previousSymbol = currentSymbol;
        }

        return sb.toString();
    }

    public static StringBuilder appendStringBuilderAtTheEnd(StringBuilder sb, String text, int targetLength) {
        while (sb.length() < targetLength) {
            sb.append(text);
        }
        return sb;
    }
}
