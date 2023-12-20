package app;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.type.TypeReference;

import app.core.dto.Configuration;
import app.core.dto.FileDatabase;
import app.core.dto.FolderSnapshot;
import app.core.dto.RelativeFileMetadata;
import app.core.file.ChecksumComputer;
import digest.XorProvider;
import files.PathIterator;
import utils.JSONUtils;
import utils.PathUtils;

public class SnapshotMakerApp {
    private static final String CONFIGURATION_FILENAME = "configuration.json";
    private static final String FILE_DATABASE_FILENAME = "file-database.json";

    // TODO Replace with parameters from Configuration
    private static final String[] CHECKSUM_ALGORITHMS = { "XOR", "MD5", "SHA-1", "SHA-256", "SHA-512" };

    private static final Logger LOGGER = LogManager.getLogger(SnapshotMakerApp.class);

    private static Configuration configuration;
    private static FileDatabase fileDatabase;

    public static void main(String[] args) {
        LOGGER.info("Snapshot Maker started");

        Security.addProvider(new XorProvider());

        try {
            LOGGER.info("Loading Configuration...");
            loadConfiguration();

            LOGGER.info("Loading File Database...");
            loadFileDatabase();

            LOGGER.info("Checking input parameters...");
            String folderName = args.length > 0 ? args[0] : ".";
            Path folder = PathUtils.checkAndGetFolder(folderName).normalize();

            LOGGER.info("Starting to make Snapshot...");
            makeSnapshot(folder);

            LOGGER.info("Application finished successfully");
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            System.exit(-1);
        }
    }

    private static void loadConfiguration() throws IOException {
        File configFile = new File(CONFIGURATION_FILENAME);
        if (configFile.exists()) {
            TypeReference<Configuration> typeReference = new TypeReference<Configuration>() {};
            configuration = JSONUtils.loadFromDisk(configFile, typeReference);
        } else {
            throw new FileNotFoundException("Configuration file not found: " + configFile.getAbsolutePath());
        }
    }

    private static void loadFileDatabase() throws IOException {
        File fileDatabaseFile = new File(FILE_DATABASE_FILENAME);
        if (fileDatabaseFile.exists()) {
            TypeReference<FileDatabase> typeReference = new TypeReference<FileDatabase>() {};
            fileDatabase = JSONUtils.loadFromDisk(fileDatabaseFile, typeReference);
        } else {
            fileDatabase = new FileDatabase();
        }
    }

    private static void makeSnapshot(Path folder) throws IOException, NoSuchAlgorithmException {
        FolderSnapshot snapshot = loadOrCreateSnapshot(folder);

        // Scan Files
        LOGGER.info("Scanning files for the snapshot...");
        PathIterator iterator = new PathIterator(folder, true);
        while (iterator.hasNext()) {
            Path file = iterator.next();
            LOGGER.debug("Found file: " + file.toString());

            // Do not adding the Snapshot File and Log File to the Snapshot
            if (file.toString().endsWith("-snapshot.json") || file.toString().endsWith("snapshot-maker.log")) {
                LOGGER.debug("Skipping file \"" + file.toString() + "\" because it is a file of this application");
                continue;
            }

            // If File not in the Snapshot yet or the File is outdated, adding it into the Snapshot
            RelativeFileMetadata newFileMetadata = new RelativeFileMetadata(file, folder);
            RelativeFileMetadata existingFileMetadata = snapshot.getFileMetadata(newFileMetadata.getRelativePath());
            if (existingFileMetadata == null) {
                LOGGER.debug("Adding as a new file to the Snapshot");
                snapshot.addFileMetadata(newFileMetadata);
            } else if(!existingFileMetadata.equalsByMetadata(newFileMetadata)) {
                LOGGER.debug("File in the Snapshot is outdated, updating with a new FileMetadata");
                snapshot.addFileMetadata(newFileMetadata);
            } else {
                LOGGER.debug("File is already in the Snapshot and looks up-to-date");
            }

            LOGGER.debug("Saving snapshot if needed...");
            snapshot.saveIfNeeded();
        }
        LOGGER.info("File scanning is done");
        LOGGER.debug("Saving snapshot...");
        snapshot.save();

        // Filtering Files to Compute Checksums
        LOGGER.info("Computing Checksums for the files for the snapshot...");
        List<RelativeFileMetadata> filesToComputeChecksums = new ArrayList<>();
        long totalFileSizeToComputeChecksums = 0;
        for (RelativeFileMetadata fileMetadata : snapshot.getFilesMap().values()) {
            if (!fileMetadata.hasChecksums(CHECKSUM_ALGORITHMS)) {
                filesToComputeChecksums.add(fileMetadata);
                totalFileSizeToComputeChecksums += fileMetadata.getSize();
            }
        }
        ChecksumComputer checksumComputer = new ChecksumComputer(CHECKSUM_ALGORITHMS);
        checksumComputer.setAllFilesTotalSize(totalFileSizeToComputeChecksums);
        checksumComputer.reset();
        for (RelativeFileMetadata fileMetadata : filesToComputeChecksums) {
            checksumComputer.computeChecksums(fileMetadata);
            snapshot.saveIfNeeded();
        }
        LOGGER.debug("Saving snapshot...");
        snapshot.save();
        LOGGER.info("Checksum computing is done, errors: " + checksumComputer.getNumberOfErrors());
    }

    private static FolderSnapshot loadOrCreateSnapshot(Path folder) throws IOException {
        String snapshotName = folder.getFileName().toString();
        String snapshotFilename = folder.toString() + File.separator + snapshotName + "-snapshot.json";
        File snapshotFile = new File(snapshotFilename);
        FolderSnapshot snapshot;
        if (snapshotFile.exists()) {
            LOGGER.info("Loading existing snapshot \"" + snapshotName + "\" from " + snapshotFilename);
            TypeReference<FolderSnapshot> typeReference = new TypeReference<FolderSnapshot>() {};
            snapshot = JSONUtils.loadFromDisk(snapshotFile, typeReference);
            LOGGER.info("Snapshot has been loaded successfully");
        } else {
            LOGGER.info("Creating new snapshot: " + snapshotName);
            snapshot = new FolderSnapshot();
            snapshot.setName(snapshotName);
        }
        snapshot.setSaveFile(snapshotFile);

        return snapshot;
    }
}
