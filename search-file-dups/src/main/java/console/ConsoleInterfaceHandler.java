package console;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.TimeZone;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.StdDateFormat;

import core.dto.Database;
import core.dto.FileMetadata;
import core.file.FolderScanner;
import progress.ProgressPrinter;
import utils.FileUtils;
import utils.FormattingUtils;
import utils.JSONUtils;

public class ConsoleInterfaceHandler {
    private static final String DATABASE_FOLDER = "database";
    private static final String LOGS_FOLDER = "logs";
    private static final long DATABASE_SAVE_INTERVAL_MILLIS = 5 * 60 * 1000; // 5 minutes


    private Scanner scanner;
    private List<Database> databases;
    private SimpleDateFormat logFilenameDateFormatter;

    public ConsoleInterfaceHandler() {
        scanner = new Scanner(System.in);
        databases = new ArrayList<>();
        logFilenameDateFormatter = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
    }

    public void start() throws Exception {
        System.out.println("Application is starting...");

        FileUtils.createFolderIfNotExists(DATABASE_FOLDER);
        FileUtils.createFolderIfNotExists(LOGS_FOLDER);

        System.out.println("Loading databases...");

        Path databaseFolderPath = Paths.get(DATABASE_FOLDER);
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(databaseFolderPath)) {
            for (Path path : ds) {
                if (Files.isSymbolicLink(path) || Files.isDirectory(path)) {
                    continue;
                }

                TypeReference<Database> typeReference = new TypeReference<Database>() {};
                Database database = JSONUtils.loadFromDisk(path.toFile(), typeReference);
                database.linkFiles();
                this.databases.add(database);
                System.out.println("Database " + database.getName() + " has been loaded successfully.");
            }
        }

        System.out.println("Application started successfully");

        mainMenu();
    }

    private void mainMenu() throws Exception {
        String input;
        while (true) {
            System.out.print("Select action: [1] Status, [2] Add folder, [3] Update folder "
                    + "[5] Find Dups in a Folder, [6] Compare Folders, [0] Exit\n");
            input = scanner.next();
            switch (input) {
            case "1":
                printStatus();
                break;
            case "2":
                addFolder();
                break;
            case "3":
                updateFolder();
                break;
            case "5":
                findDuplicationsInDatabase();
                break;
            case "6":
                compareDatabases();
                break;
            case "0":
                System.out.println("Exiting...");
                scanner.close();
                System.exit(0);
            default:
                System.out.println("Unsupported command, please try again");
            }
        }
    }

    private void printStatus() {
        System.out.println("Current Database Status:");
        for (int i = 0; i < databases.size(); i++) {
            Database database = databases.get(i);
            System.out.println("[" + i + "] - " + database.getName() + " - " + database.getPath()
                    + " - " + database.getFiles().size() + " file(s), updated: " + database.getLastUpdated());
        }
    }

    private void addFolder() throws Exception {
        Database database = new Database();

        // Database Name
        String input;
        boolean inputAcceptedFlag = false;
        while (!inputAcceptedFlag) {
            System.out.println("Please enter Database Name or [Enter] to cancel operation: ");
            input = scanner.next();
            input = input.trim();
            if ("".equalsIgnoreCase(input)) {
                System.out.println("Operation cancelled");
                return;
            }

            inputAcceptedFlag = true;
            database.setName(input);
        }

        // Folder Path
        inputAcceptedFlag = false;
        while (!inputAcceptedFlag) {
            System.out.println("Please enter Database Folder Absolute Path or [Enter] to cancel operation: ");
            input = scanner.next();
            input = input.trim();
            if ("".equalsIgnoreCase(input)) {
                System.out.println("Operation cancelled");
                return;
            }

            File file = new File(input);
            if (file.exists() && file.isDirectory()) {
                inputAcceptedFlag = true;
                database.setPath(input);
            }
        }

        // Saving Database to disk
        saveDatabase(database);

        // Adding database to the program
        databases.add(database);
    }

    private void updateFolder() throws Exception {
        // Choose Folder to update
        int dbIndex = chooseDatabaseIndex("Please enter Database [ID]");
        if (dbIndex == -1) {
            return;
        }
        Database database = databases.get(dbIndex);

        // Scan folder recursively
        System.out.println("Scanning folder recursively...");
        FolderScanner folderScanner = new FolderScanner();
        Map<String, FileMetadata> foundFiles = folderScanner.scan(Paths.get(database.getPath()));
        System.out.println("Scanning of the folder is done.");

        // Deleting deleted files from the Database
        Iterator<FileMetadata> iterator = database.getFiles().values().iterator();
        while (iterator.hasNext()) {
            FileMetadata fileMetadata = iterator.next();
            // If the File from Database is not found during Folder Scan, deleting it from the Database
            if (!foundFiles.containsKey(fileMetadata.getRelativePath())) {
                iterator.remove();
            }
        }

        // Filtering New Files only
        iterator = foundFiles.values().iterator();
        while (iterator.hasNext()) {
            FileMetadata fileMetadata = iterator.next();
            // If the file already in the Database, removing it from the list
            if (database.getFiles().containsKey(fileMetadata.getRelativePath())) {
                // Additional checks that file could be changed by size or lastModifiedDate
                FileMetadata fileInDatabase = database.getFiles().get(fileMetadata.getRelativePath());
                if (fileMetadata.equalsByMetadata(fileInDatabase)) {
                    iterator.remove();
                }
            }
        }

        // Calculating checksums
        System.out.println("Calculating Checksums of the files...");
        long databaseLastSaveTime = System.currentTimeMillis();
        List<Exception> exceptions = new ArrayList<>();
        long totalProcessedFileSize = 0;
        long totalFileSize = foundFiles.values().stream().mapToLong(f -> f.getSize()).sum();
        ProgressPrinter progressPrinter = new ProgressPrinter();
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-512");
        int fileCounter = 1, filesTotal = foundFiles.size();
        iterator = foundFiles.values().iterator();
        while (iterator.hasNext()) {
            FileMetadata fileMetadata = iterator.next();
            System.out.println(fileCounter + " of " + filesTotal + ", "
                    + FormattingUtils.humanReadableSize(totalProcessedFileSize)
                    + " of " + FormattingUtils.humanReadableSize(totalFileSize) + ": "
                    + fileMetadata.getRelativePath());
            messageDigest.reset();
            progressPrinter.reset(fileMetadata.getSize());
            try (FileInputStream inputStream = new FileInputStream(fileMetadata.getAbsolutePath().toString())) {
                int read;
                int copyChunkSize = 1024 * 1024;
                byte[] buffer = new byte[copyChunkSize];
                while ((read = inputStream.read(buffer)) > 0) {
                    messageDigest.update(buffer, 0, read);
                    progressPrinter.printProgressIncrease(read, false);
                    totalProcessedFileSize += read;
                }
                progressPrinter.printProgressFinished();
                System.out.println();
                String checksum = new BigInteger(1, messageDigest.digest()).toString(16);
                fileMetadata.setSha512(checksum);
                database.addFileMetadata(fileMetadata);

                long timeNow = System.currentTimeMillis();
                if (timeNow - databaseLastSaveTime >= DATABASE_SAVE_INTERVAL_MILLIS) {
                    saveDatabase(database);
                    databaseLastSaveTime = System.currentTimeMillis();
                }
            } catch (IOException e) {
                exceptions.add(e);
                e.printStackTrace();
            }

            fileCounter++;
        }
        System.out.println();
        System.out.println("Calculating Checksums is done.");

        // Dump exceptions during the operation
        System.out.println("Following Exceptions occured during the operation:");
        for (Exception e : exceptions) {
            e.printStackTrace();
            System.out.println();
        }

        // Final save of the database
        saveDatabase(database);
    }

    private int chooseDatabaseIndex(String message) {
        while (true) {
            System.out.println(message + " or [Enter] to cancel operation: ");
            for (int i = 0; i < databases.size(); i++) {
                Database database = databases.get(i);
                System.out.println("[" + i + "] - " + database.getName() + " - " + database.getPath());
            }
            String input = scanner.next();
            input = input.trim();
            if ("".equalsIgnoreCase(input)) {
                System.out.println("Operation cancelled");
                return -1;
            }

            try {
                int dbIndex = Integer.parseInt(input);
                if (dbIndex >= 0 && dbIndex < databases.size()) {
                    return dbIndex;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void findDuplicationsInDatabase() throws IOException {
        System.out.println("Looking for file duplications in a folder.");

        // Choose Folder to update
        int dbIndex = chooseDatabaseIndex("Please enter Database [ID]");
        if (dbIndex == -1) {
            return;
        }
        Database database = databases.get(dbIndex);

        // Adding all Metadata to a Map
        Map<String, List<FileMetadata>> fileMetadataMap = new HashMap<>();
        putDatabaseFilesToFileMetadataMap(database, fileMetadataMap);

        // Looking for Metadata with the same Checksums
        dumpChecksumMapAllDuplications(fileMetadataMap);
    }

    private void compareDatabases() throws IOException {
        System.out.println("This method will search all occurances files from Database 2 in Database 1 by comparing Checksums");

        // Choose Databases to compare
        int db1Index = chooseDatabaseIndex("Please enter Database 1 [ID]");
        if (db1Index == -1) {
            return;
        }
        int db2Index = chooseDatabaseIndex("Please enter Database 2 [ID]");
        if (db2Index == -1) {
            return;
        }

        Database database1 = databases.get(db1Index);
        Database database2 = databases.get(db2Index);

        // Creating Map like Checksum -> ArrayList<FileMetadata>
        Map<String, List<FileMetadata>> fileMetadataMap = new HashMap<>();
        putDatabaseFilesToFileMetadataMap(database1, fileMetadataMap);
        putDatabaseFilesToFileMetadataMap(database2, fileMetadataMap);

        // Loop over common FileMetadataMap
        dumpChecksumMapAllDuplications(fileMetadataMap);
    }

    private void dumpChecksumMapAllDuplications(Map<String, List<FileMetadata>> fileMetadataMap) throws IOException {
        // Log filename for output
        String logFilename = logFilenameDateFormatter.format(new Date()) + ".log";
        File logFile = new File(LOGS_FOLDER, logFilename);

        try (PrintWriter pw = new PrintWriter(logFile)) {
            for (Entry<String, List<FileMetadata>> entry : fileMetadataMap.entrySet()) {
                if (entry.getValue().size() > 1) {
                    // Display file details
                    System.out.println(entry.getKey());
                    pw.println(entry.getKey());
                    for (FileMetadata fileMetadata : entry.getValue()) {
                        String text = fileMetadata.getDatabase().getName() + ": " + fileMetadata.getAbsolutePath();
                        System.out.println(text);
                        pw.println(text);
                    }
                    System.out.println();
                    pw.println();
                }
            }
        }
    }

    private void putDatabaseFilesToFileMetadataMap(Database database, Map<String, List<FileMetadata>> fileMetadataMap) {
        for (FileMetadata fileMetadata : database.getFiles().values()) {
            String checksum = fileMetadata.getSha512();
            List<FileMetadata> chain = fileMetadataMap.get(checksum);
            if (chain == null) {
                chain = new ArrayList<>();
                fileMetadataMap.put(checksum, chain);
            }
            chain.add(fileMetadata);
        }
    }

    private void saveDatabase(Database database) throws IOException {
        System.out.println("Saving Database " + database.getName() + "...");

        database.setLastUpdated();
        File databaseFile = new File(DATABASE_FOLDER, database.getName() + ".json");

        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(mapper.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(Visibility.ANY)
                .withGetterVisibility(Visibility.NONE)
                .withSetterVisibility(Visibility.NONE)
                .withCreatorVisibility(Visibility.NONE));
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.setDateFormat(new StdDateFormat().withColonInTimeZone(true));
        mapper.setTimeZone(TimeZone.getDefault());
        mapper.writerWithDefaultPrettyPrinter().writeValue(databaseFile, database);

        System.out.println("Database " + database.getName() + " has been successfully saved");
    }
}
