package backuper.client.operations;

import java.io.IOException;

public interface Operation {
    String getDescription();
    long getFileSize();
    void perform() throws IOException;
}
