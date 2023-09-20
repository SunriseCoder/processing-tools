package app.core.file;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import app.core.dto.fs.FileSystemFile;
import utils.FormattingUtils;

public class FileChecker {
    private static final Logger LOGGER = LogManager.getLogger(FileChecker.class);

    private long numberOfErrors;

    private long allFilesTotalSize;
    private long allFilesCheckedSize;

    public void setAllFilesTotalSize(long allFilesTotalSize) {
        this.allFilesTotalSize = allFilesTotalSize;
    }

    public void setAllFilesCheckedSize(long allFilesCheckedSize) {
        this.allFilesCheckedSize = allFilesCheckedSize;
    }

    public void checkFile(File file, FileSystemFile fileMetadata) throws NoSuchAlgorithmException, IOException {
        LOGGER.info("Checking file: " + file.getAbsolutePath()
                + "(" + FormattingUtils.humanReadableSize(fileMetadata.getSize()) + "b) ...");

        Map<String, MessageDigest> messageDigests = createDigests(fileMetadata);

        long fileSize = fileMetadata.getSize();
        if (file.length() != fileSize) {
            LOGGER.error("Invalid file size - name: " + file.getAbsolutePath()
                    + ", expected size: " + fileMetadata.getSize() + ", actual size: " + file.length());
            numberOfErrors++;
            return;
        }

        long bytesReadForCurrentFileTotal = 0;
        byte[] buffer = new byte[128 * 1024];
        long fileStartTime = System.currentTimeMillis();
        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(file))) {
            long lineDotCounter = 0;
            long lineStartTime = System.currentTimeMillis();
            long lineBytesChecked = 0;
            while (bytesReadForCurrentFileTotal < fileSize) {
                int read = in.read(buffer);
                bytesReadForCurrentFileTotal += read;
                lineBytesChecked += read;
                allFilesCheckedSize += read;

                for (MessageDigest messageDigest : messageDigests.values()) {
                    messageDigest.update(buffer, 0, read);
                }

                lineDotCounter++;
                if (lineDotCounter % 100 == 0) {
                    System.out.print(".");
                }
                if (lineDotCounter % 10000 == 0) {
                    printLineStatistics(lineDotCounter % 10000, lineStartTime, lineBytesChecked);
                    lineStartTime = System.currentTimeMillis();
                    lineBytesChecked = 0;
                }
            }

            // New line if there are some new dots on the current line
            if (lineDotCounter % 10000 != 0) {
                printLineStatistics(lineDotCounter % 10000, lineStartTime, lineBytesChecked);
            }
        }

        boolean isFileOk = true;
        for (Entry<String, MessageDigest> entry : messageDigests.entrySet()) {
            String algorithm = entry.getKey();
            MessageDigest messageDigest = entry.getValue();
            String checksum = new BigInteger(1, messageDigest.digest()).toString(16);

            if (!checksum.equals(fileMetadata.getChecksums().get(algorithm))) {
                LOGGER.error("Checksum mismatch, file: " + file.getAbsolutePath()
                            + ", algorithm: " + algorithm);
                numberOfErrors++;
                isFileOk = false;
            }
        }

        if (isFileOk) {
            fileMetadata.setChecked(true);

            long now = System.currentTimeMillis();
            long fileCheckingDuration = now - fileStartTime;
            long speed = 1000 * fileMetadata.getSize() / fileCheckingDuration;
            String message = "File: " + file.getAbsolutePath() + " is OK "
                    + "(" + FormattingUtils.humanReadableSize(fileMetadata.getSize()) + "b),"
                    + " took: " + FormattingUtils.humanReadableTimeMS(fileCheckingDuration) + ","
                    + " avg speed: " + FormattingUtils.humanReadableSize(speed) + "b/s";
            LOGGER.info(message);
        }
    }

    private Map<String, MessageDigest> createDigests(FileSystemFile fileMetadata) throws NoSuchAlgorithmException {
        Map<String, MessageDigest> digests = new HashMap<>();
        for (String algorithm : fileMetadata.getChecksums().keySet()) {
            addMessageDigest(algorithm, digests);
        }
        return digests;
    }

    private void addMessageDigest(String algorithm, Map<String, MessageDigest> digests) throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance(algorithm);
        messageDigest.reset();
        digests.put(algorithm, messageDigest);
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

    public long getNumberOfErrors() {
        return numberOfErrors;
    }
}
