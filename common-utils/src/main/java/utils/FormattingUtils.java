package utils;

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
            result = String.format("%.1f%s", mantis, letter); // Like 5,8Gb
        } else {
            result = String.format("%.0f%s", mantis, letter); // Like 18Gb
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
}
