package app.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.type.TypeReference;

import app.core.dto.AbsoluteFileMetadata;
import app.core.dto.FileDatabase;
import app.core.dto.FolderSnapshot;
import app.core.dto.RelativeFileMetadata;
import app.core.dto.RelativeFileMetadata.Status;
import app.files.PathIterator;
import app.structures.Pair;
import app.utils.FormattingUtils;
import app.utils.JSONUtils;

// TODO Think about extract some common code with SnapshotApplier
public class SnapshotChecker {
    private static final Logger LOGGER = LogManager.getLogger(SnapshotChecker.class);

    private File snapshotFile;
    private Path destinationFolder;

    private FolderSnapshot snapshot;
    private FileDatabase fileDatabase;

    private List<RelativeFileMetadata> errorFilesInSnapshot;
    private List<RelativeFileMetadata> notFoundFiles;
    private List<Pair<RelativeFileMetadata, AbsoluteFileMetadata>> fileSizeMismatchFiles;
    private List<Pair<RelativeFileMetadata, AbsoluteFileMetadata>> checksumMismatchFiles;
    private List<RelativeFileMetadata> correctFiles;
    private List<AbsoluteFileMetadata> filesNotInSnapshot;
    /** If a file on a disk, but in the Snapshot the Status is NOT Ok - we can't determine whether the file is Ok or Not */
    private List<Pair<RelativeFileMetadata, AbsoluteFileMetadata>> filesExistButInSnapshotIsNotOk;
    private List<Path> emptyFolders;

    public SnapshotChecker() {
        errorFilesInSnapshot = new ArrayList<>();
        notFoundFiles = new ArrayList<>();
        fileSizeMismatchFiles = new ArrayList<>();
        checksumMismatchFiles = new ArrayList<>();
        correctFiles = new ArrayList<>();
        filesNotInSnapshot = new ArrayList<>();
        filesExistButInSnapshotIsNotOk = new ArrayList<>();
        emptyFolders = new ArrayList<>();
    }

    public void setSnapshotFile(File snapshotFile) {
        this.snapshotFile = snapshotFile;
    }

    public void setDestinationFolder(Path destinationFolder) {
        this.destinationFolder = destinationFolder;
    }

    public void setFileDatabase(FileDatabase fileDatabase) {
        this.fileDatabase = fileDatabase;
    }

    public void loadSnapshot() throws IOException {
        LOGGER.info("Loading existing snapshot from file: " + snapshotFile.getAbsolutePath());
        TypeReference<FolderSnapshot> typeReference = new TypeReference<FolderSnapshot>() {};
        snapshot = JSONUtils.loadFromDisk(snapshotFile, typeReference);
        LOGGER.info("Snapshot has been loaded successfully");
    }

    public void checkSnapshot() throws IOException {
        LOGGER.info("Starting to check Snapshot...");
        checkFilesInSnapshot();
        checkForFilesNotInSnapshot();
        checkForEmptyFolders();
        LOGGER.info("Check Snapshot is done");
    }

    private void checkFilesInSnapshot() throws IOException {
        for (RelativeFileMetadata fileInSnapshot : snapshot.getFilesMap().values()) {
            LOGGER.info("Checking the file: " + fileInSnapshot.getRelativePath());

            if (fileInSnapshot.getStatus() != Status.Ok) {
                errorFilesInSnapshot.add(fileInSnapshot);
                LOGGER.warn("File in Snapshot - Status is NOT Ok: " + fileInSnapshot.getStatus().name() + ": "
                        + String.join("; ", fileInSnapshot.getStatusMessages()));
                continue;
            }

            // Updating FileMetadata of destination file
            Path destinationFilePath = destinationFolder.resolve(fileInSnapshot.getRelativePath());
            fileDatabase.updateEntryFromDisk(destinationFilePath, true);

            // Checking that destination file exists on the disk
            AbsoluteFileMetadata destinationFileMetadata = fileDatabase.getFileMetadataByAbsolutePath(destinationFilePath.toString());
            if (destinationFileMetadata == null) {
                notFoundFiles.add(fileInSnapshot);
                LOGGER.error("File not found - In Snapshot: " + fileInSnapshot.getRelativePath() + ", destination file: " + destinationFilePath);
                continue;
            }

            // Checking that file Sizes are matching
            if (destinationFileMetadata.getSize() != fileInSnapshot.getSize()) {
                fileSizeMismatchFiles.add(new Pair<>(fileInSnapshot, destinationFileMetadata));
                LOGGER.error("File Size mismatch - In Snapshot: " + fileInSnapshot + ", destination file: " + destinationFileMetadata);
                continue;
            }

            // Checking that file Checksums are matching
            fileDatabase.computeChecksumsIfNotComputedYet(destinationFilePath.toString());
            if (!destinationFileMetadata.equalsByChecksum(fileInSnapshot)) {
                checksumMismatchFiles.add(new Pair<>(fileInSnapshot, destinationFileMetadata));
                LOGGER.error("File Checksum mismatch - In Snapshot: " + fileInSnapshot + ", destination file: " + destinationFileMetadata);
                continue;
            }

            // Considering File as Correct
            correctFiles.add(fileInSnapshot);
        }
    }

    private void checkForFilesNotInSnapshot() throws IOException {
        PathIterator pathIterator = new PathIterator(destinationFolder, true);
        while (pathIterator.hasNext()) {
            Path currentFilePath = pathIterator.next();
            Path pathInSnapshot = destinationFolder.relativize(currentFilePath);

            RelativeFileMetadata fileInSnapshot = snapshot.getFilesMap().get(pathInSnapshot.toString());
            if (fileInSnapshot == null) {
                AbsoluteFileMetadata fileMetadata = new AbsoluteFileMetadata(currentFilePath);
                filesNotInSnapshot.add(fileMetadata);
            } else if (fileInSnapshot.getStatus() != Status.Ok) {
                AbsoluteFileMetadata fileMetadata = new AbsoluteFileMetadata(currentFilePath);
                filesExistButInSnapshotIsNotOk.add(new Pair<>(fileInSnapshot, fileMetadata));
            }
        }
    }

    private void checkForEmptyFolders() {
        // TODO Implement Check for Empty Folders
        //      This could be efficienlty achieve by mark empty folders on previous step
    }

    public void reportResult() {
        StringBuilder sb = new StringBuilder();
        sb.append("Results:\n");

        sb.append("\tCorrect files:\n");
        correctFiles.stream().forEach(f -> {
            sb.append(f.getRelativePath())
                    .append(" (").append(f.getSize()).append(" - ")
                    .append(FormattingUtils.humanReadableSize(f.getSize())).append("b)\n");
        });

        sb.append("\tNot found files:\n");
        notFoundFiles.stream().forEach(f -> {
            sb.append(f.getRelativePath())
                    .append(" (").append(f.getSize()).append(" - ")
                    .append(FormattingUtils.humanReadableSize(f.getSize())).append("b)\n");
        });

        List<String> tableData = new ArrayList<>();
        sb.append("\tFile Size mismatch files:\n");
        fileSizeMismatchFiles.stream().forEach(p -> {
            tableData.clear();
            tableData.add("Snapshot: ");
            tableData.add(p.getKey().getRelativePath());
            tableData.add(" (" + p.getKey().getSize() + " - " + FormattingUtils.humanReadableSize(p.getKey().getSize()) + "b)");
            tableData.add("Disk: ");
            tableData.add(p.getValue().getAbsolutePath());
            tableData.add(" (" + p.getValue().getSize() + " - " + FormattingUtils.humanReadableSize(p.getValue().getSize()) + "b)");
            sb.append(FormattingUtils.alignLongStringsByRightSide(3, tableData));

        });

        sb.append("\tChecksum mismatch files:\n");
        checksumMismatchFiles.stream().forEach(p -> {
            tableData.clear();
            tableData.add("Snapshot: ");
            tableData.add(p.getKey().getRelativePath());
            tableData.add(" (" + p.getKey().getSize() + " - " + FormattingUtils.humanReadableSize(p.getKey().getSize()) + "b)");
            tableData.add("Disk: ");
            tableData.add(p.getValue().getAbsolutePath());
            tableData.add(" (" + p.getValue().getSize() + " - " + FormattingUtils.humanReadableSize(p.getValue().getSize()) + "b)");
            sb.append(FormattingUtils.alignLongStringsByRightSide(3, tableData));
            tableData.clear();
            p.getKey().getChecksums().entrySet().forEach(e -> {
                tableData.add("        Checksum Snapshot " + e.getKey());
                tableData.add(e.getValue());
                tableData.add("Checksum Disk " + e.getKey());
                String diskChecksumValue = p.getValue().getChecksums().containsKey(e.getKey())
                        ? p.getValue().getChecksums().get(e.getKey()) : "null";
                tableData.add(diskChecksumValue);
            });
            p.getValue().getChecksums().entrySet().forEach(e -> {
                // Dumping only the entries which were not dumped by the previous loop
                if (!p.getKey().getChecksums().containsKey(e.getKey())) {
                    tableData.add("Checksum Disk " + e.getKey());
                    tableData.add(e.getValue());
                }
            });
            sb.append(FormattingUtils.alignLongStringsByRightSide(2, tableData));
        });

        sb.append("\tExisting Files which are not in the Snapshot:\n");
        filesNotInSnapshot.stream().forEach(p -> {
            sb.append(p.getAbsolutePath())
                    .append(" (").append(p.getSize()).append(" - ")
                    .append(FormattingUtils.humanReadableSize(p.getSize())).append("b)\n");
        });

        sb.append("\tExisting Files which are In the Snapshot, but in Snapshot their Status is NOT Ok:\n");
        filesExistButInSnapshotIsNotOk.stream().forEach(p -> {
            tableData.clear();
            tableData.add("Snapshot: ");
            tableData.add(p.getKey().getRelativePath());
            tableData.add(" (" + p.getKey().getSize() + " - " + FormattingUtils.humanReadableSize(p.getKey().getSize()) + "b)");
            tableData.add("Disk: ");
            tableData.add(p.getValue().getAbsolutePath());
            tableData.add(" (" + p.getValue().getSize() + " - " + FormattingUtils.humanReadableSize(p.getValue().getSize()) + "b)");
            sb.append(FormattingUtils.alignLongStringsByRightSide(3, tableData));
        });

        // Completion Status
        long allFilesInSnapshotSize = snapshot.getFilesMap().values().stream()
                .mapToLong(f -> f.getSize()).sum();
        long fileSizeMismatchFilesSizeAsOnDisk = fileSizeMismatchFiles.stream()
                .map(p -> p.getValue()).mapToLong(f -> f.getSize()).sum();
        long correctFilesSize = correctFiles.stream()
                .mapToLong(f -> f.getSize()).sum();
        long completionSizeMin = correctFilesSize;
        long completionSizeMax = correctFilesSize + fileSizeMismatchFilesSizeAsOnDisk;
        sb.append("Completion: ")
                .append(FormattingUtils.humanReadableSize(completionSizeMin)).append("b (")
                        .append(FormattingUtils.percentage(completionSizeMin, allFilesInSnapshotSize, 2)).append(")");
        if (completionSizeMin != completionSizeMax) {
            sb.append(" - ").append(FormattingUtils.humanReadableSize(completionSizeMax)).append("b (")
                    .append(FormattingUtils.percentage(completionSizeMax, allFilesInSnapshotSize, 2)).append(")");
        }
        if (completionSizeMin < allFilesInSnapshotSize) {
            sb.append(" of ").append(FormattingUtils.humanReadableSize(allFilesInSnapshotSize)).append("b");
        }

        // Error Statuses in Snapshot File
        if (!errorFilesInSnapshot.isEmpty()) {
            sb.append("\n\t!!! WARNING !!! The following Files in Snapshot are NOT Ok:\n");
            errorFilesInSnapshot.forEach(f -> {
                sb.append(f.getRelativePath())
                        .append(" - (").append(f.getSize()).append(" - ").append(FormattingUtils.humanReadableSize(f.getSize()))
                        .append("b) - ").append(f.getStatus().name()).append(": ").append(String.join(", ", f.getStatusMessages()));
            });
        }

        LOGGER.info(sb.toString());
    }
}
