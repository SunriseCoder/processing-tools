package app.core.file;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import app.config.Configuration;
import app.config.Configuration.Keys;
import app.core.dto.fs.FileSystemFile;
import utils.FileUtils;
import utils.FormattingUtils;

public class FileCreator {
    private static final Logger LOGGER = LogManager.getLogger(FileCreator.class.getName());
    private static final Random RND = new Random();

    private File rootFolder;
    private File tmpFolder;

    public FileCreator(File rootFolder, File tmpFolder) {
        this.rootFolder = rootFolder;
        this.tmpFolder = tmpFolder;
    }

    public void fillFile(FileSystemFile file) throws IOException, NoSuchAlgorithmException {
        File absoluteFile = new File(rootFolder.getAbsolutePath() + file.getPath());
        File tmpFile = FileUtils.createFile(tmpFolder.getAbsolutePath() + "/" + file.getName(), true);
        LOGGER.info("Creating file: " + absoluteFile.getAbsolutePath()
                + " (" + tmpFile.getAbsolutePath() + ") -"
                + " (" + FormattingUtils.humanReadableSize(file.getSize()) + "b) ...");

        Map<String, MessageDigest> messageDigests = createDigests();

        long fileSize = file.getSize();
        long bytesWritten = 0;
        byte[] buffer = new byte[128 * 1024];
        long fileStartTime = System.currentTimeMillis();
        try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(tmpFile))) {
            long lineDotCounter = 0;
            long lineStartTime = System.currentTimeMillis();
            long lineBytesWritten = 0;
            while (bytesWritten < fileSize) {
                RND.nextBytes(buffer);

                long remainingBytes = fileSize - bytesWritten;
                int sizeToWrite = remainingBytes < buffer.length ? (int) remainingBytes : buffer.length;

                out.write(buffer, 0, sizeToWrite);
                bytesWritten += sizeToWrite;
                lineBytesWritten += sizeToWrite;

                for (MessageDigest messageDigest : messageDigests.values()) {
                    messageDigest.update(buffer, 0, sizeToWrite);
                }

                lineDotCounter++;
                if (lineDotCounter % 100 == 0) {
                    System.out.print(".");
                }
                if (lineDotCounter % 10000 == 0) {
                    printLineStatistics(lineDotCounter % 10000, lineStartTime, lineBytesWritten, absoluteFile);
                    lineStartTime = System.currentTimeMillis();
                    lineBytesWritten = 0;
                }
            }

            out.flush();

            // New line if there are some new dots on the current line
            if (lineDotCounter % 10000 != 0) {
                printLineStatistics(lineDotCounter % 10000, lineStartTime, lineBytesWritten, absoluteFile);
            }
        }

        absoluteFile.getParentFile().mkdirs();
        tmpFile.renameTo(absoluteFile);

        for (Entry<String, MessageDigest> entry : messageDigests.entrySet()) {
            String algorithm = entry.getKey();
            MessageDigest messageDigest = entry.getValue();
            String checksum = new BigInteger(1, messageDigest.digest()).toString(16);
            file.setChecksum(algorithm, checksum);
        }

        long now = System.currentTimeMillis();
        long fileCreatingDuration = now - fileStartTime;
        long speed = 1000 * file.getSize() / fileCreatingDuration;
        String message = "Created file: " + absoluteFile.getAbsolutePath()
                + "(" + FormattingUtils.humanReadableSize(file.getSize()) + "b),"
                + " took: " + FormattingUtils.humanReadableTimeMS(fileCreatingDuration) + ","
                + " avg speed: " + FormattingUtils.humanReadableSize(speed) + "b/s";
        LOGGER.info(message);
    }

    private void printLineStatistics(long lineDotRemainder, long lineStartTime, long lineBytesWritten, File file) {
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

        long speed = 1000 * lineBytesWritten / currentLineTime;
        String formattedSpeed = FormattingUtils.humanReadableSize(speed);
        System.out.print("Write speed: " + formattedSpeed + "b/s");

        long minFreeSpace = Long.parseLong(Configuration.getValue(Keys.MinFreeSpace));
        long freeSpace = rootFolder.getFreeSpace();
        long remainingToFill = freeSpace - minFreeSpace;
        if (remainingToFill > 0) {
            String formattedRemainingToFill = FormattingUtils.humanReadableSize(remainingToFill);
            System.out.print(", Remaining: " + formattedRemainingToFill + "b");
            long remainingTime = remainingToFill / speed;
            String formattedRemainingTime = FormattingUtils.humanReadableTimeS(remainingTime);
            System.out.print(" - " + formattedRemainingTime);
        }

        System.out.println();
    }

    private Map<String, MessageDigest> createDigests() throws NoSuchAlgorithmException {
        Map<String, MessageDigest> digests = new HashMap<>();
        addMessageDigest("XOR", digests);
        //addMessageDigest("MD5", digests);
        //addMessageDigest("SHA-256", digests);
        //addMessageDigest("SHA-512", digests);
        return digests;
    }

    private void addMessageDigest(String algorithm, Map<String, MessageDigest> digests) throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance(algorithm);
        messageDigest.reset();
        digests.put(algorithm, messageDigest);
    }
}
