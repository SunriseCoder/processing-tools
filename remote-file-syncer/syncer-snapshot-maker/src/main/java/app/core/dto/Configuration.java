package app.core.dto;

public class Configuration {
    private String[] checksumAlgorithms;
    private long snapshotSaveMinimalIntervalInMS;

    public String[] getChecksumAlgorithms() {
        return checksumAlgorithms;
    }

    public long getSnapshotSaveMinimalIntervalInMS() {
        return snapshotSaveMinimalIntervalInMS;
    }
}
