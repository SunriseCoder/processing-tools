package backuper.logger;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {
    private static final String LOG_FILE = "backuper.log";
    private static final SimpleDateFormat DATE_TIME_FORMATTER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private static final Level logLevel = Level.INFO;

    private static PrintWriter logWriter;

    static {
        try {
            logWriter = new PrintWriter(LOG_FILE);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.out.println("Cannot initialize logger, exiting");
            System.exit(-1);
        }
    }

    public static void severe(String message) {
        log(Level.SEVERE, message);
    }

    public static void error(String message) {
        log(Level.ERROR, message);
    }

    public static void warning(String message) {
        log(Level.WARNING, message);
    }

    public static void info(String message) {
        log(Level.INFO, message);
    }

    public static void debug(String message) {
        log(Level.DEBUG, message);
    }

    public static void trace(String message) {
        log(Level.TRACE, message);
    }

    public synchronized static void log(Level level, String message) {
        if (level.ordinal() > logLevel.ordinal()) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(DATE_TIME_FORMATTER.format(new Date())).append(" ").append(level).append(" ").append(message);
        logWriter.println(sb);
        logWriter.flush();
    }

    public static enum Level {
        OFF, SEVERE, ERROR, WARNING, INFO, DEBUG, TRACE;
    }
}
