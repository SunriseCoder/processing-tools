package backuper.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.DatatypeConverter;

import com.fasterxml.jackson.core.type.TypeReference;

import app.utils.FileUtils;
import app.utils.JSONUtils;
import backuper.common.dto.FileChecksums;

public class ChecksumDatabase {
    private static final int DEFAULT_CHUNK_SIZE = 4 * 1024 * 1024;
    private static final String CHECKSUM_DATABASE_PATH = "file-checksums.json";
    private static final String CHECKSUM_TEMP_FILES_PATH = "temp-file-checksums";

    private static Map<String, FileChecksums> checksums;

    static {
        // Load main database
        System.out.print("Loading File Checksum database... ");
        TypeReference<Map<String, FileChecksums>> typeReference = new TypeReference<Map<String, FileChecksums>>() {};
        try {
            File checksumDatabaseFile = new File(CHECKSUM_DATABASE_PATH);
            if (checksumDatabaseFile.exists()) {
                checksums = JSONUtils.loadFromDisk(checksumDatabaseFile, typeReference);
                System.out.println("Successful");
            } else {
                System.out.println("Database File not found, creating new Database");
                checksums = new HashMap<>();
            }
        } catch (Exception e) {
            System.out.println("Error, creating new Database");
            e.printStackTrace();
            checksums = new HashMap<>();
        }

        // Load all single database files
        System.out.println("Compacting temporary files form last run...");
        FileUtils.createFolderIfNotExists(CHECKSUM_TEMP_FILES_PATH);
        File tempFolder = new File(CHECKSUM_TEMP_FILES_PATH);
        File[] tempFiles = tempFolder.listFiles();
        for (int i = 0; i < tempFiles.length; i++) {
            File tempFile = tempFiles[i];
            System.out.print("Reading " + (i + 1) + " of " + tempFiles.length + ": " + tempFile.getName() + "... ");
            try {
                Map<String, FileChecksums> subDatabase = JSONUtils.loadFromDisk(tempFile, typeReference);
                checksums.putAll(subDatabase);
                System.out.println("Success");
            } catch (IOException e) {
                System.out.println("Failed");
                e.printStackTrace();
            }
        }

        // Deleting records of the files which do not existing anymore
        System.out.println("Deleting records of the files which do not exist anymore...");
        System.out.println("Number of records before the cleanup: " + checksums.size());
        int total = checksums.size();
        int counter = 0;
        Iterator<Entry<String, FileChecksums>> iterator = checksums.entrySet().iterator();
        while (iterator.hasNext()) {
            System.out.println("\r" + counter++ + " of " + total + "\r");
            Entry<String, FileChecksums> entry = iterator.next();
            File file = new File(entry.getKey());
            if (!file.exists()) {
                System.out.print("Deleting record for file: " + entry.getKey() + "... Done");
                iterator.remove();
            }
        }
        System.out.println("Number of records after the cleanup: " + checksums.size());

        // Saving database to a single file
        System.out.print("Saving optimized database... ");
        try {
            JSONUtils.saveToDisk(checksums, CHECKSUM_DATABASE_PATH);
            System.out.println("Success");
        } catch (IOException e) {
            System.out.println("Failed");
            e.printStackTrace();
            System.exit(-1);
        }

        // Deleting all temp checksum files
        for (int i = 0; i < tempFiles.length; i++) {
            File tempFile = tempFiles[i];
            System.out.print("Deleting " + (i + 1) + " of " + tempFiles.length + ": " + tempFile.getName() + "... ");
            System.out.println(tempFile.delete() ? "Success" : "Failed");
        }

        System.out.println("File Checksum Database has been initialized successfully");
    }

    public synchronized static String getFileChecksum(String path, long size, FileTime lastModifiedTime) throws Exception {
        FileChecksums checksums = getCheckums(path, size, lastModifiedTime);
        String result = checksums.getFileChecksum();
        return result;
    }

    public synchronized static List<String> getChunkChecksums(String path, long size, FileTime lastModifiedTime, int chunkSize)
            throws Exception {
        FileChecksums checksums = getCheckums(path, size, lastModifiedTime);
        List<String> chunkChecksums = checksums.getChunkChecksum(chunkSize);
        if (chunkChecksums == null) {
            chunkChecksums = calculateChunkChecksums(path, chunkSize);
            checksums.addChunkChecksums(chunkSize, chunkChecksums);
            saveToTmpChecksumFile(path, checksums);
        }

        return chunkChecksums;
    }

    private static FileChecksums getCheckums(String path, long size, FileTime lastModifiedTime) throws Exception {
        FileChecksums result = checksums.get(path);
        if (result == null || result.getSize() != size || !result.getLastModifiedTime().equals(lastModifiedTime)) {
            result = calculateChecksums(path, result == null ? null : new ArrayList<>(result.getChunkSizes()));
            if (result == null) {
                checksums.remove(path);
            } else {
                saveToTmpChecksumFile(path, result);
                checksums.put(result.getFilename(), result);
            }
        }

        return result;
    }

    private static FileChecksums calculateChecksums(String path, List<Integer> chunkSizes) throws Exception {
        File file = new File(path);
        if (!file.exists() || file.isDirectory()) {
            return null;
        }

        if (chunkSizes == null) {
            chunkSizes = new ArrayList<>();
            chunkSizes.add(DEFAULT_CHUNK_SIZE);
        }

        FileChecksums result = new FileChecksums();
        result.setFilename(path);
        BasicFileAttributes attributes = Files.readAttributes(Paths.get(path), BasicFileAttributes.class);
        result.setSize(attributes.size());
        result.setLastModifiedTime(attributes.lastModifiedTime());

        List<ChunkDigester> digesters = new ArrayList<>();
        for (Integer chunkSize : chunkSizes) {
            ChunkDigester digester = new ChunkDigester();
            digester.chunkSize = chunkSize;
            digester.fileChecksum = result;
            digesters.add(digester);
        }

        MessageDigest fileMD = MessageDigest.getInstance("MD5");
        int bufferSize = chunkSizes.stream().mapToInt(e -> e.intValue()).min().getAsInt();
        byte[] buffer = new byte[bufferSize];
        try (FileInputStream fis = new FileInputStream(file)) {
            int read;
            do {
                read = fis.read(buffer);
                if (read >= 0) {
                    fileMD.update(buffer, 0, read);
                    for (ChunkDigester digester : digesters) {
                        digester.update(buffer, read);
                    }
                }
            } while (read > 0);
            for (ChunkDigester digester : digesters) {
                digester.finish();
            }
            String fileChecksum = DatatypeConverter.printHexBinary(fileMD.digest()).toLowerCase();
            result.setFileChecksum(fileChecksum);
            return result;
        } catch (FileNotFoundException e) {
            // TODO Log error
            e.printStackTrace();
        } catch (Exception e) {
            // TODO Log error
            e.printStackTrace();
        }

        return null;
    }

    private static List<String> calculateChunkChecksums(String path, int chunkSize) {
        File file = new File(path);
        if (!file.exists() || file.isDirectory()) {
            return null;
        }

        List<String> result = new ArrayList<>();

        byte[] buffer = new byte[chunkSize];
        try (FileInputStream fis = new FileInputStream(file)) {
            int read;
            do {
                read = fis.read(buffer);
                if (read >= 0) {
                    MessageDigest chunkMD = MessageDigest.getInstance("MD5");
                    chunkMD.update(buffer, 0, read);
                    String chunkChecksum = DatatypeConverter.printHexBinary(chunkMD.digest()).toLowerCase();
                    result.add(chunkChecksum);
                }
            } while (read > 0);
            return result;
        } catch (FileNotFoundException e) {
            // TODO Log error
            e.printStackTrace();
        } catch (Exception e) {
            // TODO Log error
            e.printStackTrace();
        }

        return null;
    }

    private static void saveToTmpChecksumFile(String path, FileChecksums checksums) throws IOException {
        Map<String, FileChecksums> subDatabase = new HashMap<>();
        subDatabase.put(path, checksums);
        File tempFile = generateTempFile();
        JSONUtils.saveToDisk(subDatabase, tempFile);
    }

    private static File generateTempFile() {
        File tempFile;
        long filename = System.currentTimeMillis();
        do {
            StringBuilder sb = new StringBuilder();
            sb.append(filename).append(".json");
            while (sb.length() < 30) {
                sb.insert(0, "0");
            }
            tempFile = new File(CHECKSUM_TEMP_FILES_PATH, sb.toString());
            filename++;
        } while (tempFile.exists());
        return tempFile;
    }

    private static class ChunkDigester {
        public int chunkSize;
        public FileChecksums fileChecksum;

        private MessageDigest messageDigest;
        private int dataCounter;

        public void update(byte[] buffer, int read) throws Exception {
            if (messageDigest == null) {
                messageDigest = MessageDigest.getInstance("MD5");
            }
            messageDigest.update(buffer, 0, read);
            dataCounter += read;
            if (dataCounter == chunkSize) {
                doDigest();
            } else if (dataCounter > chunkSize) {
                throw new IllegalStateException("This actually shouldn't happen. Digested more than chunk size. "
                        + "Digested: " + dataCounter + ", Chunk size: " + chunkSize);
            }
        }

        public void finish() {
            if (messageDigest != null) {
                doDigest();
            }
        }

        private void doDigest() {
            String chunkChecksum = DatatypeConverter.printHexBinary(messageDigest.digest()).toLowerCase();
            fileChecksum.addChunkChecksum(chunkSize, chunkChecksum);
            messageDigest = null;
            dataCounter = 0;
        }
    }
}
