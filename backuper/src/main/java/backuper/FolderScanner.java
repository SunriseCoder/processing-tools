package backuper;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;

import backuper.helpers.PrintHelper;

public class FolderScanner {
    private Options options;
    private Path startPath;
    private Deque<Path> foldersToScan;
    private Map<String, FileMetadata> foundFiles;

    public synchronized Map<String, FileMetadata> scan(Path path, Options options) throws IOException {
        reset(options);

        this.startPath = path;
        foldersToScan.add(startPath);

        while (!foldersToScan.isEmpty()) {
            scanNextSourceFolder();
            printStatus();
        }

        PrintHelper.println();

        return foundFiles;
    }

    private void reset(Options options) {
        this.options = options;
        foldersToScan = new LinkedList<>();
        foundFiles = new TreeMap<>();
    }

    private void scanNextSourceFolder() throws IOException {
        Path folder = foldersToScan.poll();

        if (Files.isSymbolicLink(folder) && !options.isSet(Options.Names.FOLLOW_SYMLINKS)) {
            return;
        }

        try (DirectoryStream<Path> ds = Files.newDirectoryStream(folder)) {
            for (Path path : ds) {
                if (Files.isSymbolicLink(path) && !options.isSet(Options.Names.FOLLOW_SYMLINKS)) {
                    continue; // Following symlinks only if the option is on
                }

                if (Files.isDirectory(path)) {
                    foldersToScan.add(path);
                }

                String relativePath = startPath.relativize(path).toString();
                foundFiles.put(relativePath, new FileMetadata(path, relativePath));
            }
        } catch (AccessDeniedException e) {
            return; // No scan of the folder, where we don't have permissions
        }
    }

    private void printStatus() {
        String message = "Scanning: " + foldersToScan.size() + ", found: " + foundFiles.size();
        PrintHelper.printAndReturn(message);
    }
}
