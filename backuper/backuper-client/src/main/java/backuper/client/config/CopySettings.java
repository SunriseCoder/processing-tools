package backuper.client.config;

public class CopySettings {
    private int copyChunkSize;
    private int maxConnections;

    public int getCopyChunkSize() {
        return copyChunkSize;
    }

    public void setCopyChunkSize(int copyChunkSize) {
        this.copyChunkSize = copyChunkSize;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }
}
