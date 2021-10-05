package backuper.client;

import java.io.Console;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.hc.core5.http.HttpException;

import backuper.client.config.BackupTask;
import backuper.client.config.Configuration;
import backuper.client.operations.CopyLocalFileOperation;
import backuper.client.operations.CopyRemoteFileOperation;
import backuper.client.operations.CreateFolderOperation;
import backuper.client.operations.DeleteFileOperation;
import backuper.client.operations.DeleteFolderOperation;
import backuper.client.operations.Operation;
import backuper.common.LocalFolderScanner;
import backuper.common.dto.FileMetadata;
import backuper.common.helpers.FormattingHelper;
import backuper.common.helpers.PrintHelper;
import utils.NumberUtils;

public class Backuper {
    private Configuration configuration;

    private LocalFolderScanner localFolderScanner;
    private RemoteResourceScanner remoteResourceScanner;

    private List<Operation> deleteFileOperations;
    private List<Operation> deleteFolderOperations;
    private List<Operation> createFolderOperations;
    private List<CopyLocalFileOperation> copyLocalFileOperations;
    private List<CopyRemoteFileOperation> copyRemoteFileOperations;

    private long startTime, scanEndTime, copyStartTime, endTime;

    public Backuper(Configuration configuration) {
        this.configuration = configuration;

        localFolderScanner = new LocalFolderScanner();
        remoteResourceScanner = new RemoteResourceScanner();
    }

    public void doBackup() throws IOException, HttpException {
        reset();

        startTime = System.currentTimeMillis();

        // Scanning all Backup Tasks
        List<BackupTask> backupTasks = configuration.getBackupTasks();
        for (int i = 0; i < backupTasks.size(); i++) {
            BackupTask task = backupTasks.get(i);
            System.out.println("Scanning task #" + (i + 1) + ": " + task + "... ");
            scanTask(task);
        }

        long copyLocalFilesTotalSize = copyLocalFileOperations.stream().mapToLong(o -> o.getFileSize()).sum();
        long copyRemoteFilesTotalSize = copyRemoteFileOperations.stream().mapToLong(o -> o.getFileSize()).sum();

        scanEndTime = System.currentTimeMillis();
        System.out.println("Scan took " + FormattingHelper.humanReadableTime((scanEndTime - startTime) / 1000));

        // Operations number
        int totalOperationsNumber = deleteFileOperations.size() + deleteFolderOperations.size()
                + createFolderOperations.size() + copyLocalFileOperations.size() + copyRemoteFileOperations.size();
        System.out.println("Operations to perform: " + totalOperationsNumber + " operation(s)");

        // New Folders
        System.out.println("New folders: " + createFolderOperations.size());

        // New Local Files
        Supplier<Stream<CopyLocalFileOperation>> newLocalFilesStream = () -> copyLocalFileOperations.stream().filter(o -> o.isNewFile());
        System.out.println("Copy new local files: " + newLocalFilesStream.get().count() +
                " (" + NumberUtils.humanReadableSize(newLocalFilesStream.get().mapToLong(o -> o.getFileSize()).sum()) + "b)");
        // Changed Local Files
        Supplier<Stream<CopyLocalFileOperation>> changedLocalFilesStream = () -> copyLocalFileOperations.stream().filter(o -> !o.isNewFile());
        System.out.println("Copy changed local files: " + changedLocalFilesStream.get().count()
                + " (" + NumberUtils.humanReadableSize(changedLocalFilesStream.get().mapToLong(o -> o.getFileSize()).sum())
                + "b) <== Attention here if the size is too big!!!");

        // New Remote Files
        Supplier<Stream<CopyRemoteFileOperation>> newRemoteFilesStream = () -> copyRemoteFileOperations.stream().filter(o -> o.isNewFile());
        System.out.println("Copy new remote files: " + newRemoteFilesStream.get().count() +
                " (" + NumberUtils.humanReadableSize(newRemoteFilesStream.get().mapToLong(o -> o.getFileSize()).sum()) + "b)");
        // Changed Remote Files
        Supplier<Stream<CopyRemoteFileOperation>> changedRemoteFilesStream = () -> copyRemoteFileOperations.stream().filter(o -> !o.isNewFile());
        System.out.println("Copy changed remote files: " + changedRemoteFilesStream.get().count()
                + " (" + NumberUtils.humanReadableSize(changedRemoteFilesStream.get().mapToLong(o -> o.getFileSize()).sum())
                + "b) <== Attention here if the size is too big!!!");

        // Folders to delete
        System.out.println("Folders to delete: " + deleteFolderOperations.size());

        // Files to delete
        System.out.println("Files to delete: " + deleteFileOperations.size()
                + " (" + NumberUtils.humanReadableSize(deleteFileOperations.stream().mapToLong(o -> o.getFileSize()).sum())
                + "b) <== Attention here if the size is too big!!!");

        long copyFileSizeTotal = copyLocalFilesTotalSize + copyRemoteFilesTotalSize;
        System.out.println("Total size to copy: " + NumberUtils.humanReadableSize(copyFileSizeTotal) + "b");

        if (!confirmOperations()) {
            System.out.println("Backup cancelled by user");
            System.exit(-1);
        }

        // Starting actual Backup Operations
        copyStartTime = System.currentTimeMillis();

        // Performing Simple Operations
        performSimpleOperations("Deleting files...", deleteFileOperations);
        performSimpleOperations("Deleting folders...", deleteFolderOperations);
        performSimpleOperations("Create folders...", createFolderOperations);

        // Copy Local Files
        FileCopyStatus fileCopyStatus = new FileCopyStatus();
        fileCopyStatus.reset();
        fileCopyStatus.setAllFilesTotalSize(copyLocalFilesTotalSize);
        for (int i = 0; i < copyLocalFileOperations.size(); i++) {
            CopyLocalFileOperation operation = copyLocalFileOperations.get(i);
            System.out.println((i + 1) + "/" + copyLocalFileOperations.size() + ": " + operation.getDescription());
            operation.perform(fileCopyStatus);
        }

        // Copy Remote Files
        fileCopyStatus.reset();
        fileCopyStatus.setAllFilesTotalSize(copyRemoteFilesTotalSize);
        // TODO Replace the main loop with loops of each of the operations. Maybe extract to a separate methods
        for (int i = 0; i < copyRemoteFileOperations.size(); i++) {
            CopyRemoteFileOperation operation = copyRemoteFileOperations.get(i);
            System.out.println((i + 1) + "/" + copyRemoteFileOperations.size() + ": " + operation.getDescription());
            operation.perform(fileCopyStatus);
        }

        endTime = System.currentTimeMillis();

        fileCopyStatus.printCopyResults();
        System.out.println("All tasks are done");
        System.out.println("Scan took " + FormattingHelper.humanReadableTime((scanEndTime - startTime) / 1000));
        System.out.println("Copying took " + FormattingHelper.humanReadableTime((endTime - copyStartTime) / 1000));
        System.out.println("Total: " + FormattingHelper.humanReadableTime((endTime - startTime) / 1000));
    }

    private void reset() {
        deleteFileOperations = new ArrayList<>();
        deleteFolderOperations = new ArrayList<>();
        createFolderOperations = new ArrayList<>();
        copyLocalFileOperations = new ArrayList<>();
        copyRemoteFileOperations = new ArrayList<>();
    }

    private void scanTask(BackupTask task) throws IOException, HttpException {
        Path destinationPath = Paths.get(task.getDestination());
        if (Files.notExists(destinationPath)) {
            Files.createDirectories(destinationPath);
        }

        System.out.println("Scanning source folder...");
        Map<String, FileMetadata> sourceFiles = scanResource(task.getSource());
        System.out.println("Scanning destination folder...");
        Map<String, FileMetadata> destinationFiles = scanResource(task.getDestination());

        // Scanning Source Folder against Destination Folder
        for (Entry<String, FileMetadata> sourceFileEntry : sourceFiles.entrySet()) {
            String srcFileRelPath = sourceFileEntry.getKey();
            FileMetadata srcFileMetadata = sourceFileEntry.getValue();
            FileMetadata dstFileMetadata = destinationFiles.get(srcFileRelPath);

            if (srcFileMetadata.isDirectory()) {
                // Processing Folder operation
                if (dstFileMetadata == null) {
                    createFolderOperations.add(new CreateFolderOperation(srcFileMetadata, task.getDestination()));
                }
            } else {
                // Processing File operation
                if (dstFileMetadata == null || !srcFileMetadata.equalsRelatively(dstFileMetadata)) {
                    if (srcFileMetadata.isRemote()) {
                        copyRemoteFileOperations.add(new CopyRemoteFileOperation(configuration, srcFileMetadata, task.getDestination(), dstFileMetadata == null));
                    } else {
                        copyLocalFileOperations.add(new CopyLocalFileOperation(configuration, srcFileMetadata, task.getDestination(), dstFileMetadata == null));
                    }
                }
            }
        }

        // Scanning Destination Folder against Source Folder
        for (Entry<String, FileMetadata> destinationFileEntry : destinationFiles.entrySet()) {
            String dstFileRelPath = destinationFileEntry.getKey();
            FileMetadata dstFileMetadata = destinationFileEntry.getValue();
            FileMetadata srcFileMetadata = sourceFiles.get(dstFileRelPath);

            if (srcFileMetadata == null) {
                if (dstFileMetadata.isDirectory()) {
                    // TODO Test the case if removed folder is not empty.
                    // Will it be processed carefully, or add all the content of the folder to remove at this place?
                    deleteFolderOperations.add(new DeleteFolderOperation(dstFileMetadata));
                } else {
                    deleteFileOperations.add(new DeleteFileOperation(dstFileMetadata));
                }
            }
        }
    }

    private Map<String, FileMetadata> scanResource(String resource) throws IOException, HttpException {
        Map<String, FileMetadata> files = resource.startsWith("http://") || resource.startsWith("https://") ?
                remoteResourceScanner.scan(resource) : localFolderScanner.scan(Paths.get(resource));
        return files;
    }

    private boolean confirmOperations() {
        Console console = System.console();
        if (console == null) {
            return false; // If console does not support by environment, do not asking for confirmation
        }

        PrintHelper.println("Please type \"yes\" to start backup");
        String input = console.readLine();
        boolean result = "yes".equals(input);
        return result;
    }

    private void performSimpleOperations(String messsage, List<Operation> operations) throws IOException {
        System.out.println();
        for (int i = 0; i < operations.size(); i++) {
            Operation operation = operations.get(i);
            System.out.println((i + 1) + "/" + operations.size() + ": " + operation.getDescription());
            operation.perform();
        }
    }
}
