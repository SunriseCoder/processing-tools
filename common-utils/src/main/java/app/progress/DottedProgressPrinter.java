package app.progress;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import app.utils.FormattingUtils;

public class DottedProgressPrinter extends ProgressPrinter {
    private static final Logger LOGGER = LogManager.getLogger(DottedProgressPrinter.class);

    // Dot
    private long dotDataSize;
    private long currentDotProgress;

    // Line
    private int dotsPerLine;
    private long currentLineStartTime;
    private long currentLineProgress;
    private int dotsOnCurrentLine;

    // File
    private String currentFileName;
    private long currentFileStartTime;
    private long currentFileProgress;
    private long currentFileSize;

    // All Files
    private long allFilesSize;
    private long allFilesProgress;

    public void setDotDataSize(long dotDataSize) {
        this.dotDataSize = dotDataSize;
    }

    public void setDotsPerLine(int dotsPerLine) {
        this.dotsPerLine = dotsPerLine;
    }

    public void reset(long allFilesSize) {
        long now = System.currentTimeMillis();

        // Dot
        currentDotProgress = 0;

        // Line
        currentLineStartTime = now;
        currentLineProgress = 0;
        dotsOnCurrentLine = 0;

        // File
        currentFileStartTime = now;
        currentFileProgress = 0;
        currentFileSize = 0;

        // All Files
        this.allFilesSize = allFilesSize;
        allFilesProgress = 0;
    }

    @Override
    public void startNewFile(String fileName, long fileSize) {
        LOGGER.info("Starting: " + fileName + " (" + FormattingUtils.humanReadableSizeBi(fileSize) + "b)...");

        long now = System.currentTimeMillis();

        // Dot
        currentDotProgress = 0;

        // Line
        currentLineStartTime = now;
        currentLineProgress = 0;
        dotsOnCurrentLine = 0;

        // File
        currentFileName = fileName;
        currentFileStartTime = now;
        currentFileProgress = 0;
        currentFileSize = fileSize;
    }

    @Override
    public void addProgress(long progress) {
        if (dotDataSize < 1) {
            throw new IllegalStateException("Parameter dotDataSize is not set or wrong,"
                    + " must be long number of bytes which is represented by one dot.");
        }

        currentDotProgress += progress;
        currentLineProgress += progress;
        currentFileProgress += progress;
        allFilesProgress += progress;

        while (currentDotProgress > dotDataSize) {
            System.out.print(".");
            dotsOnCurrentLine++;
            currentDotProgress -= dotDataSize;

            if (dotsOnCurrentLine == dotsPerLine) {
                printLineStatistics();

                currentLineProgress = 0;
                dotsOnCurrentLine = 0;
                currentLineStartTime = System.currentTimeMillis();
            }
        }
    }

    @Override
    public void finishCurrentFile() {
        if (dotsOnCurrentLine > 0) {
            printLineStatistics();
        }
        printFileStatictics();
    }

    private void printLineStatistics() {
        long now = System.currentTimeMillis();
        long currentLineTime = now - currentLineStartTime;
        if (currentLineTime < 1000) {
            System.out.println();
            return;
        }

        // Filling remaining line space with spaces until the line end
        while (dotsOnCurrentLine < dotsPerLine) {
            System.out.print(" ");
            dotsOnCurrentLine++;
        }
        System.out.print("   ");

        // File:
        String formattedCurrentFileProgress = FormattingUtils.humanReadableSizeBi(currentFileProgress);
        String formattedCurrentFileSize = FormattingUtils.humanReadableSizeBi(currentFileSize);
        System.out.print("File: " + formattedCurrentFileProgress + "b of " + formattedCurrentFileSize + "b");

        // Speed:
        long currentLineSpeed = 1000 * currentLineProgress / currentLineTime;
        String formattedCurrentLineSpeed = FormattingUtils.humanReadableSizeBi(currentLineSpeed);
        System.out.print(", Speed: " + formattedCurrentLineSpeed + "b/s");

        // Remaining:
        long allFilesRemainingData = allFilesSize - allFilesProgress;
        if (allFilesRemainingData > 0 && currentLineSpeed > 0) {
            String formattedAllFilesRemainingData = FormattingUtils.humanReadableSizeBi(allFilesRemainingData);
            System.out.print(", Remaining: " + formattedAllFilesRemainingData + "b");
            long allFilesRemainingTime = allFilesRemainingData / currentLineSpeed;
            String formattedAllFilesRemainingTime = FormattingUtils.humanReadableTimeS(allFilesRemainingTime);
            System.out.print(" - " + formattedAllFilesRemainingTime);
        }

        System.out.println();
    }

    private void printFileStatictics() {
        long now = System.currentTimeMillis();
        long currentFileTime = now - currentFileStartTime;

        // FileName, Took time
        StringBuilder fileStatistics = new StringBuilder();
        fileStatistics.append("Done: ").append(currentFileName)
                .append(" (").append(FormattingUtils.humanReadableSizeBi(currentFileSize)).append("b), ")
                .append("took: ").append(FormattingUtils.humanReadableTimeMS(currentFileTime));

        // Average Speed
        if (currentFileTime > 0) {
            long currentFileSpeed = 1000 * currentFileSize / currentFileTime;
            fileStatistics.append(", avg speed: ").append(FormattingUtils.humanReadableSizeBi(currentFileSpeed)).append("b/s");
        }

        LOGGER.info(fileStatistics.toString());
    }
}
