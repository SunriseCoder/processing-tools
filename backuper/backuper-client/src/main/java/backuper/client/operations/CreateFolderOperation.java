package backuper.client.operations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import backuper.common.dto.FileMetadata;

public class CreateFolderOperation implements Operation {
    private Path absolutePath;

    public CreateFolderOperation(FileMetadata srcFileMetadata, String destinationBase) {
        absolutePath = Paths.get(destinationBase, srcFileMetadata.getRelativePath().toString());
    }

    @Override
    public String getDescription() {
        return "Create folder: \"" + absolutePath.toString() + "\"";
    }

    @Override
    public long getFileSize() {
        return 0;
    }

    @Override
    public void perform() throws IOException {
        Files.createDirectory(absolutePath);
        System.out.println("Folder \"" + absolutePath.toString() + "\" has been successfully created");
    }

    @Override
    public String toString() {
        return getDescription();
    }
}
