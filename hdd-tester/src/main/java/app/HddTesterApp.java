package app;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.type.TypeReference;

import app.config.Configuration;
import app.config.Configuration.Keys;
import app.core.database.Database;
import app.core.dto.fs.FileSystemFile;
import app.core.file.FileChecker;
import app.core.file.FileCreator;
import utils.ConsoleUtils;
import utils.ConsoleUtils.Option;
import utils.FileUtils;
import utils.JSONUtils;

public class HddTesterApp {
    private static final Logger LOGGER = LogManager.getLogger(HddTesterApp.class);
    private static final Random RND = new Random();

    public static void main(String[] args) throws Exception {
        LOGGER.info("Application started");

        // Checking Root Folder
        LOGGER.info("Checking Test Folder...");
        File physicalRootFolder = new File(Configuration.getValue(Keys.HddPath));
        if (!physicalRootFolder.exists()) {
            physicalRootFolder.mkdirs();
        } else if (!physicalRootFolder.isDirectory()) {
            System.out.println("Root folder \"" + physicalRootFolder.getAbsolutePath() + "\" is not a directory, exiting...");
            System.exit(-1);
        }

        // Checking Tmp Folder
        LOGGER.info("Checking and cleaning up Temporary Folder...");
        File physicalTmpFolder = new File(Configuration.getValue(Keys.HddTmpPath));
        if (!physicalTmpFolder.exists()) {
            physicalTmpFolder.mkdirs();
        } else if (!physicalTmpFolder.isDirectory()) {
            System.out.println("Temporary folder \"" + physicalTmpFolder.getAbsolutePath() + "\" is not a directory, exiting...");
            System.exit(-1);
        }
        FileUtils.cleanupFolder(physicalTmpFolder);

        // Loading Database
        LOGGER.info("Checking database...");
        Database database = new Database();
        File databaseFile = new File("database.json");
        if (databaseFile.exists() && databaseFile.isFile()) {
            LOGGER.info("Found existing database...");
            String message = "File Database has been found from the last run, what would you like to do?";
            Option optionContinue = new Option("Continue previous Test Run");
            Option optionNew = new Option("Start a new Test Run");
            Option optionExit = new Option("Exit");
            Option input = ConsoleUtils.chooseOneOption(message, optionContinue, optionNew, optionExit);

            if (input == optionContinue) {
                LOGGER.info("Trying to load existing database...");
                // TODO Implement
                database = JSONUtils.loadFromDisk(databaseFile, new TypeReference<Database>() {});
                database.linkFiles();
                database.checkRealFilesOnHdd(physicalRootFolder);
                LOGGER.info("Database has been loaded successfully...");
            } else if (input == optionNew) {
                // Nothing to do here, new Database is already created
                LOGGER.info("Creating new Database...");
            } else if (input == optionExit) {
                LOGGER.info("Exit choosen by the User, exiting...");
                System.exit(0);
            } else {
                LOGGER.info("Unsupported User choice: " + input.text);
                System.exit(-1);
            }
        } else {
            LOGGER.info("Existing Database has not been found, creating a new one...");
        }

        // Create Files
        LOGGER.info("Creating files");
        long minFreeSpace = Long.parseLong(Configuration.getValue(Keys.MinFreeSpace));
        FileCreator creator = new FileCreator(physicalRootFolder, physicalTmpFolder);
        while (physicalRootFolder.getFreeSpace() > minFreeSpace) {
            long fileSize = generateRandomFileSize(physicalRootFolder);
            FileSystemFile file = database.createNewFile(fileSize);
            try {
                creator.fillFile(file);
            } catch (IOException e) {
                LOGGER.error(e);
            }
            database.saveIfNeeded();
        }
        LOGGER.info("File Creation is done");

        // Verifying Files
        LOGGER.info("Verifying files");
        FileChecker checker = new FileChecker();
        Map<String, FileSystemFile> files = database.getFiles();
        long allFilesTotalSize = files.values().stream()
                .filter(f -> !f.isChecked())
                .mapToLong(FileSystemFile::getSize).sum();
        checker.setAllFilesTotalSize(allFilesTotalSize);
        checker.setAllFilesCheckedSize(0);
        for (Entry<String, FileSystemFile> entry : files.entrySet()) {
            File file = new File(physicalRootFolder + entry.getKey());
            FileSystemFile fileMetadata = entry.getValue();
            if (fileMetadata.isChecked()) {
                LOGGER.info("File \"" + file.getAbsolutePath() + "\" has been checked already, skipping...");
                continue;
            }
            checker.checkFile(file, fileMetadata);
            database.saveIfNeeded();
        }
        LOGGER.info("File Verification is done");

        // Final results
        long numberOfErrors = checker.getNumberOfErrors();
        LOGGER.info("Done, errors: " + numberOfErrors);
    }

    private static long generateRandomFileSize(File rootFolder) {
        long minSize = Long.parseLong(Configuration.getValue(Keys.MinFileSize));
        long maxSize = Long.parseLong(Configuration.getValue(Keys.MaxFileSize));

        double minSizeLog = Math.log(minSize) / Math.log(10);
        double maxSizeLog = Math.log(maxSize) / Math.log(10);
        double logDelta = maxSizeLog - minSizeLog;

        double valueLog = RND.nextDouble() * logDelta + minSizeLog;
        long value = (long) Math.pow(10, valueLog);

        long minFreeSpace = Long.parseLong(Configuration.getValue(Keys.MinFreeSpace));
        long maxFileSize = rootFolder.getFreeSpace() - minFreeSpace;
        if (value > maxFileSize) {
            value = maxFileSize;
        }

        return value;
    }
}
