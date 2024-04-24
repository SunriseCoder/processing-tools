package app;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.type.TypeReference;

import app.checksum.ChecksumComputer;
import app.core.FileDatabaseUpdater;
import app.core.SnapshotApplier;
import app.core.dto.Configuration;
import app.core.dto.FileDatabase;
import app.core.dto.FileFolder;
import app.digest.XorProvider;
import app.utils.JSONUtils;
import app.utils.PathUtils;

public class SnapshotAssemblerApp {
    private static final String FILE_DATABASE_FILENAME = "file-database.json";

    private static final Logger LOGGER = LogManager.getLogger(SnapshotAssemblerApp.class);

    private static Configuration configuration;
    private static Path destinationFolder;

    private static FileDatabase fileDatabase;
    private static SnapshotApplier snapshotApplier;

    public static void main(String[] args) {
        LOGGER.info("Snapshot Assembler started");

        Security.addProvider(new XorProvider());

        try {
            processInputArguments(args);
            configuration = Configuration.load();
            loadFileDatabase();
            updateFileDatabase();
            applySnapshot(args);
            fileDatabase.saveIfNeededComplete();

            LOGGER.info("Application finished successfully");
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            try {
                LOGGER.info("Trying to save FileDatabase...");
                fileDatabase.saveIfNeededComplete();
                LOGGER.info("FileDatabase has been saved Successfully");
            } catch (IOException e1) {
                LOGGER.error("Failed to save FileDatabase", e1);
            }
            System.exit(-1);
        }
    }

    private static void processInputArguments(String[] args) throws IOException {
        LOGGER.info("Checking Input Arguments...");
        if (args.length < 2) {
            LOGGER.error("Invalid input parameters: " + Arrays.toString(args));
            printUsage();
            System.exit(-1);
        }

        snapshotApplier = new SnapshotApplier();
        checkSnapshotFile(args[0]);
        checkDestinationFolder(args[1]);
    }

    private static void printUsage() {
        System.out.println("\nUsage:");
        System.out.println(SnapshotAssemblerApp.class.getName() + " <snapshot-file> <destination-folder>");
        System.out.println("  <snapshot-file>      - path to snapshot file made by SnapshotMaker application");
        System.out.println("  <destination-folder> - path to a folder where the source folder will be replicated");
    }

    private static void checkSnapshotFile(String snapshotFilename) {
        File snapshotFile = new File(snapshotFilename);
        if (!snapshotFile.exists()) {
            LOGGER.error("Snapshot file does not exist: " + snapshotFilename);
            System.exit(-1);
        }
        snapshotApplier.setSnapshotFile(snapshotFile);
    }

    private static void checkDestinationFolder(String destinationFolderPathString) throws IOException {
        Path destinationFolderPath = PathUtils.getAbsolutePathWithDriveLetterUpperCase(destinationFolderPathString);
        destinationFolder = PathUtils.getOrCreateFolder(destinationFolderPath);
        snapshotApplier.setDestinationFolder(destinationFolder);
    }

    // TODO Move this method to FileDatabase class as static method
    private static void loadFileDatabase() throws IOException, NoSuchAlgorithmException {
        LOGGER.info("Loading File Database...");
        String applicationHomePath = System.getProperty("app.home");
        File fileDatabaseFile = new File(applicationHomePath, FILE_DATABASE_FILENAME);
        if (fileDatabaseFile.exists()) {
            TypeReference<FileDatabase> typeReference = new TypeReference<FileDatabase>() {};
            fileDatabase = JSONUtils.loadFromDisk(fileDatabaseFile, typeReference);
        } else {
            fileDatabase = new FileDatabase();
        }
        fileDatabase.setSaveFileComplete(fileDatabaseFile);
        fileDatabase.setMinimalSaveIntervalInMS(configuration.getFileDatabaseMinimalSaveIntervalInMS());
        fileDatabase.setChecksumComputer(new ChecksumComputer(configuration.getChecksumAlgorithms()));
        fileDatabase.setChecksumAlgorithms(configuration.getChecksumAlgorithms());
        fileDatabase.linkSubEntities();
    }

    private static void updateFileDatabase() throws IOException {
        LOGGER.info("Updating File Database...");
        FileDatabaseUpdater fileDatabaseUpdater = new FileDatabaseUpdater();
        fileDatabaseUpdater.setFileDatabase(fileDatabase);

        List<FileFolder> fileSources = configuration.getFileSources();
        // Adding Destination Folder in order to prevent deleting already correct and existing files from the FileDatabase
        fileSources.add(new FileFolder(destinationFolder.toString(), false));
        fileDatabaseUpdater.setFileSources(fileSources);

        fileDatabaseUpdater.updateFileDatabase();
    }

    private static void applySnapshot(String[] args) throws NoSuchAlgorithmException, IOException {
        snapshotApplier.setConfiguration(configuration);
        snapshotApplier.loadSnapshot();

        snapshotApplier.setFileDatabase(fileDatabase);

        snapshotApplier.findFilesBySize();
        snapshotApplier.reportNotFoundFiles();

        snapshotApplier.computeChecksumsForCandidates();
        snapshotApplier.findFilesByChecksums();
        snapshotApplier.reportNotFoundFiles();

        snapshotApplier.checkFileOperations();

        if (!snapshotApplier.getUserConfirmationForProceedFileOperations()) {
            return;
        }

        snapshotApplier.performFileOperations();
        snapshotApplier.reportNotFoundFiles();

        snapshotApplier.validateSnapshot();
        snapshotApplier.cleanupDestinationFolder();
        snapshotApplier.reportSnapshotValidation();
    }
}
