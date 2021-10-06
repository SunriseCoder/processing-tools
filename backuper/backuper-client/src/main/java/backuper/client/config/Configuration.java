package backuper.client.config;

import java.util.List;
import java.util.Map;

public class Configuration {
    private CopySettings localCopySettings;
    Map<String, CopySettings> remoteCopySettings;
    private List<BackupTask> backupTasks;

    public CopySettings getLocalCopySettings() {
        return localCopySettings;
    }

    public void setLocalCopySettings(CopySettings localCopySettings) {
        this.localCopySettings = localCopySettings;
    }

    public Map<String, CopySettings> getRemoteCopySettings() {
        return remoteCopySettings;
    }

    public void setRemoteCopySettings(Map<String, CopySettings> remoteCopySettings) {
        this.remoteCopySettings = remoteCopySettings;
    }

    public List<BackupTask> getBackupTasks() {
        return backupTasks;
    }

    public void setBackupTasks(List<BackupTask> backupTasks) {
        this.backupTasks = backupTasks;
    }
}
