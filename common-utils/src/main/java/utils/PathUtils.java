package utils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PathUtils {

    public static Path checkAndGetFolder(String absoluteFolderName) throws IOException {
        Path path = Paths.get(absoluteFolderName).toAbsolutePath();
        if (Files.notExists(path)) {
            throw new FileNotFoundException(absoluteFolderName);
        } else if (!Files.isDirectory(path)) {
            throw new NotDirectoryException(absoluteFolderName);
        }
        return path;
    }
}
