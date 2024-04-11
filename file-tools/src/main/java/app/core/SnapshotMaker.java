package app.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.type.TypeReference;

import app.checksum.ChecksumComputer;
import app.core.dto.Configuration;
import app.core.dto.Snapshot;
import app.core.dto.SnapshotFile;
import app.files.PathIterator;
import app.utils.FormattingUtils;
import app.utils.JSONUtils;
import app.utils.PathUtils;
import exceptions.files.InvalidFileSizeException;

public class SnapshotMaker {
    private static final Logger LOGGER = LogManager.getLogger(SnapshotMaker.class);

    private Configuration configuration;

    public SnapshotMaker(Configuration configuration) {
        this.configuration = configuration;
    }

    public Snapshot makeSnapshot(Path snapshotFolder) throws IOException, NoSuchAlgorithmException {
        Snapshot snapshot = loadOrCreateSnapshot(snapshotFolder);
        PathIterator iterator = scanFilesOnDisk(snapshotFolder, snapshot);
        computeChecksums(snapshotFolder, snapshot, iterator);
        return snapshot;
    }

    private Snapshot loadOrCreateSnapshot(Path folder) throws IOException {
        String snapshotName = folder.getFileName().toString();

        String snapshotIncompleteFilename = snapshotName + "-INCOMPLETE-snapshot.json";
        File snapshotIncompleteFile = new File(snapshotIncompleteFilename).getAbsoluteFile();
        boolean incompleteFileExists = snapshotIncompleteFile.exists();

        String snapshotCompleteFilename = snapshotName + "-snapshot.json";
        File snapshotCompleteFile = new File(snapshotCompleteFilename).getAbsoluteFile();
        boolean completeFileExists = snapshotCompleteFile.exists();

        Snapshot snapshot;
        if (completeFileExists) {
            // Load Existing Snapshot from Complete Snapshot File
            snapshot = loadSnapshot(snapshotName, snapshotCompleteFile);
            if (incompleteFileExists) {
                // Delete Incomplete Snapshot File
                PathUtils.deleteFile(snapshotIncompleteFile.toPath());
            }
        } else if (incompleteFileExists) {
            // Load Existing Snapshot from Incomplete Snapshot File
            snapshot = loadSnapshot(snapshotName, snapshotIncompleteFile);
        } else {
            // Create New Snapshot
            LOGGER.info("Creating new snapshot: " + snapshotName);
            snapshot = new Snapshot();
            snapshot.setName(snapshotName);
        }

        snapshot.setSaveFileIncomplete(snapshotIncompleteFile);
        snapshot.setSaveFileComplete(snapshotCompleteFile);
        snapshot.setMinimalSaveIntervalInMS(configuration.getSnapshotMinimalSaveIntervalInMS());

        return snapshot;
    }

    // TODO Move to FolderSnapshot class as a static method
    private Snapshot loadSnapshot(String snapshotName, File snapshotFile) throws IOException {
        LOGGER.info("Loading existing snapshot \"" + snapshotName + "\" from " + snapshotFile.getAbsolutePath());
        TypeReference<Snapshot> typeReference = new TypeReference<Snapshot>() {};
        Snapshot snapshot = JSONUtils.loadFromDisk(snapshotFile, typeReference);
        LOGGER.info("Snapshot has been loaded successfully");
        return snapshot;
    }

    private PathIterator scanFilesOnDisk(Path snapshotFolder, Snapshot snapshot) throws IOException {
        // Marking existing files in the Snapshot as they potentially does not exist on disk anymore
        // Later all existing files will be whitelisted during Scan phase
        for (SnapshotFile fileMetadata : snapshot.getFilesMap().values()) {
            fileMetadata.setExistsOnDiskNow(false);
        }

        // Scan Files
        LOGGER.info("Scanning files for the snapshot...");
        PathIterator iterator = new PathIterator(snapshotFolder, true);
        while (iterator.hasNext()) {
            Path currentFile = iterator.next();
            LOGGER.debug("Found file: " + currentFile.toString());

            // Not adding the Snapshot Files, Log-files, Bat-files, etc to the Snapshot
            if (currentFile.toString().endsWith("-snapshot.json")
                    || currentFile.toString().endsWith("-snapshot.backup.json")
                    || currentFile.toString().endsWith("-INCOMPLETE-snapshot.json")
                    || currentFile.toString().endsWith("file-tools.log")
                    || currentFile.toString().matches(".*snapshot\\-maker\\-v[0-9]{3}\\.bat$")
                    ) {
                LOGGER.debug("Skipping file \"" + currentFile.toString() + "\" because it is a file of this application");
                continue;
            }

            // If File not in the Snapshot yet or the File is outdated, adding it into the Snapshot
            SnapshotFile newFileMetadata = new SnapshotFile(currentFile, snapshotFolder);
            newFileMetadata.setExistsOnDiskNow(true);
            SnapshotFile existingFileMetadata = snapshot.getFileMetadata(newFileMetadata.getRelativePath());
            if (existingFileMetadata == null) {
                LOGGER.debug("Adding as a new file to the Snapshot");
                // Adding a new file to the Snapshot
                snapshot.addFileMetadata(newFileMetadata);
            } else if(!existingFileMetadata.equalsByMetadata(newFileMetadata)) {
                LOGGER.debug("File in the Snapshot is outdated, replacing with a new FileMetadata");
                // Overwriting old file metadata with the new (actual) one
                snapshot.addFileMetadata(newFileMetadata);
            } else {
                LOGGER.debug("File is already in the Snapshot and looks up-to-date");
                // Marking the existing file that it still exists on the disk
                existingFileMetadata.setExistsOnDiskNow(true);
            }
        }

        snapshot.suggestSave();
        LOGGER.info("File scanning is done");
        return iterator;
    }

    private void computeChecksums(Path snapshotFolder, Snapshot snapshot, PathIterator iterator) throws NoSuchAlgorithmException, IOException {
        // Filtering Files to Compute Checksums
        List<String> checksumAlgorithms = configuration.getChecksumAlgorithms();

        LOGGER.info("Computing Checksums "
                + String.join(", ", checksumAlgorithms)
                + " for the files for the snapshot...");
        List<SnapshotFile> filesToComputeChecksums = new ArrayList<>();
        long totalFileSizeToComputeChecksums = 0;
        Iterator<SnapshotFile> fileMetadataIterator = snapshot.getFilesMap().values().iterator();
        while (fileMetadataIterator.hasNext()) {
            SnapshotFile fileMetadata = fileMetadataIterator.next();
            // If a file was not whitelisted during folder scan, removing it from the Snapshot
            if (!fileMetadata.isExistsOnDiskNow()) {
                iterator.remove();
            // If a file is on the disk, but checksums were not computed yet, adding it to the list to compute checksums
            } else if (!fileMetadata.hasChecksums(checksumAlgorithms)) {
                filesToComputeChecksums.add(fileMetadata);
                totalFileSizeToComputeChecksums += fileMetadata.getSize();
            }
        }

        // Computing Checksums
        ChecksumComputer checksumComputer = new ChecksumComputer(checksumAlgorithms);
        checksumComputer.setAllFilesSize(totalFileSizeToComputeChecksums);
        checksumComputer.reset();
        for (SnapshotFile fileMetadata : filesToComputeChecksums) {
            if (fileMetadata.getStatus() != SnapshotFile.Status.Ok) {
                LOGGER.info("Status of FileMetadata is NOT Ok, skipping Checksum Computing for this file...");
                continue;
            }

            LOGGER.info("Computing Checksums for file: " + fileMetadata.getRelativePath()
                    + " (" + FormattingUtils.humanReadableSizeBi(fileMetadata.getSize()) + "b)...");

            // Checking actual file size
            File file = new File(snapshotFolder.toFile(), fileMetadata.getRelativePath());
            long sizeOfFileInSnapshot = fileMetadata.getSize();
            long sizeOfRealFile = file.length();
            if (sizeOfRealFile != sizeOfFileInSnapshot) {
                String message = "Invalid file size - name: " + file.getAbsolutePath()
                        + ", expected size: " + fileMetadata.getSize() + ", actual size: " + sizeOfRealFile
                        + ". This probably means that the copy data to the folder is not finished yet.";
                throw new InvalidFileSizeException(message);
            }

            try {
                Map<String, String> checksums = checksumComputer.computeChecksums(file);
                snapshot.addChecksums(fileMetadata, checksums);
            } catch (IOException e) {
                snapshot.setFileContentReadError(fileMetadata, e.getMessage() + ": " + file.getAbsolutePath());
                LOGGER.error("Error due to compute checksum for the file: " + file.getAbsolutePath(), e);
            }
            snapshot.suggestSave();
        }

        LOGGER.info("Checksum computing is done, errors: " + checksumComputer.getNumberOfErrors());
    }
}
