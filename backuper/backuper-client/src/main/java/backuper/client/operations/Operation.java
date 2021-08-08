package backuper.client.operations;

import java.io.IOException;

import backuper.client.FileCopyStatus;

public interface Operation {
    String getDescription();
    long getCopyFileSize();
    String getRelativePath();

    void perform(FileCopyStatus fileCopyStatus) throws IOException;
}
