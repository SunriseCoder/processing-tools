package download;

import utils.FormattingUtils;
import utils.MathUtils;

public class DownloadProgressPrinter implements SimpleProgressListener {
    private long updateProgressInterval;

    private long fileSize;
    private long startTime;
    private long lastReportTime;
    private volatile long dataCompletedTotal;
    private volatile long dataSinceLastReport;
    private int lastMessageLength;

    public DownloadProgressPrinter(long updateProgressInterval) {
        this.updateProgressInterval = updateProgressInterval;
    }

    public synchronized void start(long fileSize) {
        this.fileSize = fileSize;
        startTime = System.currentTimeMillis();

        // Time and Data Counters clean up
        lastReportTime = startTime;
        dataCompletedTotal = 0;
        dataSinceLastReport = 0;
        lastMessageLength = 0;
    }

    @Override
    public synchronized void progress(long amount) {
        dataCompletedTotal += amount;
        dataSinceLastReport += amount;
    }

    public synchronized void printProgress(boolean force) {
        // Checking, is the time to print report or not yet
        long now = System.currentTimeMillis();
        long timeSinceLastReport = now - lastReportTime;
        if (!force && timeSinceLastReport < updateProgressInterval) {
            return;
        }

        // Calculations
        long momentumSpeed = 1000 * dataSinceLastReport / timeSinceLastReport;
        long timeSinceStart = now - startTime;
        long averageSpeed = 1000 * dataCompletedTotal / timeSinceStart;
        long remainingTime = dataCompletedTotal == 0 ? 0 :
            MathUtils.roundToLong((double) (fileSize - dataCompletedTotal) * timeSinceStart / dataCompletedTotal);

        // Formatting message and printing

        // Moving cursor at the beginning of the previous progress message
        for (int i = 0; i < lastMessageLength; i++) {
            System.out.print("\b");
        }
        // Formatting progress message
        StringBuilder message = new StringBuilder();
        message.append(FormattingUtils.humanReadableSize(dataCompletedTotal));
        message.append("b of ");
        message.append(FormattingUtils.humanReadableSize(fileSize)).append("b (");
        message.append(dataCompletedTotal * 100 / fileSize).append("%), ");
        message.append(FormattingUtils.humanReadableSize(momentumSpeed)).append("b/s");
        message.append(", avg: ").append(FormattingUtils.humanReadableSize(averageSpeed)).append("b/s");
        message.append(", passed: ").append(FormattingUtils.humanReadableTimeS(timeSinceStart / 1000));
        message.append(", remain: ").append(FormattingUtils.humanReadableTimeS(remainingTime / 1000));

        // Overwriting symbols from the last message
        int messageRealLength = message.length();
        int extraSpacesCounter = 0;
        while (message.length() < lastMessageLength) {
            message.append(" ");
            extraSpacesCounter++;
        }
        // Printing the message
        System.out.print(message);
        // Moving cursor back to the end of the real message
        for (;extraSpacesCounter > 0; extraSpacesCounter--) {
            System.out.print("\b");
        }

        // End of the report
        lastReportTime = now;
        dataSinceLastReport = 0;
        lastMessageLength = messageRealLength;
    }
}
