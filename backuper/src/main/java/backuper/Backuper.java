package backuper;

import java.io.Console;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;
import java.util.stream.Stream;

import backuper.dto.BackupTask;
import backuper.dto.FileMetadata;
import backuper.helpers.FormattingHelper;
import backuper.helpers.PrintHelper;
import backuper.operations.CopyFileOperation;
import backuper.operations.CreateFolderOperation;
import backuper.operations.DeleteFileOperation;
import backuper.operations.DeleteFolderOperation;
import backuper.operations.Operation;
import backuper.operations.OperationsComparator;
import utils.NumberUtils;

public class Backuper {
    private FolderScanner folderScanner;

    public Backuper() {
        folderScanner = new FolderScanner();
    }

    public void doBackupTasks(List<BackupTask> tasks) throws IOException {
        List<Operation> operations = new ArrayList<>();
        long startTime, scanEndTime, copyStartTime, endTime;

        // Scanning all Backup Tasks
        startTime = System.currentTimeMillis();
        for (int i = 0; i < tasks.size(); i++) {
            BackupTask task = tasks.get(i);

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

        // New Files
        Supplier<Stream<Operation>> newFilesStream = () -> operations.stream()
                .filter(o -> (o instanceof CopyFileOperation) && ((CopyFileOperation)o).isNewFile());
        System.out.println("Copy new files: " + newFilesStream.get().count() +
                " (" + NumberUtils.humanReadableSize(newFilesStream.get().mapToLong(o -> o.getCopyFileSize()).sum()) + "b)");

        // Changed Files
        Supplier<Stream<Operation>> changedFilesStream = () -> operations.stream()
                .filter(o -> (o instanceof CopyFileOperation) && !((CopyFileOperation)o).isNewFile());
        System.out.println("Copy changed files: " + changedFilesStream.get().count()
                + " (" + NumberUtils.humanReadableSize(changedFilesStream.get().mapToLong(o -> o.getCopyFileSize()).sum())
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

        System.out.println("All tasks are done");
        System.out.println("Scan took " + FormattingHelper.humanReadableTime((scanEndTime - startTime) / 1000));
        System.out.println("Copying took " + FormattingHelper.humanReadableTime((endTime - copyStartTime) / 1000));
        System.out.println("Total: " + FormattingHelper.humanReadableTime((endTime - startTime) / 1000));
    }

    private List<Operation> scanTask(BackupTask task) throws IOException {
        List<Operation> operations = new ArrayList<>();

        System.out.println("Scanning source folder...");
        Map<String, FileMetadata> sourceFiles = folderScanner.scan(Paths.get(task.getSource()));
        System.out.println("Scanning destination folder...");
        Map<String, FileMetadata> destinationFiles = folderScanner.scan(Paths.get(task.getDestination()));

        // Scanning Source Folder against Destination Folder
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
                    operations.add(new CopyFileOperation(srcFileMetadata, task.getDestination(), dstFileMetadata == null));
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
