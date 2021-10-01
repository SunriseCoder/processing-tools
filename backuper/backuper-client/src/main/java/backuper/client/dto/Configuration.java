package backuper.client.dto;

import java.util.List;

public class Configuration {
    private int localFileChunkSize;
    private int remoteFileChunkSize;
    private int maxConnectionsNumber;
    private List<BackupTask> backupTasks;

    public int getLocalFileChunkSize() {
        return localFileChunkSize;
    }

    public void setLocalFileChunkSize(int localFileChunkSize) {
        this.localFileChunkSize = localFileChunkSize;
    }

    public int getRemoteFileChunkSize() {
        return remoteFileChunkSize;
    }

    public void setRemoteFileChunkSize(int remoteFileChunkSize) {
        this.remoteFileChunkSize = remoteFileChunkSize;
    }

    public int getMaxConnectionsNumber() {
        return maxConnectionsNumber;
    }

    public void setMaxConnectionsNumber(int maxConnectionsNumber) {
        this.maxConnectionsNumber = maxConnectionsNumber;
    }

    public List<BackupTask> getBackupTasks() {
        return backupTasks;
    }

    public void setBackupTasks(List<BackupTask> backupTasks) {
        this.backupTasks = backupTasks;
    }
}
