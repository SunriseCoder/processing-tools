package app.utils;

import java.io.IOException;
import java.io.PrintWriter;

public class DumpUtils {

    public static void dumpStringToFile(String string, String filename) {
        try (PrintWriter pw = new PrintWriter(filename)) {
            pw.print(string);
            pw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
