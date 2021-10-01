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
import backuper.client.operations.OperationsComparator;
import backuper.common.LocalFolderScanner;
import backuper.common.dto.FileMetadata;
import backuper.common.helpers.FormattingHelper;
import backuper.common.helpers.PrintHelper;
import utils.NumberUtils;

public class Backuper {
    private Configuration configuration;

    private LocalFolderScanner localFolderScanner;
    private RemoteResourceScanner remoteResourceScanner;

    public Backuper(Configuration configuration) {
        this.configuration = configuration;

        localFolderScanner = new LocalFolderScanner();
        remoteResourceScanner = new RemoteResourceScanner();
    }

    public void doBackup() throws IOException, HttpException {
        List<BackupTask> backupTasks = configuration.getBackupTasks();
        List<Operation> operations = new ArrayList<>();
        long startTime, scanEndTime, copyStartTime, endTime;

        // Scanning all Backup Tasks
        startTime = System.currentTimeMillis();
        for (int i = 0; i < backupTasks.size(); i++) {
            BackupTask task = backupTasks.get(i);

            System.out.println("Scanning task #" + (i + 1) + ": " + task + "... ");
            List<Operation> taskOperations = scanTask(task);
            operations.addAll(taskOperations);
        }

        // Sorting Folder deletion order
        operations.sort(new OperationsComparator());
        scanEndTime = System.currentTimeMillis();
        System.out.println("Scan took " + FormattingHelper.humanReadableTime((scanEndTime - startTime) / 1000));

        // Operations number
        System.out.println("Operations to perform: " + operations.size() + " operation(s)");

        // New Folders
        System.out.println("New folders: " + operations.stream().filter(o -> (o instanceof CreateFolderOperation)).count());

        // New Local Files
        Supplier<Stream<Operation>> newLocalFilesStream = () -> operations.stream()
                .filter(o -> (o instanceof CopyLocalFileOperation) && ((CopyLocalFileOperation)o).isNewFile());
        System.out.println("Copy new local files: " + newLocalFilesStream.get().count() +
                " (" + NumberUtils.humanReadableSize(newLocalFilesStream.get().mapToLong(o -> o.getCopyFileSize()).sum()) + "b)");
        // Changed Local Files
        Supplier<Stream<Operation>> changedLocalFilesStream = () -> operations.stream()
                .filter(o -> (o instanceof CopyLocalFileOperation) && !((CopyLocalFileOperation)o).isNewFile());
        System.out.println("Copy changed local files: " + changedLocalFilesStream.get().count()
                + " (" + NumberUtils.humanReadableSize(changedLocalFilesStream.get().mapToLong(o -> o.getCopyFileSize()).sum())
                + "b) <== Attention here if the size is too big!!!");

        // New Remote Files
        Supplier<Stream<Operation>> newRemoteFilesStream = () -> operations.stream()
                .filter(o -> (o instanceof CopyRemoteFileOperation) && ((CopyRemoteFileOperation)o).isNewFile());
        System.out.println("Copy new remote files: " + newRemoteFilesStream.get().count() +
                " (" + NumberUtils.humanReadableSize(newRemoteFilesStream.get().mapToLong(o -> o.getCopyFileSize()).sum()) + "b)");
        // Changed Remote Files
        Supplier<Stream<Operation>> changedRemoteFilesStream = () -> operations.stream()
                .filter(o -> (o instanceof CopyRemoteFileOperation) && !((CopyRemoteFileOperation)o).isNewFile());
        System.out.println("Copy changed remote files: " + changedRemoteFilesStream.get().count()
                + " (" + NumberUtils.humanReadableSize(changedRemoteFilesStream.get().mapToLong(o -> o.getCopyFileSize()).sum())
                + "b) <== Attention here if the size is too big!!!");

        // Folders to delete
        System.out.println("Folders to delete: " + operations.stream().filter(o -> (o instanceof DeleteFolderOperation)).count());

        // Files to delete
        Supplier<Stream<Operation>> deletedFilesStream = () -> operations.stream()
                .filter(o -> (o instanceof DeleteFileOperation));
        System.out.println("Files to delete: " + deletedFilesStream.get().count()
                + " (" + NumberUtils.humanReadableSize(deletedFilesStream.get().mapToLong(o -> o.getCopyFileSize()).sum())
                + "b) <== Attention here if the size is too big!!!");

        long copyFileSizeTotal = operations.stream()
                .mapToLong(o -> o.getCopyFileSize()).sum();
        System.out.println("Total size to copy: " + NumberUtils.humanReadableSize(copyFileSizeTotal) + "b");

        if (!confirmOperations()) {
            System.out.println("Backup cancelled by user");
            System.exit(-1);
        }

        // Starting actual File Copying
        FileCopyStatus fileCopyStatus = new FileCopyStatus();
        fileCopyStatus.reset();
        fileCopyStatus.setAllFilesTotalSize(copyFileSizeTotal);

        long copiedFileSize = 0;
        copyStartTime = System.currentTimeMillis();
        for (int i = 0; i < operations.size(); i++) {
            Operation operation = operations.get(i);
            System.out.println((i + 1) + "/" + operations.size()
                    + " (" + NumberUtils.humanReadableSize(copiedFileSize) + "b/" + NumberUtils.humanReadableSize(copyFileSizeTotal) + "b): "
                    + operation.getDescription());
            operation.perform(fileCopyStatus);
            copiedFileSize += operation.getCopyFileSize();
        }
        endTime = System.currentTimeMillis();

        fileCopyStatus.printCopyResults();
        System.out.println("All tasks are done");
        System.out.println("Scan took " + FormattingHelper.humanReadableTime((scanEndTime - startTime) / 1000));
        System.out.println("Copying took " + FormattingHelper.humanReadableTime((endTime - copyStartTime) / 1000));
        System.out.println("Total: " + FormattingHelper.humanReadableTime((endTime - startTime) / 1000));
    }

    private List<Operation> scanTask(BackupTask task) throws IOException, HttpException {
        Path destinationPath = Paths.get(task.getDestination());
        if (Files.notExists(destinationPath)) {
            Files.createDirectories(destinationPath);
        }

        System.out.println("Scanning source folder...");
        Map<String, FileMetadata> sourceFiles = scanResource(task.getSource());
        System.out.println("Scanning destination folder...");
        Map<String, FileMetadata> destinationFiles = scanResource(task.getDestination());

        // Scanning Source Folder against Destination Folder
        List<Operation> operations = new ArrayList<>();
        for (Entry<String, FileMetadata> sourceFileEntry : sourceFiles.entrySet()) {
            String srcFileRelPath = sourceFileEntry.getKey();
            FileMetadata srcFileMetadata = sourceFileEntry.getValue();
            FileMetadata dstFileMetadata = destinationFiles.get(srcFileRelPath);

            if (srcFileMetadata.isDirectory()) {
                // Processing Folder operation
                if (dstFileMetadata == null) {
                    operations.add(new CreateFolderOperation(srcFileMetadata, task.getDestination()));
                }
            } else {
                // Processing File operation
                if (dstFileMetadata == null || !srcFileMetadata.equalsRelatively(dstFileMetadata)) {
                    if (srcFileMetadata.isRemote()) {
                        operations.add(new CopyRemoteFileOperation(configuration, srcFileMetadata, task.getDestination(), dstFileMetadata == null));
                    } else {
                        operations.add(new CopyLocalFileOperation(configuration, srcFileMetadata, task.getDestination(), dstFileMetadata == null));
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
                    operations.add(new DeleteFolderOperation(dstFileMetadata));
                } else {
                    operations.add(new DeleteFileOperation(dstFileMetadata));
                }
            }
        }

        return operations;
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
}
