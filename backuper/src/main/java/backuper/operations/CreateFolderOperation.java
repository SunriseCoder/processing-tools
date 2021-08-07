package backuper.operations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import backuper.FileCopyStatus;
import backuper.dto.FileMetadata;

public class CreateFolderOperation implements Operation {
    private Path absolutePath;
    private Path relativePath;

    public CreateFolderOperation(FileMetadata srcFileMetadata, String destinationBase) {
        absolutePath = Paths.get(destinationBase, srcFileMetadata.getRelativePath().toString());
        relativePath = srcFileMetadata.getRelativePath();
    }

    @Override
    public String getDescription() {
        return "Create folder: \"" + absolutePath.toString() + "\"";
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
        Files.createDirectory(absolutePath);
        System.out.println("Folder \"" + absolutePath.toString() + "\" has been successfully created");
    }

    @Override
    public String toString() {
        return getDescription();
    }
}
