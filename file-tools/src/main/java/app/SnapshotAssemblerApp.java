package app;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import app.core.FileDatabaseUpdater;
import app.core.SnapshotAssembler;
import app.core.dto.AssemblerFileSource;
import app.core.dto.Configuration;
import app.core.dto.FileDatabase;
import app.utils.PathUtils;

public class SnapshotAssemblerApp {
    private static final Logger LOGGER = LogManager.getLogger(SnapshotAssemblerApp.class);

    private static Configuration configuration;
    private static Path destinationFolder;

    private static FileDatabase fileDatabase;
    private static SnapshotAssembler snapshotApplier;

    public static void main(String[] args) {
        LOGGER.info("Snapshot Assembler started");

        try {
            processInputArguments(args);
            configuration = Configuration.load();
            fileDatabase = FileDatabase.load(configuration);
            updateFileDatabase();
            assembleSnapshot(args);
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

        snapshotApplier = new SnapshotAssembler();
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

    private static void updateFileDatabase() throws IOException {
        LOGGER.info("Updating File Database...");
        FileDatabaseUpdater fileDatabaseUpdater = new FileDatabaseUpdater();
        fileDatabaseUpdater.setFileDatabase(fileDatabase);

        List<AssemblerFileSource> fileSources = configuration.getAssemblerFileSources();
        // Adding Destination Folder in order to prevent deleting already correct and existing files from the FileDatabase
        fileSources.add(new AssemblerFileSource(destinationFolder.toString(), false));
        fileDatabaseUpdater.setFileSources(fileSources);

        fileDatabaseUpdater.updateFileDatabase();
    }

    private static void assembleSnapshot(String[] args) throws NoSuchAlgorithmException, IOException {
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
