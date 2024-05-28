package app.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.type.TypeReference;

import app.checksum.ChecksumComputer;
import app.collection.RMap;
import app.core.comparator.ClosestToDestFolderComparator;
import app.core.dto.Configuration;
import app.core.dto.FileDatabase;
import app.core.dto.FileMetadata;
import app.core.dto.Snapshot;
import app.core.dto.SnapshotFile;
import app.core.file.operations.CopyOperation;
import app.core.file.operations.DeleteFileOperation;
import app.core.file.operations.FileOperation;
import app.core.file.operations.MoveFileOperation;
import app.files.PathIterator;
import app.progress.DottedProgressPrinter;
import app.structures.Pair;
import app.utils.ConsoleUtils;
import app.utils.FormattingUtils;
import app.utils.JSONUtils;
import app.utils.PathUtils;

// TODO Refactor - This class is too heavy, extract some functionality into other (maybe new) classes
public class SnapshotAssembler {
    private static final Logger LOGGER = LogManager.getLogger(SnapshotAssembler.class);

    private Configuration configuration;

    private File snapshotFile;
    private Path destinationFolder;

    private Snapshot snapshot;
    private FileDatabase fileDatabase;

    private Map<Long, List<SnapshotFile>> snapshotFilesMapBySize;
    private Map<Long, List<FileMetadata>> filesInFileDatabaseMapBySize;
    private List<FileMetadata> filesToComputeChecksums;

    private List<SnapshotFile> snapshotFilesWithoutCandidates;

    private List<FileOperation> fileOperations;

    private long totalCopySize;

    private List<SnapshotFile> missingFiles;
    private List<Pair<SnapshotFile, FileMetadata>> filesNotMatchedByMetadata;
    private List<Pair<SnapshotFile, FileMetadata>> filesNotMatchedByChecksum;
    private List<Pair<SnapshotFile, FileMetadata>> redundantFilesInDestinationFolder;

    public SnapshotAssembler() {
        filesToComputeChecksums = new ArrayList<>();
        snapshotFilesWithoutCandidates = new ArrayList<>();
        fileOperations = new ArrayList<>();
        missingFiles = new ArrayList<>();
        filesNotMatchedByMetadata = new ArrayList<>();
        filesNotMatchedByChecksum = new ArrayList<>();
        redundantFilesInDestinationFolder = new ArrayList<>();
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
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
        TypeReference<Snapshot> typeReference = new TypeReference<Snapshot>() {};
        snapshot = JSONUtils.loadFromDisk(snapshotFile, typeReference);
        LOGGER.info("Snapshot has been loaded successfully");
    }

    public void findFilesBySize() {
        LOGGER.info("Searching Files by Size...");

        // Creating Map Snapshot Files by Size
        snapshotFilesMapBySize = new HashMap<>();
        for (SnapshotFile fileMetadata : snapshot.getFilesMap().values()) {
            long fileSize = fileMetadata.getSize();
            List<SnapshotFile> fileList = snapshotFilesMapBySize.get(fileSize);
            if (fileList == null) {
                fileList = new ArrayList<>();
                snapshotFilesMapBySize.put(fileSize, fileList);
            }
            fileList.add(fileMetadata);
        }

        // Creating Map FileDatabase Files by Size
        filesInFileDatabaseMapBySize = new HashMap<>();
        for (FileMetadata fileMetadata : fileDatabase.getActiveFiles().values()) {
            long fileSize = fileMetadata.getSize();
            List<FileMetadata> fileList = filesInFileDatabaseMapBySize.get(fileSize);
            if (fileList == null) {
                fileList = new ArrayList<>();
                filesInFileDatabaseMapBySize.put(fileSize, fileList);
            }
            fileList.add(fileMetadata);
        }

        // Adding FileMetadata from Disks to the List to Compute Checksums Where FileSize matches the FileSize of the Files in the Snapshot
        for (Entry<Long, List<SnapshotFile>> entry : snapshotFilesMapBySize.entrySet()) {
            List<FileMetadata> filesWithParticularSize = filesInFileDatabaseMapBySize.get(entry.getKey());
            if (filesWithParticularSize != null && !filesWithParticularSize.isEmpty()) {
                filesToComputeChecksums.addAll(filesWithParticularSize);
            }
        }

        // Removing FileMetadata which already have Checksums computed
        Iterator<FileMetadata> iterator = filesToComputeChecksums.iterator();
        while (iterator.hasNext()) {
            FileMetadata fileMetadata = iterator.next();
            List<String> checksumAlgorithms = configuration.getChecksumAlgorithms();
            if (fileMetadata.hasChecksums(checksumAlgorithms)) {
                iterator.remove();
            }
        }
    }

    public void computeChecksumsForCandidates() throws NoSuchAlgorithmException, IOException {
        LOGGER.info("Computing Checksums for the Candidates...");
        List<String> checksumAlgorithms = configuration.getChecksumAlgorithms();
        ChecksumComputer checksumComputer = new ChecksumComputer(checksumAlgorithms);

        long totalFileSizeToComputeChecksums = filesToComputeChecksums.stream().mapToLong(f -> f.getSize()).sum();
        checksumComputer.setAllFilesSize(totalFileSizeToComputeChecksums);
        checksumComputer.reset();

        for (FileMetadata fileMetadata : filesToComputeChecksums) {
            File file = new File(fileMetadata.getAbsolutePath());
            Map<String, String> checksums = checksumComputer.computeChecksums(file);
            fileMetadata.addChecksums(checksums);
            fileDatabase.suggestSave();
        }
        fileDatabase.saveIfNeededComplete();
    }

    // TODO Refactor Remove throws IOException after removing throws IOException of all functions calls
    public void findFilesByChecksums() throws IOException {
        LOGGER.info("Searching Files by Checksums...");
        // Comparator to find best suitable candidates by sorting a candidate List
        Comparator<FileMetadata> candidateComparator = new ClosestToDestFolderComparator(destinationFolder.toString());

        // Taking a group of Files from Snapshot with the same FileSize, Loop over All Snapshot Files
        for (Entry<Long, List<SnapshotFile>> snapshotFilesEntry : snapshotFilesMapBySize.entrySet()) {
            // If there is no suitable candidates on the disks for the particular FileSize, adding to NotFound and continue the loop
            if (!filesInFileDatabaseMapBySize.containsKey(snapshotFilesEntry.getKey())) {
                snapshotFilesWithoutCandidates.addAll(snapshotFilesEntry.getValue());
                continue;
            }

            // Splitting the Group of Snapshot Files with the same FileSize into the group(s) by Checksums
            List<SnapshotFile> snapshotFilesWithSameSize = new ArrayList<>(snapshotFilesEntry.getValue());
            // List<Pair<Checksums, List<SnapshotFile>>>
            List<Pair<RMap<String, String>, List<SnapshotFile>>> snapshotFilesByChecksums
                    = splitSnapshotFilesBySizeIntoFilesByChecksum(snapshotFilesWithSameSize);

            // Splitting the Group of FilesOnDisks with the same FileSize into the group(s) by Checksums
            List<FileMetadata> filesOnDisksWithSameSize = new ArrayList<>(filesInFileDatabaseMapBySize.get(snapshotFilesEntry.getKey()));
            // List<Pair<Checksums, List<FileOnDisk>>>
            List<Pair<RMap<String, String>, List<FileMetadata>>> filesOnDisksByChecksums
                    = splitFilesOnDisksBySizeIntoFilesByChecksum(filesOnDisksWithSameSize);

            // Loop over SnapshotFilesGroups with the same Checksums
            for (Pair<RMap<String, String>, List<SnapshotFile>> snapshotFilesWithSameChecksumsEntry : snapshotFilesByChecksums) {
                // Checksums Map to find a FilesOnDisksGroup
                RMap<String, String> checksums = snapshotFilesWithSameChecksumsEntry.getKey();
                // List of the Files with the same Checksums
                List<SnapshotFile> snapshotFilesWithSameChecksumsList = snapshotFilesWithSameChecksumsEntry.getValue();
                // FilesOnDisksGroup that matches the Checksums or null if there is no matching group
                List<FileMetadata> filesOnDisksWithSameChecksums = findFilesOnDisksGroupByChecksum(filesOnDisksByChecksums, checksums);
                // If there is no matching group -> adding Snapshot Files to FilesWithoutCandidates and continue the loop
                if (filesOnDisksWithSameChecksums == null) {
                    snapshotFilesWithoutCandidates.addAll(snapshotFilesWithSameChecksumsList);
                    continue;
                }

                // Otherwise looking for the best candidate for each Snapshot File

                List<FileMetadata> movableFiles = filesOnDisksWithSameChecksums.stream()
                        .filter(f -> !f.isReadOnly()).collect(Collectors.toList());
                movableFiles.sort(candidateComparator);

                List<FileMetadata> nonMovableFiles = filesOnDisksWithSameChecksums.stream()
                        .filter(f -> f.isReadOnly()).collect(Collectors.toList());
                nonMovableFiles.sort(candidateComparator);

                if (movableFiles.size() >= snapshotFilesWithSameChecksumsList.size()) {
                    // If we have enough Movable Files -> Adding Move Operations for all Snapshot Files and then Delete Operations for Remnant FilesOnDisks
                    Iterator<SnapshotFile> snapshotFilesIterator = snapshotFilesWithSameChecksumsList.iterator();
                    Iterator<FileMetadata> movableFilesIterator = movableFiles.iterator();
                    while (snapshotFilesIterator.hasNext()) {
                        SnapshotFile snapshotFileMetadata = snapshotFilesIterator.next();
                        FileMetadata movableFileMetadata = movableFilesIterator.next();
                        Path sourcePath = Paths.get(movableFileMetadata.getAbsolutePath());
                        Path destinationPath = destinationFolder.resolve(snapshotFileMetadata.getRelativePath());
                        FileOperation fileOperation = new MoveFileOperation(sourcePath, destinationPath);
                        fileOperation.setLastModifiedTime(snapshotFileMetadata.getLastModified());
                        fileOperations.add(fileOperation);
                    }
                    while (movableFilesIterator.hasNext()) {
                        FileMetadata movableFileMetadata = movableFilesIterator.next();
                        Path targetPath = Paths.get(movableFileMetadata.getAbsolutePath());
                        FileOperation fileOperation = new DeleteFileOperation(targetPath, "Too many similar moveable files");
                        fileOperations.add(fileOperation);
                    }
                } else if (!movableFiles.isEmpty()) {
                    // Lack of Movable Files
                    if (nonMovableFiles.isEmpty()) {
                        // Lack of Movable Files and there is no Non-Movable Files -> Copy Operations until enough Movable Files, then Move Operations
                        Iterator<SnapshotFile> snapshotFilesIterator = snapshotFilesWithSameChecksumsList.iterator();
                        Iterator<FileMetadata> movableFilesIterator = movableFiles.iterator();
                        SnapshotFile snapshotFileMetadata = snapshotFilesIterator.next();
                        FileMetadata movableFileMetadata = movableFilesIterator.next();
                        // Copy Operations
                        int lackOfMovableFiles = snapshotFilesWithSameChecksumsList.size() - movableFiles.size();
                        for (int i = 0; i < lackOfMovableFiles; i++) {
                            Path sourcePath = Paths.get(movableFileMetadata.getAbsolutePath());
                            Path destinationPath = destinationFolder.resolve(snapshotFileMetadata.getRelativePath());
                            FileOperation fileOperation = new CopyOperation(sourcePath, destinationPath);
                            fileOperation.setLastModifiedTime(snapshotFileMetadata.getLastModified());
                            fileOperations.add(fileOperation);
                            snapshotFileMetadata = snapshotFilesIterator.next();
                        }
                        // Moving the First Movable File
                        Path sourcePath = Paths.get(movableFileMetadata.getAbsolutePath());
                        Path destinationPath = destinationFolder.resolve(snapshotFileMetadata.getRelativePath());
                        FileOperation fileOperation = new MoveFileOperation(sourcePath, destinationPath);
                        fileOperation.setLastModifiedTime(snapshotFileMetadata.getLastModified());
                        fileOperations.add(fileOperation);
                        // Checking if there are more files - moving them as well
                        while (snapshotFilesIterator.hasNext()) {
                            snapshotFileMetadata = snapshotFilesIterator.next();
                            movableFileMetadata = movableFilesIterator.next();
                            sourcePath = Paths.get(movableFileMetadata.getAbsolutePath());
                            destinationPath = destinationFolder.resolve(snapshotFileMetadata.getRelativePath());
                            fileOperation = new MoveFileOperation(sourcePath, destinationPath);
                            fileOperation.setLastModifiedTime(snapshotFileMetadata.getLastModified());
                            fileOperations.add(fileOperation);
                        }
                    } else {
                        // Lack of Movable Files and there is(are) Movable File(s) -> Move Operations for all Movable Files and Copy for the first non-movable
                        Iterator<SnapshotFile> snapshotFilesIterator = snapshotFilesWithSameChecksumsList.iterator();
                        Iterator<FileMetadata> movableFilesIterator = movableFiles.iterator();
                        // Move all Movable Files first
                        while (movableFilesIterator.hasNext()) {
                            SnapshotFile snapshotFileMetadata = snapshotFilesIterator.next();
                            FileMetadata movableFileMetadata = movableFilesIterator.next();
                            Path sourcePath = Paths.get(movableFileMetadata.getAbsolutePath());
                            Path destinationPath = destinationFolder.resolve(snapshotFileMetadata.getRelativePath());
                            FileOperation fileOperation = new MoveFileOperation(sourcePath, destinationPath);
                            fileOperation.setLastModifiedTime(snapshotFileMetadata.getLastModified());
                            fileOperations.add(fileOperation);
                        }
                        // Then Copy the First Non-Movable File
                        FileMetadata nonMovableFileMetadata = nonMovableFiles.get(0);
                        while (snapshotFilesIterator.hasNext()) {
                            SnapshotFile snapshotFileMetadata = snapshotFilesIterator.next();
                            Path sourcePath = Paths.get(nonMovableFileMetadata.getAbsolutePath());
                            Path destinationPath = destinationFolder.resolve(snapshotFileMetadata.getRelativePath());
                            FileOperation fileOperation = new CopyOperation(sourcePath, destinationPath);
                            fileOperation.setLastModifiedTime(snapshotFileMetadata.getLastModified());
                            fileOperations.add(fileOperation);
                        }
                    }
                } else {
                    // There is No Movable Files at all, but there is(are) NonMovable File(s) -> Copy Operations for the first non-movable file
                    Iterator<SnapshotFile> snapshotFilesIterator = snapshotFilesWithSameChecksumsList.iterator();
                    FileMetadata nonMovableFileMetadata = nonMovableFiles.get(0);
                    while (snapshotFilesIterator.hasNext()) {
                        SnapshotFile snapshotFileMetadata = snapshotFilesIterator.next();
                        Path sourcePath = Paths.get(nonMovableFileMetadata.getAbsolutePath());
                        Path destinationPath = destinationFolder.resolve(snapshotFileMetadata.getRelativePath());
                        FileOperation fileOperation = new CopyOperation(sourcePath, destinationPath);
                        fileOperation.setLastModifiedTime(snapshotFileMetadata.getLastModified());
                        fileOperations.add(fileOperation);
                    }
                }
            }
        }
    }

    private List<Pair<RMap<String, String>, List<SnapshotFile>>> splitSnapshotFilesBySizeIntoFilesByChecksum(
            List<SnapshotFile> snapshotFilesWithSameSize) {
        List<Pair<RMap<String, String>, List<SnapshotFile>>> snapshotFilesByChecksums = new ArrayList<>();

        while (snapshotFilesWithSameSize.size() > 0) {
            // Iterator to remove files which have matches in order to split files into the groups
            Iterator<SnapshotFile> iterator = snapshotFilesWithSameSize.iterator();
            // First FileMetadata to make a group
            SnapshotFile snapshotFileMetadata = iterator.next();
            iterator.remove();
            // List where FileMetadata with the same Checksums will be added
            List<SnapshotFile> snapshotFilesWithSameChecksum = new ArrayList<>();
            snapshotFilesWithSameChecksum.add(snapshotFileMetadata);
            // A Structure like Pair<Checksums, List<SnapshotFiles>>
            Pair<RMap<String, String>, List<SnapshotFile>> snapshotFilesChecksumEntry
                    = new Pair<>(snapshotFileMetadata.getChecksums(), snapshotFilesWithSameChecksum);
            snapshotFilesByChecksums.add(snapshotFilesChecksumEntry);

            // Looking for the Files with same Checksums among the Files with same FileSize
            while (iterator.hasNext()) {
                SnapshotFile candidate = iterator.next();
                if (candidate.equalsByChecksum(snapshotFileMetadata)) {
                    snapshotFilesWithSameChecksum.add(candidate);
                    iterator.remove();
                }
            }
        }
        return snapshotFilesByChecksums;
    }

    private List<Pair<RMap<String, String>, List<FileMetadata>>> splitFilesOnDisksBySizeIntoFilesByChecksum(
            List<FileMetadata> filesOnDisksWithSameSize) {
        List<Pair<RMap<String, String>, List<FileMetadata>>> filesOnDisksByChecksums = new ArrayList<>();

        while (filesOnDisksWithSameSize.size() > 0) {
            // Iterator to remove files which have matches in order to split files into the groups
            Iterator<FileMetadata> iterator = filesOnDisksWithSameSize.iterator();
            // First FileMetadata to make a group
            FileMetadata fileOnDiskMetadata = iterator.next();
            iterator.remove();
            // List where FileMetadata with the same Checksums will be added
            List<FileMetadata> filesOnDisksWithSameChecksum = new ArrayList<>();
            filesOnDisksWithSameChecksum.add(fileOnDiskMetadata);
            // A Structure like Pair<Checksums, List<FileOnDisk>>
            Pair<RMap<String, String>, List<FileMetadata>> filesOnDisksChecksumEntry
                    = new Pair<>(fileOnDiskMetadata.getChecksums(), filesOnDisksWithSameChecksum);
            filesOnDisksByChecksums.add(filesOnDisksChecksumEntry);

            // Looking for the Files with same Checksums among the Files with same FileSize
            while (iterator.hasNext()) {
                FileMetadata candidate = iterator.next();
                if (candidate.equalsByChecksum(fileOnDiskMetadata)) {
                    filesOnDisksWithSameChecksum.add(candidate);
                    iterator.remove();
                }
            }
        }
        return filesOnDisksByChecksums;
    }

    private List<FileMetadata> findFilesOnDisksGroupByChecksum(
            List<Pair<RMap<String, String>, List<FileMetadata>>> filesOnDisksByChecksums, RMap<String, String> checksums) {
        for (Pair<RMap<String, String>, List<FileMetadata>> filesOnDisksEntry : filesOnDisksByChecksums) {
            if (filesOnDisksEntry.getValue().get(0).equalsByChecksum(checksums)) {
                return filesOnDisksEntry.getValue();
            }
        }
        return null;
    }

    public void reportNotFoundFiles() {
        String filesNotFoundAsString = snapshotFilesWithoutCandidates.stream()
                .map(f -> f.getRelativePath() + " (" + f.getSize() + ")")
                .collect(Collectors.joining("\n"));
        long filesNotFoundTotalSize = snapshotFilesWithoutCandidates.stream()
                .mapToLong(f -> f.getSize())
                .sum();
        Level logLevel = snapshotFilesWithoutCandidates.isEmpty() ? Level.INFO : Level.ERROR;
        LOGGER.log(logLevel, "The following files were NOT found: "
                + snapshotFilesWithoutCandidates.size() + " file(s), "
                + " total size: " + FormattingUtils.humanReadableSizeBi(filesNotFoundTotalSize)
                + "\n" + filesNotFoundAsString);
    }

    public void checkFileOperations() {
        LOGGER.info("Checking File Operations...");
        Iterator<FileOperation> operationIterator = fileOperations.iterator();
        while (operationIterator.hasNext()) {
            FileOperation operation = operationIterator.next();
            if (!operation.isValid()) {
                LOGGER.info("Operation " + operation + " is invalid, removing from the Operation List");
                operationIterator.remove();
            }
        }
    }

    public boolean getUserConfirmationForProceedFileOperations() throws IOException {
        LOGGER.info("Getting User Confirmation for Proceed File Operations...");

        StringBuilder sb = new StringBuilder();
        sb.append("File Operations:\n");

        for (FileOperation fileOperation : fileOperations) {
            sb.append(fileOperation).append("\n");
        }

        totalCopySize = fileOperations.stream()
                .mapToLong(o -> o.getCopyDataSize())
                .sum();
        sb.append("Total Real Data Copy Size: ").append(FormattingUtils.humanReadableSizeBi(totalCopySize)).append("\n");

        long totalMoveSize = fileOperations.stream()
                .filter(o -> (o instanceof MoveFileOperation))
                .mapToLong(o -> o.getFileSize())
                .sum();
        sb.append("Total Move Size: ").append(FormattingUtils.humanReadableSizeBi(totalMoveSize)).append("\n");

        long totalDeleteSize = fileOperations.stream()
                .filter(o -> (o instanceof DeleteFileOperation))
                .mapToLong(o -> o.getFileSize())
                .sum();
        sb.append("Total Delete Size: ").append(FormattingUtils.humanReadableSizeBi(totalDeleteSize)).append("\n");

        sb.append("Do you confirm the operations (yes/no)? ");
        LOGGER.info("User Confirmation text:\n" + sb.toString());

        boolean userConfirmation = ConsoleUtils.userConfirm(null);
        if (userConfirmation) {
            LOGGER.info("User Confirmed the Real File Operations...");
        } else {
            LOGGER.info("User did NOT Confirm the Real File Operations...");
        }
        return userConfirmation;
    }

    public void performFileOperations() throws IOException {
        LOGGER.info("Performing Real File Operations - Copying and Moving the Files...");

        DottedProgressPrinter progressPrinter = new DottedProgressPrinter();
        progressPrinter.setDotDataSize(configuration.getProgressPrinterDotDataSize());
        progressPrinter.setDotsPerLine(configuration.getProgressPrinterDotsPerLine());
        progressPrinter.reset(totalCopySize);

        long fileCopyChunkSize = configuration.getFileCopyChunkSize();

        for (FileOperation fileOperation : fileOperations) {
            fileOperation.setTemporaryFolder(destinationFolder);
            fileOperation.setCopyChunkSize(fileCopyChunkSize);
            fileOperation.setProgressPrinter(progressPrinter);
            fileOperation.perform();
        }
    }

    public void validateSnapshot() throws IOException, NoSuchAlgorithmException {
        LOGGER.info("Starting to Validate Snapshot...");

        missingFiles.clear();
        filesNotMatchedByMetadata.clear();
        filesToComputeChecksums.clear();
        filesNotMatchedByChecksum.clear();
        List<String> checksumAlgorithms = configuration.getChecksumAlgorithms();

        // Checking Snapshot against Destination Folder to be sure that all files has been delivered
        for (SnapshotFile fileInSnapshot : snapshot.getFilesMap().values()) {
            Path absoluteFilePath = destinationFolder.resolve(fileInSnapshot.getRelativePath());
            fileDatabase.updateEntryFromDisk(absoluteFilePath, false);
            FileMetadata fileInFileDatabase = fileDatabase.getFileMetadataByAbsolutePath(absoluteFilePath.toString());
            if (fileInFileDatabase == null) {
                missingFiles.add(fileInSnapshot);
                continue;
            }

            if (!fileInSnapshot.equalsByMetadata(fileInFileDatabase)) {
                filesNotMatchedByMetadata.add(new Pair<>(fileInSnapshot, fileInFileDatabase));
                continue;
            }

            if (!fileInFileDatabase.hasChecksums(checksumAlgorithms)) {
                filesToComputeChecksums.add(fileInFileDatabase);
                continue;
            }
        }

        LOGGER.info("Validate Snapshot - Starting to Compute Checksums...");
        ChecksumComputer checksumComputer = new ChecksumComputer(checksumAlgorithms);
        checksumComputer.reset();
        long computeChecksumSize = filesToComputeChecksums.stream()
                .mapToLong(m -> m.getSize())
                .sum();
        checksumComputer.setAllFilesSize(computeChecksumSize);
        // Computing Checksums
        for (FileMetadata fileMetadata : filesToComputeChecksums) {
            File file = new File(fileMetadata.getAbsolutePath());
            Map<String, String> checksums = checksumComputer.computeChecksums(file);
            fileMetadata.addChecksums(checksums);
        }

        // Comparing Checksums
        for (SnapshotFile fileInSnapshot : snapshot.getFilesMap().values()) {
            Path absoluteFilePath = destinationFolder.resolve(fileInSnapshot.getRelativePath());
            FileMetadata fileInFileDatabase = fileDatabase.getFileMetadataByAbsolutePath(absoluteFilePath.toString());
            if (!fileInSnapshot.equalsByChecksum(fileInFileDatabase)) {
                filesNotMatchedByChecksum.add(new Pair<>(fileInSnapshot, fileInFileDatabase));
                continue;
            }
        }

        // Checking Destination Folder against Snapshot to find old outdated files
        PathIterator destinationFolderIterator = new PathIterator(destinationFolder, true);
        while (destinationFolderIterator.hasNext()) {
            Path fileOnDisk = destinationFolderIterator.next();
            Path fileOnDiskRelativePath = destinationFolder.relativize(fileOnDisk);
            if (!snapshot.getFilesMap().containsKey(fileOnDiskRelativePath.toString())) {
                FileMetadata fileMetadata = new FileMetadata(fileOnDisk);
                redundantFilesInDestinationFolder.add(new Pair<>(null, fileMetadata));
            }
        }
    }

    public void cleanupDestinationFolder() throws IOException {
        LOGGER.info("Starting Destination Folder Cleanup...");
        cleanupDestinationFolder("Redundant", redundantFilesInDestinationFolder);
        cleanupDestinationFolder("MetadataMismatch", filesNotMatchedByMetadata);
        cleanupDestinationFolder("ChecksumMismatch", filesNotMatchedByChecksum);
    }

    private void cleanupDestinationFolder(String errorType, List<Pair<SnapshotFile, FileMetadata>> files) throws IOException {
        if (files.isEmpty()) {
            return;
        }

        long totalFileSizeToDelete = files.stream()
                .mapToLong(p -> p.getValue().getSize())
                .sum();

        StringBuilder fileListStringBuilder = new StringBuilder();
        for (Pair<SnapshotFile, FileMetadata> pair : files) {
            FileMetadata absoluteFileMetadata = pair.getValue();
            Path fileAbsolutePath = Paths.get(absoluteFileMetadata.getAbsolutePath());
            Path fileRelativePath = destinationFolder.relativize(fileAbsolutePath);
            fileListStringBuilder.append("\t" + fileRelativePath.toString() + ":\n");
            fileListStringBuilder.append("In Snapshot: ").append(pair.getKey() == null ? "null" : pair.getKey().toString()).append("\n");
            fileListStringBuilder.append("On Disk: ").append(pair.getValue().toString()).append("\n");
        }

        StringBuilder confirmationMessage = new StringBuilder();
        confirmationMessage.append("Destination Folder Cleanup, found " + errorType + " file(s):\n");
        confirmationMessage.append(fileListStringBuilder);
        confirmationMessage.append("Total File Size to Delete: ")
                .append(FormattingUtils.humanReadableSizeBi(totalFileSizeToDelete)).append("b\n");
        confirmationMessage.append("Actually, this should never happen, so investigation of this situation is recommended.").append("\n");
        confirmationMessage.append("Would you like to DELETE these files (yes/no)? ");

        LOGGER.info("Asking User for Confirmation to DELETE " + errorType + " file(s):\n" + fileListStringBuilder.toString());
        boolean deleteConfirmed = ConsoleUtils.userConfirm(confirmationMessage.toString());
        if (!deleteConfirmed) {
            return;
        }

        for (Pair<SnapshotFile, FileMetadata> pair : files) {
            FileMetadata absoluteFileMetadata = pair.getValue();
            Path fileAbsolutePath = Paths.get(absoluteFileMetadata.getAbsolutePath());
            LOGGER.info("Deleting " + errorType + " file: " + fileAbsolutePath.toString());
            PathUtils.deleteFile(fileAbsolutePath);
        }
    }

    public void reportSnapshotValidation() {
        LOGGER.info("Snapshot Validation is done");
        if (missingFiles.isEmpty()) {
            return;
        }

        LOGGER.warn("The following files are missing:\n" + missingFiles.stream()
                .map(f -> f.getRelativePath()).collect(Collectors.joining("\n")));
    }
}
