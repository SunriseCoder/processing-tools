package files;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;

public class PathIterator implements Iterator<Path> {
    private boolean filesOnly;

    private Queue<Path> nonScannedFolders;
    private Queue<Path> foundPaths;

    public PathIterator(Path rootFolder, boolean filesOnly) throws IOException {
        this.filesOnly = filesOnly;
        nonScannedFolders = new ArrayDeque<>();
        foundPaths = new ArrayDeque<>();

        if (Files.notExists(rootFolder)) {
            throw new FileNotFoundException(rootFolder.toAbsolutePath().toString());
        }
        if (!Files.isDirectory(rootFolder)) {
            throw new NotDirectoryException(rootFolder.toAbsolutePath().toString());
        }
        nonScannedFolders.add(rootFolder);
    }

    @Override
    public boolean hasNext() {
        while (foundPaths.isEmpty() && !nonScannedFolders.isEmpty()) {
            try {
                scanNonScannedFolder();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        boolean result = !foundPaths.isEmpty();
        return result;
    }

    private void scanNonScannedFolder() throws IOException {
        Path folder = nonScannedFolders.poll();
        DirectoryStream<Path> directoryStream = Files.newDirectoryStream(folder);
        for (Path path : directoryStream) {
            if (Files.isSymbolicLink(path)) {
                continue;
            } else if (Files.isDirectory(path)) {
                nonScannedFolders.add(path);
                if (!filesOnly) {
                    foundPaths.add(path);
                }
            } else if (Files.isRegularFile(path)) {
                foundPaths.add(path);
            } else {
                throw new IllegalStateException("Path is neither file, neither folder or there is a bug in this if-statement");
            }
        }
    }

    @Override
    public Path next() {
        Path result = foundPaths.poll();
        return result;
    }
}
