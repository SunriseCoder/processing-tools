package app.core.file;

import java.util.ArrayDeque;
import java.util.Queue;

import app.core.dto.fs.FileSystemFolder;

public class FolderIterator {
    private FileSystemFolder rootFolder;
    private Queue<FileSystemFolder> unscannedFolders;
    private Queue<FileSystemFolder> nextFolders;

    public FolderIterator(FileSystemFolder rootFolder) {
        unscannedFolders = new ArrayDeque<>();
        nextFolders = new ArrayDeque<>();

        this.rootFolder = rootFolder;
        unscannedFolders.add(rootFolder);
    }

    public boolean hasNext() {
        if (nextFolders.isEmpty() && !unscannedFolders.isEmpty()) {
            scanNextFolder();
        }

        boolean hasNext = !nextFolders.isEmpty();
        return hasNext;
    }

    private void scanNextFolder() {
        FileSystemFolder nextFolder = unscannedFolders.poll();
        nextFolders.add(nextFolder);
        nextFolders.addAll(nextFolder.getFolderCollection());
    }

    public FileSystemFolder next() {
        FileSystemFolder nextFolder = nextFolders.poll();
        return nextFolder;
    }

    public void reset() {
        unscannedFolders.clear();
        nextFolders.clear();
        unscannedFolders.add(rootFolder);
    }
}
