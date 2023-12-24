package app;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
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

    private static final Logger LOGGER = LogManager.getLogger(SnapshotMakerApp.class);

    private static Configuration configuration;
    private static FileDatabase fileDatabase;

    public static void main(String[] args) {
        LOGGER.info("Snapshot Maker started");

        Security.addProvider(new XorProvider());

        try {
            LOGGER.info("Loading Configuration...");
            loadConfiguration();

            //TODO Uncomment this to start real using of File Database
            //LOGGER.info("Loading File Database...");
            //loadFileDatabase();

            LOGGER.info("Checking input parameters...");
            String folderName = args.length > 0 ? args[0] : ".";
            Path snapshotFolder = PathUtils.checkAndGetFolder(folderName).normalize();

            LOGGER.info("Starting to make Snapshot for folder: " + snapshotFolder.toString());
            makeSnapshot(snapshotFolder);

            LOGGER.info("Application finished successfully");
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            System.exit(-1);
        }
    }

    private static void loadConfiguration() throws IOException {
        String applicationHomePath = System.getProperty("app.home");
        File configFile = new File(applicationHomePath, CONFIGURATION_FILENAME);
        if (configFile.exists()) {
            TypeReference<Configuration> typeReference = new TypeReference<Configuration>() {};
            configuration = JSONUtils.loadFromDisk(configFile, typeReference);
        } else {
            throw new FileNotFoundException("Configuration file not found: " + configFile.getAbsolutePath());
        }
    }

    private static void loadFileDatabase() throws IOException {
        String applicationHomePath = System.getProperty("app.home");
        File fileDatabaseFile = new File(applicationHomePath, FILE_DATABASE_FILENAME);
        if (fileDatabaseFile.exists()) {
            TypeReference<FileDatabase> typeReference = new TypeReference<FileDatabase>() {};
            fileDatabase = JSONUtils.loadFromDisk(fileDatabaseFile, typeReference);
        } else {
            fileDatabase = new FileDatabase();
        }
    }

    private static void makeSnapshot(Path snapshotFolder) throws IOException, NoSuchAlgorithmException {
        FolderSnapshot snapshot = loadOrCreateSnapshot(snapshotFolder);

        // Marking existing files in the Snapshot as they potentially does not exist on disk anymore
        // Later all existing files will be whitelisted during Scan phase
        for (RelativeFileMetadata fileMetadata : snapshot.getFilesMap().values()) {
            fileMetadata.setExistsOnDiskNow(false);
        }

        // Scan Files
        LOGGER.info("Scanning files for the snapshot...");
        PathIterator iterator = new PathIterator(snapshotFolder, true);
        while (iterator.hasNext()) {
            Path currentFile = iterator.next();
            LOGGER.debug("Found file: " + currentFile.toString());

            // Do not adding the Snapshot File and Log File to the Snapshot
            if (currentFile.toString().endsWith("-snapshot.json")
                    || currentFile.toString().endsWith("snapshot-maker.log")
                    || currentFile.toString().matches(".*syncer\\-snapshot\\-maker\\-v[0-9]{3}\\.bat$")
                    ) {
                LOGGER.debug("Skipping file \"" + currentFile.toString() + "\" because it is a file of this application");
                continue;
            }

            // If File not in the Snapshot yet or the File is outdated, adding it into the Snapshot
            RelativeFileMetadata newFileMetadata = new RelativeFileMetadata(currentFile, snapshotFolder);
            newFileMetadata.setExistsOnDiskNow(true);
            RelativeFileMetadata existingFileMetadata = snapshot.getFileMetadata(newFileMetadata.getRelativePath());
            if (existingFileMetadata == null) {
                LOGGER.debug("Adding as a new file to the Snapshot");
                // Adding a new file to the Snapshot
                snapshot.addFileMetadata(newFileMetadata);
            } else if(!existingFileMetadata.equalsByMetadata(newFileMetadata)) {
                LOGGER.debug("File in the Snapshot is outdated, updating with a new FileMetadata");
                // Overwriting old file metadata with the new (actual) one
                snapshot.addFileMetadata(newFileMetadata);
            } else {
                LOGGER.debug("File is already in the Snapshot and looks up-to-date");
                // Marking the existing file that it still exists on the disk
                existingFileMetadata.setExistsOnDiskNow(true);
            }

            LOGGER.debug("Saving snapshot if needed...");
            snapshot.saveIfNeeded();
        }
        LOGGER.info("File scanning is done");

        snapshot.save();

        // Filtering Files to Compute Checksums
        LOGGER.info("Computing Checksums "
                + Arrays.toString(configuration.getChecksumAlgorithms())
                + " for the files for the snapshot...");
        List<RelativeFileMetadata> filesToComputeChecksums = new ArrayList<>();
        long totalFileSizeToComputeChecksums = 0;
        Iterator<RelativeFileMetadata> fileMetadataIterator = snapshot.getFilesMap().values().iterator();
        while (fileMetadataIterator.hasNext()) {
            RelativeFileMetadata fileMetadata = fileMetadataIterator.next();
            // If a file was not whitelisted during folder scan, removing it from the Snapshot
            if (!fileMetadata.isExistsOnDiskNow()) {
                iterator.remove();
            // If a file is on the disk, but checksums were not computed yet, adding it to the list to compute checksums
            } else if (!fileMetadata.hasChecksums(configuration.getChecksumAlgorithms())) {
                filesToComputeChecksums.add(fileMetadata);
                totalFileSizeToComputeChecksums += fileMetadata.getSize();
            }
        }

        // Computing Checksums
        ChecksumComputer checksumComputer = new ChecksumComputer(configuration.getChecksumAlgorithms());
        checksumComputer.setAllFilesTotalSize(totalFileSizeToComputeChecksums);
        checksumComputer.reset();
        for (RelativeFileMetadata fileMetadata : filesToComputeChecksums) {
            checksumComputer.computeChecksums(snapshotFolder, fileMetadata);
            snapshot.saveIfNeeded();
        }

        snapshot.save();

        LOGGER.info("Checksum computing is done, errors: " + checksumComputer.getNumberOfErrors());
    }

    private static FolderSnapshot loadOrCreateSnapshot(Path folder) throws IOException {
        String snapshotName = folder.getFileName().toString();
        String snapshotFilename = snapshotName + "-snapshot.json";
        File snapshotFile = new File(snapshotFilename);
        FolderSnapshot snapshot;
        if (snapshotFile.exists()) {
            LOGGER.info("Loading existing snapshot \"" + snapshotName + "\" from " + snapshotFilename);
            TypeReference<FolderSnapshot> typeReference = new TypeReference<FolderSnapshot>() {};
            snapshot = JSONUtils.loadFromDisk(snapshotFile, typeReference);
            snapshot.setConfiguration(configuration);
            LOGGER.info("Snapshot has been loaded successfully");
        } else {
            LOGGER.info("Creating new snapshot: " + snapshotName);
            snapshot = new FolderSnapshot();
            snapshot.setConfiguration(configuration);
            snapshot.setName(snapshotName);
        }
        snapshot.setSaveFile(snapshotFile);

        return snapshot;
    }
}
