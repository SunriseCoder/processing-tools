package backuper.operations;

import java.io.IOException;

import backuper.FileCopyStatus;

public interface Operation {
    String getDescription();
    long getCopyFileSize();
    String getRelativePath();

    void perform(FileCopyStatus fileCopyStatus) throws IOException;
}
