package app;

import java.io.File;
import java.security.Security;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.type.TypeReference;

import app.config.Configuration;
import app.config.Configuration.Keys;
import app.core.database.Database;
import app.core.dto.fs.FileSystemFile;
import app.core.file.FileChecker;
import app.digest.XorProvider;
import app.utils.FileUtils;
import app.utils.JSONUtils;

public class CheckFilesApp {
    private static final Logger LOGGER = LogManager.getLogger(CheckFilesApp.class);

    public static void main(String[] args) throws Exception {
        LOGGER.info("Application started");

        Security.addProvider(new XorProvider());

        // Checking Root Folder
        LOGGER.info("Checking Test Folder...");
        File physicalRootFolder = new File(Configuration.getValue(Keys.HddPath));
        if (!physicalRootFolder.exists()) {
            LOGGER.error("Test Folder " + physicalRootFolder.getAbsolutePath() + " does not exist");
        } else if (!physicalRootFolder.isDirectory()) {
            LOGGER.error("Root folder \"" + physicalRootFolder.getAbsolutePath() + "\" is not a directory, exiting...");
            System.exit(-1);
        }

        // Checking Tmp Folder
        LOGGER.info("Checking Temporary Folder...");
        File physicalTmpFolder = new File(Configuration.getValue(Keys.HddTmpPath));
        if (physicalTmpFolder.exists() && physicalTmpFolder.isDirectory()) {
            LOGGER.info("Cleaning up Temporary Folder...");
            FileUtils.cleanupFolder(physicalTmpFolder);
        }

        // Loading Database
        LOGGER.info("Checking database...");
        Database database = new Database();
        File databaseFile = new File("database.json");
        if (databaseFile.exists() && databaseFile.isFile()) {
            LOGGER.info("Trying to load existing database...");
            database = JSONUtils.loadFromDisk(databaseFile, new TypeReference<Database>() {});
            database.linkFiles();
            database.checkExistingOfRealFilesOnHdd(physicalRootFolder);
            LOGGER.info("Database has been loaded successfully...");
        } else {
            LOGGER.error("Database has not been found: " + databaseFile.getAbsolutePath());
            System.exit(-1);
        }

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
                checker.addSkippedSize(fileMetadata.getSize());
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
}
