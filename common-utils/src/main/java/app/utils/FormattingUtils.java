package app.utils;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class FormattingUtils {

    public static String humanReadableSize(long size) {
        if (size < 1024) {
            return String.valueOf(size);
        }

        int log = (int) (Math.log(size) / Math.log(1024));
        char letter = "kMGTPE".charAt(log - 1);
        double mantis = size / Math.pow(1024, log);
        String result;
        if (mantis < 10) {
            result = String.format("%.1f%s", mantis, letter); // Like 5,8G
        } else {
            result = String.format("%.0f%s", mantis, letter); // Like 18G
        }
        return result;
    }

    public static String humanReadableTimeS(long seconds) {
        int s = (int) (seconds % 60);
        int m = (int) (seconds / 60 % 60);
        int h = (int) (seconds / 60 / 60 % 24);
        int d = (int) (seconds / 60 / 60 / 24);

        String result = (d > 0 ? d + ":" : "") + String.format("%02d:%02d:%02d", h, m, s);
        return result;
    }

    public static String humanReadableTimeMS(long milliseconds) {
        int ms = (int) (milliseconds % 1000);
        int s = (int) (milliseconds / 1000 % 60);
        int m = (int) (milliseconds / 1000 / 60 % 60);
        int h = (int) (milliseconds / 1000 / 60 / 60 % 24);
        int d = (int) (milliseconds / 1000 / 60 / 60 / 24);

        String result = (d > 0 ? d + ":" : "") + String.format("%02d:%02d:%02d.%03d", h, m, s, ms);
        return result;
    }

    public static String mapToString(Map<?, ?> map) {
        String allEntriesString = map.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(", "));

        String result = "{" + allEntriesString + "}";
        return result;
    }

    public static String percentage(long value, long total, int precision) {
        double percentage = 100.0 * value / total;
        String percentageString = String.format(Locale.US, "%." + precision + "f%%", percentage);
        return percentageString;
    }

    /**
     * This method is designed for visual comparison of long file paths, like:
     * Copy       d:\path1\a-folder\file.ext
     *   -> e:\anotherPath\a-folder\file.ext
     *
     * @param strings prefix and string pairs, should be even number of strings (dividable by 2)
     * @return formatted string
     */
    public static String alignLongStringsByRightSide(int columnsNumber, String... strings) {
        List<String> list = Arrays.asList(strings);
        String result = alignLongStringsByRightSide(columnsNumber, list);
        return result;
    }

    /**
     * This method is designed for visual comparison of long file paths, like:
     * Copy       d:\path1\a-folder\file.ext
     *   -> e:\anotherPath\a-folder\file.ext
     *
     * @param strings prefix and string pairs, should be even number of strings (dividable by 2)
     * @return formatted string
     */
    public static String alignLongStringsByRightSide(int columnsNumber, List<String> strings) {
     // Checking input arguments length
        if (strings.size() % columnsNumber != 0) {
            throw new IllegalArgumentException("Amount of String arguments must be dividable by columnsNumber " + columnsNumber + ".");
        }

        // Checking longest string lengths
        int[] maxLengths = new int[columnsNumber];
        for (int i = 0; i < strings.size(); i += columnsNumber) {
            for (int j = 0; j < columnsNumber; j++) {
                if (strings.get(i + j).length() > maxLengths[j]) {
                    maxLengths[j] = strings.get(i + j).length();
                }
            }
        }

        // Adjusting all the strings to the max lengths
        StringBuilder sb = new StringBuilder();
        // Rows Loop
        for (int i = 0; i < strings.size(); i += columnsNumber) {
            // New Line Character to the end of the previous line
            if (i > 0) {
                sb.append("\n");
            }
            // Columns Loop
            for (int j = 0; j < columnsNumber; j++) {
                // Space Symbol to the end of last column
                if (j > 0) {
                    sb.append(" ");
                }
                // Filling the current string with space symbols to size of the longest string in the current column
                String string = strings.get(i + j);
                for (int k = string.length(); k < maxLengths[j]; k++) {
                    sb.append(" ");
                }
                // Appending the current String
                sb.append(string);
            }
        }
        sb.append("\n");

        return sb.toString();
    }
}
