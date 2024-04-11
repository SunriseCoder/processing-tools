package app;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.type.TypeReference;

import app.core.dto.Snapshot;
import app.core.dto.SnapshotFile;
import app.utils.FormattingUtils;
import app.utils.JSONUtils;

public class SnapshotDumpApp {
    private static final Logger LOGGER = LogManager.getLogger(SnapshotDumpApp.class);

    private static Snapshot snapshot;

    public static void main(String[] args) throws IOException {
        processInputArguments(args);
        dumpSnapshot();
    }

    private static void processInputArguments(String[] args) throws IOException {
        LOGGER.info("Checking Input Arguments...");
        if (args.length < 1) {
            LOGGER.error("Invalid input parameters: " + Arrays.toString(args));
            printUsage();
            System.exit(-1);
        }

        File snapshotFile = new File(args[0]);
        LOGGER.info("Loading existing snapshot from file: " + snapshotFile.getAbsolutePath());
        TypeReference<Snapshot> typeReference = new TypeReference<Snapshot>() {};
        snapshot = JSONUtils.loadFromDisk(snapshotFile, typeReference);
        LOGGER.info("Snapshot has been loaded successfully");
    }

    private static void printUsage() {
        System.out.println("\nUsage:");
        System.out.println(SnapshotDumpApp.class.getName() + " <snapshot-file>");
        System.out.println("  <snapshot-file>   - path to snapshot file made by SnapshotMaker application");
    }

    private static void dumpSnapshot() {
        StringBuilder sb = new StringBuilder();
        sb.append("Snapshot dump:\n");
        sb.append("\tName: ").append(snapshot.getName()).append("\n");
        sb.append("\tLast updated: ").append(snapshot.getLastUpdated()).append("\n");

        List<SnapshotFile> files = new ArrayList<>(snapshot.getFilesMap().values());
        files.sort((a, b) -> a.getRelativePath().compareTo(b.getRelativePath()));
        files.stream().forEach(m -> {
            sb.append(m.getRelativePath()).append(" (")
                    .append(FormattingUtils.humanReadableSizeBi(m.getSize()))
                    .append("b)\n");
        });

        long allFilesSize = snapshot.getFilesMap().values().stream()
                .mapToLong(m -> m.getSize())
                .sum();
        sb.append("\tTotal File Size: " + allFilesSize
                + " (" + FormattingUtils.humanReadableSizeBi(allFilesSize) + "b)");

        // TODO Show info when File Status is NOT Ok - amount, basic and details

        LOGGER.info(sb.toString());
    }
}
