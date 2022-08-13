package progress;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import utils.MathUtils;

public class ProgressPrinter {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("mm:ss");
    private static final int PROGRESS_PRINT_INTERVAL_PERCENTAGE = 1;
    private static final long PROGRESS_PRINT_INTERVAL_MS = 1000;

    // Service variables
    private long startTime;
    private long total;
    private long lastValue;

    private int lastPrintPercentage;
    private long lastPrintTime;

    public void reset() {
        reset(0);
    }

    public void reset(long total) {
        this.total = total;
        lastValue = 0;

        startTime = System.currentTimeMillis();
        lastPrintPercentage = 0;
        lastPrintTime = startTime;
    }

    public void setTotal(long total) {
        this.total = total;
        this.startTime = System.currentTimeMillis();
    }

    public void printProgressFinished() {
        printProgress(total, true);
    }

    public void printProgressIncrease(long delta, boolean force) {
        long currentValue = lastValue + delta;
        printProgress(currentValue, force);
    }

    public void printProgress(long currentValue, boolean force) {
        long now = System.currentTimeMillis();
        int currentPercentage = MathUtils.roundToInt(100.0 * currentValue / total);
        lastValue = currentValue;
        if (!force && now - lastPrintTime < PROGRESS_PRINT_INTERVAL_MS
                && currentPercentage - lastPrintPercentage < PROGRESS_PRINT_INTERVAL_PERCENTAGE) {
            return;
        }

        System.out.print("\r[");
        for (int i = 0; i < currentPercentage - 1; i++) {
            System.out.print("=");
        }
        System.out.print(">");
        for (int i = currentPercentage; i < 100; i++) {
            System.out.print(" ");
        }
        System.out.print("] " + currentPercentage + "% ");
        if (currentPercentage > 0) {
            long elapsed = System.currentTimeMillis() - startTime;
            long elapsedPercentCost = elapsed / 10 / currentPercentage;
            long estimation = (100 - currentPercentage) * elapsedPercentCost / 100;
            LocalTime time = LocalTime.ofSecondOfDay(estimation);
            String timeStr = time.format(formatter);
            System.out.print("ETA: " + timeStr);
        }

        lastPrintPercentage = currentPercentage;
        lastPrintTime = now;
    }

    public void println(String string) {
        System.out.println(string);
    }
}
