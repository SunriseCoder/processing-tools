package backuper.helpers;

public class FormattingHelper {

    public static String humanReadableSize(long size) {
        if (size < 1024) {
            return size + "b";
        }

        int log = (int) (Math.log(size) / Math.log(1024));
        char letter = "kMGTPE".charAt(log - 1);
        double mantis = size / Math.pow(1024, log);
        String result;
        if (mantis < 10) {
            result = String.format("%.1f%sb", mantis, letter); // Like 5,8Gb
        } else {
            result = String.format("%.0f%sb", mantis, letter); // Like 18Gb
        }
        return result;
    }

    public static String humanReadableTime(long seconds) {
        int s = (int) (seconds % 60);
        int m = (int) (seconds / 60 % 60);
        int h = (int) (seconds / 60 / 60 % 24);
        int d = (int) (seconds / 60 / 60 / 24);

        String result = (d > 0 ? d + ":" : "") + String.format("%02d:%02d:%02d", h, m, s);
        return result;
    }
}
