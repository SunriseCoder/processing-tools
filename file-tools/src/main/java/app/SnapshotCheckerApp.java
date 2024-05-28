package app;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import app.core.SnapshotChecker;
import app.core.dto.FileDatabase;
import app.utils.PathUtils;

// TODO Refactoring:
// 1. Merge all Syncer modules into one module
// 2. Merge FileMetadatas into one class and Configuration Files into one class, etc
// 3. Move all loadSnapshot, loadConfig, etc to their classes as static methods
// 4. Rewrite ChecksumComputer using ProgressPrinter which will be set from outside
// 5. SnapshotMaker - exclude log- and other files from scan - adjust ignore list
public class SnapshotCheckerApp {
    private static final Logger LOGGER = LogManager.getLogger(SnapshotCheckerApp.class);

    private static Path destinationFolder;

    private static FileDatabase fileDatabase;
    private static SnapshotChecker snapshotChecker;

    public static void main(String[] args) {
        LOGGER.info("Snapshot Checker started");

        try {
            processInputArguments(args);
            fileDatabase = FileDatabase.load();
            checkSnapshot();
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

        snapshotChecker = new SnapshotChecker();
        checkSnapshotFile(args[0]);
        checkDestinationFolder(args[1]);
    }

    private static void printUsage() {
        System.out.println("\nUsage:");
        System.out.println(SnapshotCheckerApp.class.getName() + " <snapshot-file> <destination-folder>");
        System.out.println("  <snapshot-file>      - path to snapshot file made by SnapshotMaker application");
        System.out.println("  <destination-folder> - path to a folder where the source folder will be replicated");
    }

    private static void checkSnapshotFile(String snapshotFilename) {
        File snapshotFile = new File(snapshotFilename);
        if (!snapshotFile.exists()) {
            LOGGER.error("Snapshot file does not exist: " + snapshotFilename);
            System.exit(-1);
        }
        snapshotChecker.setSnapshotFile(snapshotFile);
    }

    private static void checkDestinationFolder(String destinationFolderPathString) throws IOException {
        Path destinationFolderPath = PathUtils.getAbsolutePathWithDriveLetterUpperCase(destinationFolderPathString);
        destinationFolder = PathUtils.getOrCreateFolder(destinationFolderPath);
        snapshotChecker.setDestinationFolder(destinationFolder);
    }

    private static void checkSnapshot() throws IOException {
        snapshotChecker.setFileDatabase(fileDatabase);

        snapshotChecker.loadSnapshot();
        snapshotChecker.checkSnapshot();
        snapshotChecker.reportResult();
    }
}
