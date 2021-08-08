package backuper.client.operations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import backuper.client.FileCopyStatus;
import backuper.common.dto.FileMetadata;

public class DeleteFolderOperation implements Operation {
    private Path absolutePath;
    private Path relativePath;

    public DeleteFolderOperation(FileMetadata fileMetadata) {
        absolutePath = fileMetadata.getAbsolutePath();
        relativePath = fileMetadata.getRelativePath();
    }

    @Override
    public String getDescription() {
        return "Delete folder: \"" + absolutePath.toString() + "\"";
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
        System.out.println("Folder \"" + absolutePath.toString() + "\" has been successfully deleted");
    }

    @Override
    public String toString() {
        return getDescription();
    }
}
