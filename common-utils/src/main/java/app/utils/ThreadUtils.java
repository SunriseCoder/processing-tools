package app.utils;

import javafx.application.Platform;

public class ThreadUtils {

    public static void runLaterAfterSleep(long milliseconds, Runnable runnable) {
        Thread thread = new Thread(() -> {
            sleep(milliseconds);
            runLater(runnable);
        });
        thread.start();
    }

    public static void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            // TODO Figure out, what may happens here
            e.printStackTrace();
        }
    }

    public static void runLater(Runnable runnable) {
        Platform.runLater(runnable);
    }
}
