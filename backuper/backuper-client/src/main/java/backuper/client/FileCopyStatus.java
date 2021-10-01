package backuper.client;

import java.text.DecimalFormat;

import backuper.common.helpers.FormattingHelper;
import backuper.common.helpers.PrintHelper;

public class FileCopyStatus {
    private static final int COPYING_STATUS_INTERVAL = 1000;

    private DecimalFormat percentFormat = new DecimalFormat("0.00%");

    private long startCopyingTime;
    private long currentFileStartTime;
    private long currentFileCopiedSize;
    private long currentFileTotalSize;
    private long allFilesCopiedSize;
    private long allFilesTotalSize;
    private long lastPrintTime;
    private long lastPrintAllFilesCopiedSize;
    private int lastMessageLength;

    public synchronized void reset() {
        startCopyingTime = System.currentTimeMillis();
        currentFileCopiedSize = 0;
        currentFileTotalSize = 0;
        allFilesCopiedSize = 0;
        allFilesTotalSize = 0;
        lastPrintAllFilesCopiedSize = 0;
    }

    public synchronized void startNewFile(long fileSize) {
        long now = System.currentTimeMillis();
        currentFileStartTime = now;
        lastPrintTime = now;

        currentFileCopiedSize = 0;
        currentFileTotalSize = fileSize;
    }

    public synchronized void addAllFilesTotalSize(long delta) {
        this.allFilesTotalSize += delta;
    }

    public synchronized void addCopiedSize(long delta) {
        this.currentFileCopiedSize += delta;
        this.allFilesCopiedSize += delta;
    }

    public synchronized void printCopyProgress() {
        long now = System.currentTimeMillis();
        long timeDelta = now - lastPrintTime;
        long copiedDelta = allFilesCopiedSize - lastPrintAllFilesCopiedSize;
        if (timeDelta >= COPYING_STATUS_INTERVAL && copiedDelta > 0) {
            // 25Mb of 4.3G (1.05%) / 35Gb of 2Tb (1.25%) / avg: 75Mb/s / Eta: 101:01:52
            // Current file
            double currentPercent = (double) currentFileCopiedSize / currentFileTotalSize;
            String currentFileCopiedSizeStr = FormattingHelper.humanReadableSize(currentFileCopiedSize);
            String message = String.format("%6s", currentFileCopiedSizeStr);
            message += " of " + FormattingHelper.humanReadableSize(currentFileTotalSize);
            String currentPercentStr = percentFormat.format(currentPercent);
            currentPercentStr = String.format("%7s", currentPercentStr);
            message += " (" + currentPercentStr + ") / ";

            // All files
            double allFilesPercent = (double) allFilesCopiedSize / allFilesTotalSize;
            String allFilesCopiedSizeStr = FormattingHelper.humanReadableSize(allFilesCopiedSize);
            message += String.format("%6s", allFilesCopiedSizeStr);
            message += " of " + FormattingHelper.humanReadableSize(allFilesTotalSize);
            String allFilesPercentStr = percentFormat.format(allFilesPercent);
            allFilesPercentStr = String.format("%7s", allFilesPercentStr);
            message += " (" + allFilesPercentStr + ") / ";

            // Speed
            long currentFileTimeDelta = now - currentFileStartTime;
            long speed = currentFileTimeDelta > 0 ? currentFileCopiedSize * 1000 / currentFileTimeDelta : 0;
            String speedStr = FormattingHelper.humanReadableSize(speed);
            speedStr = String.format("%6s", speedStr);
            message += "avg: " + speedStr + "/s / ";

            // Remaining estimation
            long duration = now - startCopyingTime;
            if (duration != 0 && allFilesPercent != 0) {
                // Algorithm based on overall average speed
                //long totalTime = (long) (duration / allFilesPercent);
                //long remaining = (long) (totalTime * (1 - allFilesPercent));
                //remaining /= 1000;

                // Algorithm based on last status update period speed
                long remainingSize = allFilesTotalSize - allFilesCopiedSize;
                long remaining = remainingSize / speed;
                message += "Eta: " + FormattingHelper.humanReadableTime(remaining);
            }
            lastMessageLength = message.length();
            PrintHelper.printAndReturn(message);

            lastPrintTime = now;
            lastPrintAllFilesCopiedSize = allFilesCopiedSize;
        }
    }

    public synchronized void printCopyResults() {
        long now = System.currentTimeMillis();
        long timeDelta = now - startCopyingTime;
        if (timeDelta > 0) {
            long speed = allFilesCopiedSize * 1000 / timeDelta;

            String message = "Copied: " + FormattingHelper.humanReadableSize(allFilesCopiedSize)
                    + ", took: " + FormattingHelper.humanReadableTime(timeDelta / 1000)
                    + ", avg: " + FormattingHelper.humanReadableSize(speed) + "/s";

            PrintHelper.println(message);
        }
    }

    public synchronized void printLastLineCleanup() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lastMessageLength; i++) {
            sb.append(" ");
        }
        PrintHelper.printAndReturn(sb.toString());
    }

    public synchronized long getCurrentFileCopiedSize() {
        return currentFileCopiedSize;
    }

    public synchronized void setCurrentFileCopiedSize(long currentFileCopiedSize) {
        this.currentFileCopiedSize = currentFileCopiedSize;
    }

    public synchronized long getCurrentFileTotalSize() {
        return currentFileTotalSize;
    }

    public synchronized void setCurrentFileTotalSize(long currentFileTotalSize) {
        this.currentFileTotalSize = currentFileTotalSize;
    }

    public synchronized long getAllFilesCopiedSize() {
        return allFilesCopiedSize;
    }

    public synchronized void setAllFilesCopiedSize(long allFilesCopiedSize) {
        this.allFilesCopiedSize = allFilesCopiedSize;
    }

    public synchronized long getAllFilesTotalSize() {
        return allFilesTotalSize;
    }

    public synchronized void setAllFilesTotalSize(long allFilesTotalSize) {
        this.allFilesTotalSize = allFilesTotalSize;
    }
}
