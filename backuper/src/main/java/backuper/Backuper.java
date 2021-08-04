package backuper;

import java.io.Console;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import backuper.helpers.FormattingHelper;
import backuper.helpers.PrintHelper;

public class Backuper {
    private Path srcPath;
    private Path dstPath;

    private FolderScanner folderScanner;

    private Map<String, FileMetadata> newFiles;
    private Map<String, FileMetadata> changeSizedFiles;
    private Map<String, FileMetadata> deletedFiles;

    public Backuper() {
        folderScanner = new FolderScanner();
        newFiles = new LinkedHashMap<>();
        changeSizedFiles = new LinkedHashMap<>();
    }

    public void doBackup(Options options) throws IOException {
        PrintHelper.println("Scanning source folder...");
        Map<String, FileMetadata> srcFiles = folderScanner.scan(srcPath, options);

        PrintHelper.println("Scanning destination folder...");
        if (Files.notExists(dstPath)) {
            Files.createDirectories(dstPath);
        }
        Map<String, FileMetadata> dstFiles = folderScanner.scan(dstPath, options);

        PrintHelper.println("Comparing the trees...");
        compareFileTrees(srcFiles, dstFiles);
        printTreeDiffs();

        if (isBackupConfirmed()) {
            processFiles();
            PrintHelper.println("Copying is done");
        } else {
            PrintHelper.println("Copying has been cancelled");
        }
    }

    private void compareFileTrees(Map<String, FileMetadata> srcFiles, Map<String, FileMetadata> dstFiles) {
        for (Entry<String, FileMetadata> srcFile : srcFiles.entrySet()) {
            String srcKey = srcFile.getKey();
            if (!dstFiles.containsKey(srcKey)) {
                newFiles.put(srcKey, srcFile.getValue());
            } else {
                FileMetadata srcMetadata = srcFile.getValue();
                FileMetadata dstMetadata = dstFiles.get(srcKey);
                if (srcMetadata.getSize() != dstMetadata.getSize()) {
                    changeSizedFiles.put(srcKey, srcMetadata);
                }
            }
        }

        deletedFiles = dstFiles.entrySet().stream().filter(e -> !srcFiles.containsKey(e.getKey()))
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
    }

    private void printTreeDiffs() {
        PrintHelper.println("New files:");
        printMetadataCollection(newFiles.values(), "+");

        PrintHelper.println("Files with changed size:");
        printMetadataCollection(changeSizedFiles.values(), "*");

        PrintHelper.println("Deleted files:");
        printMetadataCollection(deletedFiles.values(), "-");
    }

    private void printMetadataCollection(Collection<FileMetadata> collection, String prefix) {
        for (FileMetadata element : collection) {
            String fileType = element.isSymlink() ? ">" : element.isDirectory() ? "/" : "#";
            long size = element.getSize();
            String sizeStr = String.format("%8s", FormattingHelper.humanReadableSize(size));

            String message = "    ";
            message += sizeStr + " ";
            message += prefix + " ";
            message += fileType + " ";
            message += element.getRelativePath();

            PrintHelper.println(message);
        }
    }

    private boolean isBackupConfirmed() {
        Console console = System.console();
        if (console == null) {
            return true; // If console does not support by environment, do not asking for confirmation
        }

        PrintHelper.println("Please type \"yes\" to start syncronization");
        String input = console.readLine();
        boolean result = "yes".equals(input);
        return result;
    }

    private void processFiles() throws IOException {
        PrintHelper.println("Synchronization started...");

        FileProcessor processor = new FileProcessor();
        processor.setSrcPath(srcPath);
        processor.setDstPath(dstPath);

        processor.addFilesToCopy(newFiles.values());
        processor.addFilesToCopy(changeSizedFiles.values());
        processor.addFilesToDelete(deletedFiles.values());

        processor.start();
    }

    public void setSrcPath(Path srcPath) {
        this.srcPath = srcPath;
    }

    public void setDstPath(Path dstPath) {
        this.dstPath = dstPath;
    }
}
