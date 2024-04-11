package app.core.file.operations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

import app.progress.ProgressPrinter;
import app.utils.FormattingUtils;
import app.utils.PathUtils;

public class CopyOperation implements FileOperation {
    private Path source;
    private Path destination;
    private long fileSize;

    private long copyChunkSize;
    private FileTime lastModifiedTime;

    private ProgressPrinter progressPrinter;
    private Path temporaryFolder;

    public CopyOperation(Path source, Path destination) throws IOException {
        this.source = source;
        this.destination = destination;

        // TODO Refactor
        // 1. Replace FS IO operation with retrieving fileSize from FileMetadata (wrapper of the nio Path with its attributes)
        // 2. Remove throw IOException from constructor signature
        this.fileSize = Files.size(source);
    }

    @Override
    public boolean isValid() {
        boolean result = source != null && destination != null && !source.equals(destination);
        return result;
    }

    @Override
    public long getCopyDataSize() {
        return fileSize;
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
        progressPrinter.startNewFile("\n" + this.toString(), getCopyDataSize());

        Path temporaryFile = Files.createTempFile(temporaryFolder, "copy-file-tmp-", ".tmp");
        PathUtils.copyFile(source, temporaryFile, copyChunkSize, progressPrinter);
        if (lastModifiedTime != null) {
            // Set lastModifiedTime attribute to temporary file after the copy is done
            Files.setAttribute(temporaryFile, "lastModifiedTime", lastModifiedTime, LinkOption.NOFOLLOW_LINKS);
        }

        PathUtils.moveFileWithReplacement(temporaryFile, destination);

        progressPrinter.finishCurrentFile();
    }

    @Override
    public String toString() {
        String fileSizeString = "(" + fileSize + " = " + FormattingUtils.humanReadableSizeBi(fileSize) + "b)";
        return FormattingUtils.alignLongStringsByRightSide(3,
                "COPY", source.toString(), fileSizeString,
                "->", destination.toString(), "");
    }
}
