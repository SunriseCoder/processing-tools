package backuper.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import backuper.client.config.BackupTask;
import backuper.client.operations.CopyRemoteFileOperation;
import backuper.client.operations.CopyRemoteFileOperation.CopyChunkTask;
import backuper.common.helpers.PrintHelper;
import utils.ThreadUtils;

public class RemoteFilesCopier {
    private Map<BackupTask, List<CopyRemoteFileOperation>> copyOperations;
    private FileCopyStatus fileCopyStatus;

    private long totalFilesSize;

    private Iterator<CopyRemoteFileOperation> operationsIterator;
    private List<OperationFutures> allFutures;
    private CopyRemoteFileOperation currentOperation;
    private OperationFutures currentOperationFutures;

    public RemoteFilesCopier() {
        copyOperations = new LinkedHashMap<>();
    }

    public long getTotalSize() {
        return totalFilesSize;
    }

    public void setCopyOperations(Map<BackupTask, List<CopyRemoteFileOperation>> copyOperations) {
        this.copyOperations = copyOperations;
        totalFilesSize = copyOperations.values().stream()
                .flatMap(v -> v.stream()).mapToLong(o -> o.getFileSize()).sum();
    }

    public void setFileCopyStatus(FileCopyStatus fileCopyStatus) {
        this.fileCopyStatus = fileCopyStatus;
    }

    public void copy() throws IOException {
        fileCopyStatus.reset();
        fileCopyStatus.setAllFilesTotalSize(totalFilesSize);

        for (Entry<BackupTask, List<CopyRemoteFileOperation>> copyRemoteFileOperationsEntry : copyOperations.entrySet()) {
            BackupTask backupTask = copyRemoteFileOperationsEntry.getKey();
            List<CopyRemoteFileOperation> operations = copyRemoteFileOperationsEntry.getValue();

            System.out.println("Starting remote files copy for task: " + backupTask.getName());

            copyBackupTaskFiles(backupTask, operations);

            System.out.println("Copy remote files for task: " + backupTask.getName() + " is done");
        }
    }

    private void copyBackupTaskFiles(BackupTask backupTask, List<CopyRemoteFileOperation> operations) throws IOException {
        // Initializing FileCopyStatus
        long backupTaskFileSize = operations.stream().mapToLong(o -> o.getFileSize()).sum();
        fileCopyStatus.setCurrentFileTotalSize(backupTaskFileSize);

        // Initializing Operations
        allFutures = new ArrayList<>();
        operationsIterator = operations.iterator();
        if (!startNextOperation()) {
            return;
        }

        // Preparing Executor
        int maxConnections = backupTask.getCopySettings().getMaxConnections();
        int maxFuturesNumber = maxConnections * 2;
        ExecutorService executor = Executors.newFixedThreadPool(maxConnections);

        // Main Loop of Copy Remote Files
        while (operationsIterator.hasNext() || !allFutures.isEmpty()) {
            int futuresCounter = cleanupFinishedTasks();
            submitMoreTasks(executor, maxFuturesNumber - futuresCounter);

            if (!allFutures.isEmpty()) {
                ThreadUtils.sleep(10);
            }

            fileCopyStatus.printCopyProgress(false);
        }

        fileCopyStatus.printCopyProgress(true);
        PrintHelper.println();
        executor.shutdown();
    }

    private boolean startNextOperation() {
        boolean result = operationsIterator.hasNext();

        if (result) {
            currentOperation = operationsIterator.next();
            currentOperation.prepare(fileCopyStatus);
            currentOperationFutures = new OperationFutures(currentOperation);
            allFutures.add(currentOperationFutures);
        }

        return result;
    }

    /**
     *
     * @return amount of incomplete Futures
     */
    private int cleanupFinishedTasks() throws IOException {
        int futuresCounter = 0;

        Iterator<OperationFutures> operationsIterator = allFutures.iterator();
        while (operationsIterator.hasNext()) {
            OperationFutures operationFutures = operationsIterator.next();
            Iterator<Future<?>> futuresIterator = operationFutures.getFutures().iterator();
            while(futuresIterator.hasNext()) {
                Future<?> future = futuresIterator.next();
                if (future.isDone()) {
                    futuresIterator.remove();
                } else {
                    futuresCounter++;
                }
            }

            // Finishing CopyRemoteFileOperation if:
            // 1. No active copy tasks
            // 2. We started to copy next file already OR there is no more Chunks for the current file
            if (operationFutures.getFutures().isEmpty()) {
                if (operationFutures.getOperation() != currentOperation || !currentOperation.hasNextChunk()) {
                    operationFutures.getOperation().finish();
                    operationsIterator.remove();
                }
            }
        }

        return futuresCounter;
    }

    private void submitMoreTasks(ExecutorService executor, int futuresNumber) throws IOException {
        for (int i = 0; i < futuresNumber; i++) {
            if (!currentOperation.hasNextChunk()) {
                // Looking for non-zero-length files and finishing zero-length files without copy operations
                while (startNextOperation()) {
                    if (currentOperation.hasNextChunk()) {
                        break;
                    } else {
                        currentOperation.finish();
                    }
                }
            }

            // Creating and Submitting copy task
            if (currentOperation.hasNextChunk()) {
                CopyChunkTask copyChunkTask = currentOperation.createNextCopyChunkTask();
                Future<?> future = executor.submit(copyChunkTask);
                currentOperationFutures.getFutures().add(future);
            }
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
