package app;

import java.io.IOException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import app.core.SnapshotMaker;
import app.core.dto.Configuration;
import app.core.dto.FolderSnapshot;
import app.core.dto.RelativeFileMetadata;
import app.digest.XorProvider;
import app.utils.FormattingUtils;
import app.utils.PathUtils;

public class SnapshotMakerApp {
    private static final Logger LOGGER = LogManager.getLogger(SnapshotMakerApp.class);

    private static Configuration configuration;

    public static void main(String[] args) {
        LOGGER.info("Snapshot Maker started");

        Security.addProvider(new XorProvider());

        try {
            configuration = Configuration.load();
            Path snapshotFolder = getSnapshotFolder(args);
            FolderSnapshot snapshot = makeSnapshot(snapshotFolder);
            printSnapshotStatistics(snapshot);

            LOGGER.info("Application finished successfully");
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            System.exit(-1);
        }
    }

    private static Path getSnapshotFolder(String[] args) throws IOException {
        LOGGER.info("Checking input parameters...");
        String folderName = args.length > 0 ? args[0] : ".";
        Path snapshotFolder = PathUtils.checkAndGetFolder(folderName).normalize();
        return snapshotFolder;
    }

    private static FolderSnapshot makeSnapshot(Path snapshotFolder) throws IOException, NoSuchAlgorithmException {
        LOGGER.info("Starting to make Snapshot for folder: " + snapshotFolder.toString());
        SnapshotMaker snapshotMaker = new SnapshotMaker(configuration);
        FolderSnapshot snapshot = snapshotMaker.makeSnapshot(snapshotFolder);
        snapshot.saveIfNeededComplete();
        return snapshot;
    }

    private static void printSnapshotStatistics(FolderSnapshot snapshot) {
        StringBuilder message = new StringBuilder();
        message.append("Snapshot " + snapshot.getName() + " Statistics:\n");

        String filesStatistics = snapshot.getFilesMap().values().stream()
                .map(m -> m.getRelativePath()
                        + " (" + FormattingUtils.humanReadableSize(m.getSize()) + "b)")
                .collect(Collectors.joining("\n"));
        message.append(filesStatistics);

        String filesWithErrorsStatistics = snapshot.getFilesMap().values().stream()
                .filter(m -> m.getStatus() != RelativeFileMetadata.Status.Ok)
                .map(m -> m.getRelativePath() + " (" + FormattingUtils.humanReadableSize(m.getSize()) + "b) "
                        + String.join(", ", m.getStatusMessages()))
                .collect(Collectors.joining("\n"));
        if (!filesWithErrorsStatistics.isEmpty()) {
            message.append("\n\t!!! Errors !!!\n").append(filesWithErrorsStatistics);
        }

        long allFilesSize = snapshot.getFilesMap().values().stream()
                .mapToLong(m -> m.getSize())
                .sum();
        message.append("\nTotal File Size: " + allFilesSize
                + " (" + FormattingUtils.humanReadableSize(allFilesSize) + "b)");
        LOGGER.info(message.toString());
    }
}
