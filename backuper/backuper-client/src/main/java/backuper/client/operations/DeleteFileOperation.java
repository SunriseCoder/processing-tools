package backuper.client.operations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import backuper.client.FileCopyStatus;
import backuper.common.dto.FileMetadata;

public class DeleteFileOperation implements Operation {
    private Path absolutePath;
    private Path relativePath;

    private long fileSize;

    public DeleteFileOperation(FileMetadata fileMetadata) {
        absolutePath = fileMetadata.getAbsolutePath();
        relativePath = fileMetadata.getRelativePath();
        fileSize = fileMetadata.getSize();
    }

    @Override
    public String getDescription() {
        return "Delete file: \"" + absolutePath.toString() + "\"";
    }

    @Override
    public long getCopyFileSize() {
        return fileSize;
    }

    @Override
    public String getRelativePath() {
        return relativePath.toString();
    }

    @Override
    public void perform(FileCopyStatus fileCopyStatus) throws IOException {
        Files.delete(absolutePath);
        System.out.println("File \"" + absolutePath.toString() + "\" has been successfully deleted");
    }

    @Override
    public String toString() {
        return getDescription();
    }
}
