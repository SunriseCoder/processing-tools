package app.core.file.operations;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

import app.progress.ProgressPrinter;

public interface FileOperation {
    boolean isValid();
    long getCopyDataSize();
    long getFileSize();
    void setTemporaryFolder(Path temporaryFolder);
    void setCopyChunkSize(long copyChunkSize);
    void setLastModifiedTime(FileTime lastModifiedTime);
    void setProgressPrinter(ProgressPrinter progressPrinter);
    void perform() throws IOException;
}
