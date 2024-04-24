package app.files;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// TODO Remove (or not) this class, replace all usages with Files.walk() or Files.walkFileTree()
//  Or make iterator which will make walkFileTree inside and collect everything - Folders, Files, Links, etc...
public class PathIterator implements Iterator<Path>, Iterable<Path> {
    private static final Logger LOGGER = LogManager.getLogger(PathIterator.class);

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
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(folder);) {

            for (Path path : directoryStream) {
                // TODO Refactor:
                // 1. Add attributes to the new wrapper structure for the Path and pass it for further processing
                // 2. Research, is it possible to restrict usage of nio.Files class everywhere except for FileUtils class
                // !!! LinkOption.NOFOLLOW_LINKS is essential to make attributes.isSymbolicLink() working !!!
                BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
                if (attributes.isSymbolicLink()) {
                    continue;
                } else if (attributes.isDirectory()) {
                    nonScannedFolders.add(path);
                    if (!filesOnly) {
                        foundPaths.add(path);
                    }
                } else if (attributes.isRegularFile()) {
                    foundPaths.add(path);
                } else {
                    throw new IllegalStateException("Path is neither file, neither folder or there is a bug in this if-statement:"
                            + path.toString());
                }
            }
        } catch (FileSystemException e) {
            LOGGER.error("Exception during Scan Folders", e);
        }
    }

    @Override
    public Path next() {
        Path result = foundPaths.poll();
        return result;
    }

    @Override
    public Iterator<Path> iterator() {
        return this;
    }

    public Stream<Path> stream() {
        return StreamSupport.stream(spliterator(), false);
    }
}
