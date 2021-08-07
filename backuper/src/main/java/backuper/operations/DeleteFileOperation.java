package backuper.operations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import backuper.FileCopyStatus;
import backuper.dto.FileMetadata;

public class DeleteFileOperation implements Operation {
    private Path absolutePath;
    private Path relativePath;

    public DeleteFileOperation(FileMetadata fileMetadata) {
        absolutePath = fileMetadata.getAbsolutePath();
        relativePath = fileMetadata.getRelativePath();
    }

    @Override
    public String getDescription() {
        return "Delete file: \"" + absolutePath.toString() + "\"";
    }

    @Override
    public long getCopyFileSize() {
        return 0;
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
