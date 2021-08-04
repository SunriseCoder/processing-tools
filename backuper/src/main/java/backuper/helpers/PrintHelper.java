package backuper.helpers;

public class PrintHelper {
    private static final int CONSOLE_WIDTH = 119;

    public static void println() {
        System.out.println();
    }

    public static void println(String string) {
        String message = String.format("%-" + CONSOLE_WIDTH + "s", string);
        System.out.println(message);
    }

    public static void printAndReturn(String string) {
        String message = String.format("%-" + CONSOLE_WIDTH + "s\r", string);
        System.out.print(message);
    }
}
