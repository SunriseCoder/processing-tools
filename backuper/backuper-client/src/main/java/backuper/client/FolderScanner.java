package backuper.client;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;

import backuper.client.dto.FileMetadata;
import backuper.client.helpers.PrintHelper;

public class FolderScanner {
    private Path startPath;
    private Deque<Path> foldersToScan;
    private Map<String, FileMetadata> foundFiles;

    public synchronized Map<String, FileMetadata> scan(Path startPath) throws IOException {
        foldersToScan = new ArrayDeque<>();
        foundFiles = new LinkedHashMap<>();

        this.startPath = startPath;
        foldersToScan.add(startPath);

        while (!foldersToScan.isEmpty()) {
            scanNextSourceFolder();
            printStatus();
        }

        PrintHelper.println();

        return foundFiles;
    }

    private void scanNextSourceFolder() throws IOException {
        Path folder = foldersToScan.poll();
        FileMetadata fileMetadata = new FileMetadata(folder, startPath);
        foundFiles.put(fileMetadata.getRelativePath().toString(), fileMetadata);

        if (Files.isSymbolicLink(folder)) {
            return;
        }

        try (DirectoryStream<Path> ds = Files.newDirectoryStream(folder)) {
            for (Path path : ds) {
                if (Files.isSymbolicLink(path)) {
                    continue; // Following symlinks only if the option is on
                }

                if (Files.isDirectory(path)) {
                    foldersToScan.add(path);
                }

                fileMetadata = new FileMetadata(path, startPath);
                foundFiles.put(fileMetadata.getRelativePath().toString(), fileMetadata);
            }
        } catch (AccessDeniedException e) {
            System.out.println("Access denided: " + e.getFile() + ", " + e.getMessage());
            // TODO Add logging to the file here
            return; // No scan of the folder, where we don't have permissions
        }
    }

    private void printStatus() {
        String message = "To scan: " + foldersToScan.size() + ", found: " + foundFiles.size();
        PrintHelper.printAndReturn(message);
    }
}
