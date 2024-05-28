package app.checksum;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import app.digest.XorProvider;
import app.utils.FormattingUtils;

//TODO  1. Replace embedded progress printer with usage of ProgressPrinter
//      2. Make setProgressPrinter(ProgressPrinter progressPrinter) setter
//      3. Add: if (progressPrinter != null) for all interactions with progressPrinter to work without printing progress
//      4. All places where this class is instantiated, use: setProgressPrinter(new DottedProgressPrinter());
public class ChecksumComputer {
    private static final Logger LOGGER = LogManager.getLogger(ChecksumComputer.class);

    private Map<String, MessageDigest> messageDigests;

    private long numberOfErrors;

    private long allFilesSize;
    private long allFilesProgress;

    public ChecksumComputer(List<String> algorithms) throws NoSuchAlgorithmException {
        Security.addProvider(new XorProvider());

        messageDigests = new HashMap<>();
        for (String algorithm : algorithms) {
            MessageDigest messageDigest = MessageDigest.getInstance(algorithm);
            messageDigest.reset();
            messageDigests.put(algorithm, messageDigest);
        }
    }

    public void reset() {
        setAllFilesProgress(0);
        numberOfErrors = 0;
    }

    public void setAllFilesSize(long allFilesSize) {
        this.allFilesSize = allFilesSize;
    }

    public void setAllFilesProgress(long allFilesProgress) {
        this.allFilesProgress = allFilesProgress;
    }

    // TODO Rewrite using Java NIO - Path, FileChannel, ByteBuffer
    //      Example of the usage is in PathUtils.copyFile(...) method
    public Map<String, String> computeChecksums(File file) throws IOException {
        Map<String, String> checksums = new HashMap<>();

        // Reading Data from Disk
        long bytesReadForCurrentFileTotal = 0;
        byte[] buffer = new byte[128 * 1024];
        long fileStartTime = System.currentTimeMillis();
        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(file))) {
            long lineDotCounter = 0;
            long startTimeOfTheCurrentDotLine = System.currentTimeMillis();
            long bytesCheckedForTheCurrentDotLine = 0;
            long fileSize = file.length();
            while (bytesReadForCurrentFileTotal < fileSize ) {
                int read = in.read(buffer);

                // Adding Data to all MessageDigests
                for (MessageDigest messageDigest : messageDigests.values()) {
                    messageDigest.update(buffer, 0, read);
                }

                // Add read size to statistic variables
                bytesReadForCurrentFileTotal += read;
                bytesCheckedForTheCurrentDotLine += read;
                allFilesProgress += read;

                // Printing progress
                lineDotCounter++;
                if (lineDotCounter % 100 == 0) {
                    System.out.print(".");
                }
                if (lineDotCounter % 10000 == 0) {
                    printLineStatistics(lineDotCounter % 10000, startTimeOfTheCurrentDotLine, bytesCheckedForTheCurrentDotLine);
                    startTimeOfTheCurrentDotLine = System.currentTimeMillis();
                    bytesCheckedForTheCurrentDotLine = 0;
                }
            }

            // Finalizing MessageDigests and putting Checksums to FileMetadata
            for (Entry<String, MessageDigest> messageDigestEntry : messageDigests.entrySet()) {
                String algorithm = messageDigestEntry.getKey();
                MessageDigest messageDigest = messageDigestEntry.getValue();
                String checksum = new BigInteger(1, messageDigest.digest()).toString(16);
                checksums.put(algorithm, checksum);
            }

            // New line if there are some new dots on the current line
            if (lineDotCounter % 10000 != 0) {
                printLineStatistics(lineDotCounter % 10000, startTimeOfTheCurrentDotLine, bytesCheckedForTheCurrentDotLine);
            }

            // Print Statistics of Current File
            long now = System.currentTimeMillis();
            long fileCheckingDuration = now - fileStartTime;
            fileCheckingDuration = fileCheckingDuration == 0 ? 1 : fileCheckingDuration; // Preventing divide by zero
            long speed = 1000 * fileSize / fileCheckingDuration;
            String message = "File: " + file.getAbsolutePath() + " is OK "
                    + "(" + FormattingUtils.humanReadableSizeBi(fileSize) + "b),"
                    + " took: " + FormattingUtils.humanReadableTimeMS(fileCheckingDuration) + ","
                    + " avg speed: " + FormattingUtils.humanReadableSizeBi(speed) + "b/s";
            LOGGER.info(message);
        }

        return checksums;
    }

    private void printLineStatistics(long lineDotRemainder, long lineStartTime, long lineBytesChecked) {
        long now = System.currentTimeMillis();
        long currentLineTime = now - lineStartTime;
        if (currentLineTime < 1000) {
            System.out.println();
            return;
        }

        if (lineDotRemainder > 0) {
            for (long i = lineDotRemainder; i <= 10000; i += 100) {
                System.out.print(" ");
            }
        }
        System.out.print("   ");

        long speed = 1000 * lineBytesChecked / currentLineTime;
        String formattedSpeed = FormattingUtils.humanReadableSizeBi(speed);
        System.out.print("Read speed: " + formattedSpeed + "b/s");

        long remainingToCheck = allFilesSize - allFilesProgress;
        if (remainingToCheck > 0 && speed > 0) {
            String formattedRemainingToFill = FormattingUtils.humanReadableSizeBi(remainingToCheck);
            System.out.print(", Remaining: " + formattedRemainingToFill + "b");
            long remainingTime = remainingToCheck / speed;
            String formattedRemainingTime = FormattingUtils.humanReadableTimeS(remainingTime);
            System.out.print(" - " + formattedRemainingTime);
        }

        System.out.println();
    }

    public void addProgress(long progress) {
        allFilesProgress += progress;
    }

    public long getNumberOfErrors() {
        return numberOfErrors;
    }
}
