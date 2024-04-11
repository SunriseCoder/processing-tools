package app.core.file.operations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

import app.progress.ProgressPrinter;
import app.utils.FormattingUtils;
import app.utils.PathUtils;

public class MoveFolderOperation implements FileOperation {
    private Path source;
    private Path destination;
    private long fileSize;

    private boolean isMoveWithinSameDisk;
    private long copyChunkSize;
    private FileTime lastModifiedTime;

    private ProgressPrinter progressPrinter;
    private Path temporaryFolder;

    public MoveFolderOperation(Path source, Path destination) throws IOException {
        this.source = source;
        this.destination = destination;

        isMoveWithinSameDisk = PathUtils.isSameDrive(source, destination);

        // TODO Refactor
        // 1. Replace FS IO operation with retrieving fileSize from FileMetadata (wrapper of the nio Path with its attributes)
        // 2. Remove throw IOException from constructor signature
        fileSize = PathUtils.countFolderSize(source);
    }

    @Override
    public boolean isValid() {
        boolean result = source != null && destination != null && !source.equals(destination);
        result &= PathUtils.exists(source) && PathUtils.isDirectory(source);
        result &= (PathUtils.notExists(destination) && PathUtils.exists(destination.getRoot()))
                || (PathUtils.exists(destination) && PathUtils.isDirectory(destination));
        return result;
    }

    @Override
    public long getCopyDataSize() {
        long copyDataSize = isMoveWithinSameDisk ? 0 : fileSize;
        return copyDataSize;
    }

    @Override
    public long getFileSize() {
        return fileSize;
    }

    @Override
    public void setTemporaryFolder(Path temporaryFolder) {
        this.temporaryFolder = temporaryFolder;
    }

    @Override
    public void setCopyChunkSize(long copyChunkSize) {
        this.copyChunkSize = copyChunkSize;
    }

    @Override
    public void setLastModifiedTime(FileTime lastModifiedTime) {
        this.lastModifiedTime = lastModifiedTime;
    }

    @Override
    public void setProgressPrinter(ProgressPrinter progressPrinter) {
        this.progressPrinter = progressPrinter;
    }

    @Override
    public void perform() throws IOException {
        // TODO Add startNewFolder and finishNewFolder to ProgressPrinter.
        //      Use startNewFile probably at the level of PathUtils.copyFile
        //      Find all Calls of startNewFile and replace with startNewFolder where needed,
        //          same with finishCurrentFolder
        progressPrinter.startNewFile("\n" + this.toString(), getCopyDataSize());

        if (lastModifiedTime != null) {
            // Set lastModifiedTime attribute to the source file before move is done
            Files.setAttribute(source, "lastModifiedTime", lastModifiedTime, LinkOption.NOFOLLOW_LINKS);
        }
        PathUtils.moveFolderSmart(source, destination, temporaryFolder, copyChunkSize, progressPrinter);

        progressPrinter.finishCurrentFile();
    }

    @Override
    public String toString() {
        String fileSizeString = "(" + fileSize + " = " + FormattingUtils.humanReadableSizeBi(fileSize) + "b)";
        return FormattingUtils.alignLongStringsByRightSide(3,
                "MOVE_FOLDER", source.toString(), fileSizeString,
                "->", destination.toString(), "");
    }
}
