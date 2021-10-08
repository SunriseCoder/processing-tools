package backuper.client;

import java.text.DecimalFormat;

import backuper.common.helpers.PrintHelper;
import utils.FormattingUtils;
import utils.MathUtils;

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
        long now = System.currentTimeMillis();
        startCopyingTime = now;
        currentFileStartTime = now;
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

    public synchronized void printCopyProgress(boolean force) {
        long now = System.currentTimeMillis();
        long timeDelta = now - lastPrintTime;
        long copiedDelta = allFilesCopiedSize - lastPrintAllFilesCopiedSize;
        if (force || (timeDelta >= COPYING_STATUS_INTERVAL && copiedDelta > 0)) {
            // 25Mb of 4.3G (1.05%) / 35Gb of 2Tb (1.25%) / avg: 75Mb/s / Eta: 101:01:52/102:07:25
            // Current file
            double currentPercent = (double) currentFileCopiedSize / currentFileTotalSize;
            StringBuilder sb = new StringBuilder();
            sb.append(FormattingUtils.humanReadableSize(currentFileCopiedSize));
            sb.append("b of ").append(FormattingUtils.humanReadableSize(currentFileTotalSize));
            sb.append("b (").append(percentFormat.format(currentPercent)).append(") | ");

            // All files
            double allFilesPercent = (double) allFilesCopiedSize / allFilesTotalSize;
            sb.append(FormattingUtils.humanReadableSize(allFilesCopiedSize));
            sb.append("b of ").append(FormattingUtils.humanReadableSize(allFilesTotalSize));
            sb.append("b (").append(percentFormat.format(allFilesPercent)).append(") | ");

            // Speed
            long currentFileTimeDelta = now - currentFileStartTime;
            long speed = currentFileTimeDelta > 0 ? currentFileCopiedSize * 1000 / currentFileTimeDelta : 0;
            sb.append("avg: ").append(FormattingUtils.humanReadableSize(speed)).append("b/s | ");

            // Remaining estimation
            sb.append("Eta: ");

            // Algorithm based on last status update period speed
            long remainingSize = allFilesTotalSize - allFilesCopiedSize;
            if (speed > 0) {
                long remaining = remainingSize / speed;
                sb.append(FormattingUtils.humanReadableTimeS(remaining));
            } else {
                sb.append("Unknown");
            }

            // Algorithm based on overall average speed
            sb.append("/");
            long copiedAllFilesTime = now - startCopyingTime;
            if (copiedAllFilesTime > 0 && allFilesPercent > 0) {
                double totalTime = copiedAllFilesTime / allFilesPercent;
                long remainingTime = MathUtils.roundToLong(totalTime * (1 - allFilesPercent) / 1000);
                sb.append(FormattingUtils.humanReadableTimeS(remainingTime));
            } else {
                sb.append("Unknown");
            }

            // Saving actual length of current message
            int currentMesssageLength = sb.length();

            // Adding spaces at the end to overwrite the end of the last message
            while (sb.length() < lastMessageLength) {
                sb.append(" ");
            }
            PrintHelper.printAndReturn(sb.toString());
            lastMessageLength = currentMesssageLength;

            lastPrintTime = now;
            lastPrintAllFilesCopiedSize = allFilesCopiedSize;
        }
    }

    public synchronized void printCopyResults() {
        long now = System.currentTimeMillis();
        long timeDelta = now - startCopyingTime;
        if (timeDelta > 0) {
            long speed = allFilesCopiedSize * 1000 / timeDelta;

            StringBuilder sb = new StringBuilder();
            sb.append("Copied: ").append(FormattingUtils.humanReadableSize(allFilesCopiedSize))
                    .append("b, took: ").append(FormattingUtils.humanReadableTimeS(timeDelta / 1000))
                    .append(", avg: ").append(FormattingUtils.humanReadableSize(speed)).append("b/s");

            PrintHelper.println(sb.toString());
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

    public long getCurrentFileStartTime() {
        return currentFileStartTime;
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
