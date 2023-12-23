package app.core.file;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import app.core.dto.RelativeFileMetadata;
import exceptions.files.InvalidFileSizeException;
import utils.FormattingUtils;

public class ChecksumComputer {
    private static final Logger LOGGER = LogManager.getLogger(ChecksumComputer.class);

    private Map<String, MessageDigest> messageDigests;

    private long numberOfErrors;

    private long allFilesTotalSize;
    private long allFilesCheckedSize;

    public ChecksumComputer(String[] algorithms) throws NoSuchAlgorithmException {
        messageDigests = new HashMap<>();
        for (String algorithm : algorithms) {
            MessageDigest messageDigest = MessageDigest.getInstance(algorithm);
            messageDigest.reset();
            messageDigests.put(algorithm, messageDigest);
        }
    }

    public void reset() {
        setAllFilesCheckedSize(0);
        numberOfErrors = 0;
        resetDigests();
    }

    private void resetDigests() {
        for (MessageDigest messageDigest : messageDigests.values()) {
            messageDigest.reset();
        }
    }

    public void setAllFilesTotalSize(long allFilesTotalSize) {
        this.allFilesTotalSize = allFilesTotalSize;
    }

    public void setAllFilesCheckedSize(long allFilesCheckedSize) {
        this.allFilesCheckedSize = allFilesCheckedSize;
    }

    public void computeChecksums(Path startFolder, RelativeFileMetadata fileMetadata) throws IOException {
        LOGGER.info("Computing Checksums for file: " + fileMetadata.getRelativePath()
            + " (" + FormattingUtils.humanReadableSize(fileMetadata.getSize()) + "b)...");

        // Checking actual file size
        File file = new File(startFolder.toFile(), fileMetadata.getRelativePath());
        long sizeOfFileInSnapshot = fileMetadata.getSize();
        long sizeOfRealFile = file.length();
        if (sizeOfRealFile != sizeOfFileInSnapshot) {
            String message = "Invalid file size - name: " + file.getAbsolutePath()
                    + ", expected size: " + fileMetadata.getSize() + ", actual size: " + sizeOfRealFile
                    + ". This probably means that the copy data to the folder is not finished yet.";
            throw new InvalidFileSizeException(message);
        }

        // Reading Data from Disk
        long bytesReadForCurrentFileTotal = 0;
        byte[] buffer = new byte[128 * 1024];
        long fileStartTime = System.currentTimeMillis();
        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(file))) {
            long lineDotCounter = 0;
            long startTimeOfTheCurrentDotLine = System.currentTimeMillis();
            long bytesCheckedForTheCurrentDotLine = 0;
            while (bytesReadForCurrentFileTotal < sizeOfRealFile) {
                int read = in.read(buffer);

                // Adding Data to all MessageDigests
                for (MessageDigest messageDigest : messageDigests.values()) {
                    messageDigest.update(buffer, 0, read);
                }

                // Add read size to statistic variables
                bytesReadForCurrentFileTotal += read;
                bytesCheckedForTheCurrentDotLine += read;
                allFilesCheckedSize += read;

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
                fileMetadata.addChecksum(algorithm, checksum);
            }

            // New line if there are some new dots on the current line
            if (lineDotCounter % 10000 != 0) {
                printLineStatistics(lineDotCounter % 10000, startTimeOfTheCurrentDotLine, bytesCheckedForTheCurrentDotLine);
            }

            // Print Statistics of Current File
            long now = System.currentTimeMillis();
            long fileCheckingDuration = now - fileStartTime;
            fileCheckingDuration = fileCheckingDuration == 0 ? 1 : fileCheckingDuration; // Preventing divide by zero
            long speed = 1000 * fileMetadata.getSize() / fileCheckingDuration;
            String message = "File: " + file.getAbsolutePath() + " is OK "
                    + "(" + FormattingUtils.humanReadableSize(fileMetadata.getSize()) + "b),"
                    + " took: " + FormattingUtils.humanReadableTimeMS(fileCheckingDuration) + ","
                    + " avg speed: " + FormattingUtils.humanReadableSize(speed) + "b/s";
            LOGGER.info(message);
        }
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
        String formattedSpeed = FormattingUtils.humanReadableSize(speed);
        System.out.print("Read speed: " + formattedSpeed + "b/s");

        long remainingToCheck = allFilesTotalSize - allFilesCheckedSize;
        if (remainingToCheck > 0 && speed > 0) {
            String formattedRemainingToFill = FormattingUtils.humanReadableSize(remainingToCheck);
            System.out.print(", Remaining: " + formattedRemainingToFill + "b");
            long remainingTime = remainingToCheck / speed;
            String formattedRemainingTime = FormattingUtils.humanReadableTimeS(remainingTime);
            System.out.print(" - " + formattedRemainingTime);
        }

        System.out.println();
    }

    public void addSkippedSize(long size) {
        allFilesCheckedSize += size;
    }

    public long getNumberOfErrors() {
        return numberOfErrors;
    }
}
