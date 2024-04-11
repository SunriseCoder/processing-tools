package app.core.file.operations;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

import app.progress.ProgressPrinter;
import app.utils.FormattingUtils;
import app.utils.PathUtils;

public class DeleteFolderOperation implements FileOperation {
    private Path target;
    private String reason;
    private long fileSize;

    private ProgressPrinter progressPrinter;

    public DeleteFolderOperation(Path target, String reason) throws IOException {
        this.target = target;
        this.reason = reason;

        // TODO Refactor
        // 1. Replace FS IO operation with retrieving fileSize from FileMetadata (wrapper of the nio Path with its attributes)
        // 2. Remove throw IOException from constructor signature
        fileSize = PathUtils.countFolderSize(target);
    }

    @Override
    public boolean isValid() {
        boolean result = target != null;
        return result;
    }

    @Override
    public long getCopyDataSize() {
        return 0;
    }

    @Override
    public long getFileSize() {
        return fileSize;
    }

    @Override
    public void setTemporaryFolder(Path temporaryFolder) {
        // Not used by Delete Operation
    }

    @Override
    public void setCopyChunkSize(long copyChunkSize) {
        // Not used by Delete Operation
    }

    @Override
    public void setLastModifiedTime(FileTime lastModifiedTime) {
        // Not used by Delete Operation
    }

    @Override
    public void setProgressPrinter(ProgressPrinter progressPrinter) {
        this.progressPrinter = progressPrinter;
    }

    @Override
    public void perform() throws IOException {
        progressPrinter.startNewFile("\n" + this.toString(), getCopyDataSize());

        PathUtils.deleteFolderRecursively(target);

        // TODO Implement - delete all empty parent folders recursively

        progressPrinter.finishCurrentFile();
    }

    @Override
    public String toString() {
        String fileSizeString = "(" + fileSize + " = " + FormattingUtils.humanReadableSizeBi(fileSize) + "b)";
        return FormattingUtils.alignLongStringsByRightSide(3,
                "DELETE_FOLDER", target.toString(), fileSizeString,
                "!!!", reason, "");
    }
}
