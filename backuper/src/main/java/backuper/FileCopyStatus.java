package backuper;

import java.text.DecimalFormat;

import backuper.helpers.FormattingHelper;
import backuper.helpers.PrintHelper;

public class FileCopyStatus {
    private static final int COPYING_STATUS_INTERVAL = 1000;

    private DecimalFormat percentFormat = new DecimalFormat("0.00%");

    private long startCopyingTime;
    private long currentFileCopiedSize;
    private long currentFileTotalSize;
    private long allFilesCopiedSize;
    private long allFilesTotalSize;
    private long lastPrintTime;
    private long lastPrintAllFilesCopiedSize;


    public void reset() {
        currentFileCopiedSize = 0;
        currentFileTotalSize = 0;
        allFilesCopiedSize = 0;
        allFilesTotalSize = 0;
        lastPrintAllFilesCopiedSize = 0;
    }

    public void start() {
        startCopyingTime = System.currentTimeMillis();
    }

    public void addAllFilesTotalSize(long delta) {
        this.allFilesTotalSize += delta;
    }

    public void addCopiedSize(long delta) {
        this.currentFileCopiedSize += delta;
        this.allFilesCopiedSize += delta;
    }

    public void printCopyProgress() {
        long now = System.currentTimeMillis();
        long timeDelta = now - lastPrintTime;
        if (timeDelta >= COPYING_STATUS_INTERVAL) {
            // 25Mb of 4.3G (1.05%) / 35Gb of 2Tb (1.25%) / 75Mb/s / Eta: 101:01:52
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
            long copiedDelta = allFilesCopiedSize - lastPrintAllFilesCopiedSize;
            long speed = copiedDelta * 1000 / timeDelta;
            String speedStr = FormattingHelper.humanReadableSize(speed);
            speedStr = String.format("%6s", speedStr);
            message += speedStr + "/s / ";

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
            PrintHelper.printAndReturn(message);

            lastPrintTime = now;
            lastPrintAllFilesCopiedSize = allFilesCopiedSize;
        }
    }

    public long getCurrentFileCopiedSize() {
        return currentFileCopiedSize;
    }

    public void setCurrentFileCopiedSize(long currentFileCopiedSize) {
        this.currentFileCopiedSize = currentFileCopiedSize;
    }

    public long getCurrentFileTotalSize() {
        return currentFileTotalSize;
    }

    public void setCurrentFileTotalSize(long currentFileTotalSize) {
        this.currentFileTotalSize = currentFileTotalSize;
    }

    public long getAllFilesCopiedSize() {
        return allFilesCopiedSize;
    }

    public void setAllFilesCopiedSize(long allFilesCopiedSize) {
        this.allFilesCopiedSize = allFilesCopiedSize;
    }

    public long getAllFilesTotalSize() {
        return allFilesTotalSize;
    }

    public void setAllFilesTotalSize(long allFilesTotalSize) {
        this.allFilesTotalSize = allFilesTotalSize;
    }
}
