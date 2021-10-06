package backuper.client;

import java.io.Console;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.hc.core5.http.HttpException;

import backuper.client.config.BackupTask;
import backuper.client.config.Configuration;
import backuper.client.operations.CopyLocalFileOperation;
import backuper.client.operations.CopyRemoteFileOperation;
import backuper.client.operations.CopyRemoteFileOperation.CopyChunkTask;
import backuper.client.operations.CreateFolderOperation;
import backuper.client.operations.DeleteFileOperation;
import backuper.client.operations.DeleteFolderOperation;
import backuper.client.operations.Operation;
import backuper.common.LocalFolderScanner;
import backuper.common.dto.FileMetadata;
import backuper.common.helpers.PrintHelper;
import utils.FileUtils;
import utils.FormattingUtils;
import utils.MapUtils;
import utils.ThreadUtils;

public class Backuper {
    private Configuration config;

    private LocalFolderScanner localFolderScanner;
    private RemoteResourceScanner remoteResourceScanner;

    private List<Operation> deleteFileOperations;
    private List<Operation> deleteFolderOperations;
    private List<Operation> createFolderOperations;
    private List<CopyLocalFileOperation> copyLocalFileOperations;
    private Map<BackupTask, List<CopyRemoteFileOperation>> copyRemoteFileOperations;

    private long startTime, scanEndTime, copyStartTime, endTime;

    public Backuper(Configuration config) {
        this.config = config;

        localFolderScanner = new LocalFolderScanner();
        remoteResourceScanner = new RemoteResourceScanner();
    }

    public void doBackup() throws IOException, HttpException {
        reset();

        startTime = System.currentTimeMillis();

        // Scanning all Backup Tasks
        List<BackupTask> backupTasks = config.getBackupTasks();
        for (int i = 0; i < backupTasks.size(); i++) {
            BackupTask task = backupTasks.get(i);
            System.out.println("Scanning task #" + (i + 1) + ": " + task + "... ");
            scanTask(task);
        }

        long copyLocalFilesTotalSize = copyLocalFileOperations.stream().mapToLong(o -> o.getFileSize()).sum();
        long copyRemoteFilesTotalSize = copyRemoteFileOperations.values().stream()
                .flatMap(v -> v.stream()).mapToLong(o -> o.getFileSize()).sum();

        scanEndTime = System.currentTimeMillis();
        System.out.println("Scan took " + FormattingUtils.humanReadableTimeS((scanEndTime - startTime) / 1000));

        // Operations number
        long totalOperationsNumber = deleteFileOperations.size() + deleteFolderOperations.size()
                + createFolderOperations.size() + copyLocalFileOperations.size()
                + copyRemoteFileOperations.values().stream().flatMap(l -> l.stream()).count();
        System.out.println("Operations to perform: " + totalOperationsNumber + " operation(s)");

        // New Folders
        System.out.println("New folders: " + createFolderOperations.size());

        // New Local Files
        Supplier<Stream<CopyLocalFileOperation>> newLocalFilesStream = () -> copyLocalFileOperations.stream().filter(o -> o.isNewFile());
        System.out.println("Copy new local files: " + newLocalFilesStream.get().count() +
                " (" + FormattingUtils.humanReadableSize(newLocalFilesStream.get().mapToLong(o -> o.getFileSize()).sum()) + "b)");
        // Changed Local Files
        Supplier<Stream<CopyLocalFileOperation>> changedLocalFilesStream = () -> copyLocalFileOperations.stream().filter(o -> !o.isNewFile());
        System.out.println("Copy changed local files: " + changedLocalFilesStream.get().count()
                + " (" + FormattingUtils.humanReadableSize(changedLocalFilesStream.get().mapToLong(o -> o.getFileSize()).sum())
                + "b) <== Attention here if the size is too big!!!");

        // New Remote Files
        Supplier<Stream<CopyRemoteFileOperation>> newRemoteFilesStream =
                () -> copyRemoteFileOperations.values().stream()
                        .flatMap(v -> v.stream())
                        .filter(o -> o.isNewFile());
        System.out.println("Copy new remote files: " + newRemoteFilesStream.get().count() +
                " (" + FormattingUtils.humanReadableSize(newRemoteFilesStream.get().mapToLong(o -> o.getFileSize()).sum()) + "b)");
        // Changed Remote Files
        Supplier<Stream<CopyRemoteFileOperation>> changedRemoteFilesStream =
                () -> copyRemoteFileOperations.values().stream()
                        .flatMap(v -> v.stream())
                        .filter(o -> !o.isNewFile());
        System.out.println("Copy changed remote files: " + changedRemoteFilesStream.get().count()
                + " (" + FormattingUtils.humanReadableSize(changedRemoteFilesStream.get().mapToLong(o -> o.getFileSize()).sum())
                + "b) <== Attention here if the size is too big!!!");

        // Folders to delete
        System.out.println("Folders to delete: " + deleteFolderOperations.size());

        // Files to delete
        System.out.println("Files to delete: " + deleteFileOperations.size()
                + " (" + FormattingUtils.humanReadableSize(deleteFileOperations.stream().mapToLong(o -> o.getFileSize()).sum())
                + "b) <== Attention here if the size is too big!!!");

        long copyFileSizeTotal = copyLocalFilesTotalSize + copyRemoteFilesTotalSize;
        System.out.println("Total size to copy: " + FormattingUtils.humanReadableSize(copyFileSizeTotal) + "b");

        if (totalOperationsNumber == 0) {
            System.out.println("It seems that all of the files are up to date");
            System.exit(0);
        }
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

        // Copy Files
        FileCopyStatus fileCopyStatus = new FileCopyStatus();
        copyLocalFiles(copyLocalFilesTotalSize, fileCopyStatus);
        copyRemoteFiles(copyRemoteFilesTotalSize, fileCopyStatus);

        endTime = System.currentTimeMillis();

        fileCopyStatus.printCopyResults();
        System.out.println("All tasks are done");
        System.out.println("Scan took " + FormattingUtils.humanReadableTimeS((scanEndTime - startTime) / 1000));
        System.out.println("Copying took " + FormattingUtils.humanReadableTimeS((endTime - copyStartTime) / 1000));
        System.out.println("Total: " + FormattingUtils.humanReadableTimeS((endTime - startTime) / 1000));
    }

    private void reset() {
        deleteFileOperations = new ArrayList<>();
        deleteFolderOperations = new ArrayList<>();
        createFolderOperations = new ArrayList<>();
        copyLocalFileOperations = new ArrayList<>();
        copyRemoteFileOperations = new LinkedHashMap<>();
    }

    private void scanTask(BackupTask backupTask) throws IOException, HttpException {
        FileUtils.createFolderIfNotExists(backupTask.getDestination());
        FileUtils.createFolderIfNotExists(backupTask.getTmp());
        FileUtils.cleanupFolder(backupTask.getTmp());

        System.out.println("Scanning source folder...");
        Map<String, FileMetadata> sourceFiles = scanResource(backupTask.getSource());
        System.out.println("Scanning destination folder...");
        Map<String, FileMetadata> destinationFiles = scanResource(backupTask.getDestination());

        // Scanning Source Folder against Destination Folder
        for (Entry<String, FileMetadata> sourceFileEntry : sourceFiles.entrySet()) {
            String srcFileRelPath = sourceFileEntry.getKey();
            FileMetadata srcFileMetadata = sourceFileEntry.getValue();
            FileMetadata dstFileMetadata = destinationFiles.get(srcFileRelPath);

            if (srcFileMetadata.isDirectory()) {
                // Processing Folder operation
                if (dstFileMetadata == null) {
                    createFolderOperations.add(new CreateFolderOperation(srcFileMetadata, backupTask.getDestination()));
                }
            } else {
                // Processing File operation
                if (dstFileMetadata == null || !srcFileMetadata.equalsRelatively(dstFileMetadata)) {
                    if (srcFileMetadata.isRemote()) {
                        CopyRemoteFileOperation copyRemoteFileOperation = new CopyRemoteFileOperation(backupTask, srcFileMetadata, backupTask.getDestination(), dstFileMetadata == null);
                        MapUtils.addToListValue(copyRemoteFileOperations, backupTask, copyRemoteFileOperation);
                    } else {
                        copyLocalFileOperations.add(new CopyLocalFileOperation(backupTask, srcFileMetadata, backupTask.getDestination(), dstFileMetadata == null));
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

    private void copyLocalFiles(long copyLocalFilesTotalSize, FileCopyStatus fileCopyStatus) throws IOException, HttpException {
        fileCopyStatus.reset();
        fileCopyStatus.setAllFilesTotalSize(copyLocalFilesTotalSize);
        for (int i = 0; i < copyLocalFileOperations.size(); i++) {
            CopyLocalFileOperation operation = copyLocalFileOperations.get(i);
            System.out.println((i + 1) + "/" + copyLocalFileOperations.size() + ": " + operation.getDescription());
            operation.perform(fileCopyStatus);
        }
    }

    private void copyRemoteFiles(long copyRemoteFilesTotalSize, FileCopyStatus fileCopyStatus) throws IOException, HttpException {
        // FileCopyStatus Reset
        fileCopyStatus.reset();
        fileCopyStatus.setCurrentFileTotalSize(copyRemoteFilesTotalSize);
        fileCopyStatus.setAllFilesTotalSize(copyRemoteFilesTotalSize);

        for (Entry<BackupTask, List<CopyRemoteFileOperation>> copyRemoteFileOperationEntry : copyRemoteFileOperations.entrySet()) {
            BackupTask backupTask = copyRemoteFileOperationEntry.getKey();
            List<CopyRemoteFileOperation> operationsForCurrentTask = copyRemoteFileOperationEntry.getValue();
            System.out.println("Starting remote files copy for task: " + backupTask.getName());

            // Preparing Executor
            int maxConnections = backupTask.getCopySettings().getMaxConnections();
            int maxFuturesNumber = maxConnections * 2;
            ExecutorService executor = Executors.newFixedThreadPool(maxConnections);

            CopyRemoteFileOperation currentOperation = null;
            OperationFutures currentOperationFutures = null;
            Iterator<CopyRemoteFileOperation> operationIterator = operationsForCurrentTask.iterator();
            List<OperationFutures> operationFuturesList = new ArrayList<>();
            // Main Loop of Copy Remote Files
            while (operationIterator.hasNext() || operationFuturesList.size() > 0) {
                // Checking and cleaning up finished tasks
                int futuresCounter = 0;
                Iterator<OperationFutures> operationFuturesIterator = operationFuturesList.iterator();
                while (operationFuturesIterator.hasNext()) {
                    OperationFutures operationFutures = operationFuturesIterator.next();
                    Iterator<Future<?>> futuresIterator = operationFutures.getFutures().iterator();
                    while(futuresIterator.hasNext()) {
                        Future<?> future = futuresIterator.next();
                        if (future.isDone()) {
                            futuresIterator.remove();
                        } else {
                            futuresCounter++;
                        }
                    }
                    // It's possible that all the task could be executed faster than this main loop. So therefore finish file in following cases only:
                    // No active tasks for current file left && (next file is already started || this is the last file)
                    if (operationFutures.getFutures().size() == 0 && (operationFutures.getOperation() != currentOperation || !operationIterator.hasNext())) {
                        operationFutures.getOperation().finish();
                        operationFuturesIterator.remove();
                    }
                }

                // Creating new futures
                for (int i = futuresCounter; i < maxFuturesNumber; i++) {
                    // First operation
                    if (currentOperation == null) {
                        currentOperation = operationIterator.next();
                        currentOperation.prepare(fileCopyStatus);
                        currentOperationFutures = new OperationFutures(currentOperation);
                        operationFuturesList.add(currentOperationFutures);
                    }

                    // Next operation
                    if (!currentOperation.hasNextChunk()) {
                        while (operationIterator.hasNext()) {
                            currentOperation = operationIterator.next();
                            currentOperation.prepare(fileCopyStatus);
                            if (currentOperation.hasNextChunk()) {
                                currentOperationFutures = new OperationFutures(currentOperation);
                                operationFuturesList.add(currentOperationFutures);
                                break;
                            } else {
                                currentOperation.finish();
                            }
                        }
                    }

                    // Getting creating future
                    if (currentOperation.hasNextChunk()) {
                        CopyChunkTask copyChunkTask = currentOperation.getNextChunk();
                        Future<?> future = executor.submit(copyChunkTask);
                        currentOperationFutures.getFutures().add(future);
                    }
                }

                if (operationFuturesList.size() > 0) {
                    ThreadUtils.sleep(10);
                }
                fileCopyStatus.printCopyProgress(false);
            }

            fileCopyStatus.printCopyProgress(true);
            PrintHelper.println();
            executor.shutdown();
            System.out.println("Copy remote files for task: " + backupTask.getName() + " is done");
        }
    }

    private static class OperationFutures {
        private CopyRemoteFileOperation operation;
        private List<Future<?>> futures;

        public OperationFutures(CopyRemoteFileOperation operation) {
            this.operation = operation;
            futures = new ArrayList<>();
        }

        public CopyRemoteFileOperation getOperation() {
            return operation;
        }

        public List<Future<?>> getFutures() {
            return futures;
        }
    }
}
