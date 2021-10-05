package backuper.client.config;

import java.util.List;

public class Configuration {
    private int localFileChunkSize;
    // TODO Change back to only one remoteFileChunkSize
    private int remoteFileMinChunkSize;
    private int remoteFileMaxChunkSize;
    private int maxConnectionsNumber;
    private List<BackupTask> backupTasks;

    public int getLocalFileChunkSize() {
        return localFileChunkSize;
    }

    public void setLocalFileChunkSize(int localFileChunkSize) {
        this.localFileChunkSize = localFileChunkSize;
    }

    public int getRemoteFileMinChunkSize() {
        return remoteFileMinChunkSize;
    }

    public void setRemoteFileMinChunkSize(int remoteFileMinChunkSize) {
        this.remoteFileMinChunkSize = remoteFileMinChunkSize;
    }

    public int getRemoteFileMaxChunkSize() {
        return remoteFileMaxChunkSize;
    }

    public void setRemoteFileMaxChunkSize(int remoteFileMaxChunkSize) {
        this.remoteFileMaxChunkSize = remoteFileMaxChunkSize;
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
