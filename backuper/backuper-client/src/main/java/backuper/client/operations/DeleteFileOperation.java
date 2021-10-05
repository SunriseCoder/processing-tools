package backuper.client.operations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import backuper.common.dto.FileMetadata;

public class DeleteFileOperation implements Operation {
    private Path absolutePath;
    private long fileSize;

    public DeleteFileOperation(FileMetadata fileMetadata) {
        absolutePath = fileMetadata.getAbsolutePath();
        fileSize = fileMetadata.getSize();
    }

    @Override
    public String getDescription() {
        return "Delete file: \"" + absolutePath.toString() + "\"";
    }

    @Override
    public long getFileSize() {
        return fileSize;
    }

    @Override
    public void perform() throws IOException {
        Files.delete(absolutePath);
        System.out.println("File \"" + absolutePath.toString() + "\" has been successfully deleted");
    }

    @Override
    public String toString() {
        return getDescription();
    }
}
