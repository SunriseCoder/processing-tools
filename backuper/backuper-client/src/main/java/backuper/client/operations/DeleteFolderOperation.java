package backuper.client.operations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import backuper.common.dto.FileMetadata;

public class DeleteFolderOperation implements Operation {
    private Path absolutePath;

    public DeleteFolderOperation(FileMetadata fileMetadata) {
        absolutePath = fileMetadata.getAbsolutePath();
    }

    @Override
    public String getDescription() {
        return "Delete folder: \"" + absolutePath.toString() + "\"";
    }

    @Override
    public long getFileSize() {
        return 0;
    }

    @Override
    public void perform() throws IOException {
        Files.delete(absolutePath);
        System.out.println("Folder \"" + absolutePath.toString() + "\" has been successfully deleted");
    }

    @Override
    public String toString() {
        return getDescription();
    }
}
