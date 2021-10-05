package backuper.client.operations;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.apache.hc.core5.http.HttpException;

import backuper.client.FileCopyStatus;
import backuper.client.config.BackupTask;
import backuper.client.config.Configuration;
import backuper.common.dto.FileMetadata;
import backuper.common.helpers.PrintHelper;

public class CopyLocalFileOperation {
    private Configuration config;
    private BackupTask backupTask;

    private Path relativePath;
    private Path srcAbsolutePath;
    private Path dstAbsolutePath;
    private long fileSize;
    private boolean newFile;

    private FileMetadata srcFileMetadata;

    public CopyLocalFileOperation(Configuration config, BackupTask backupTask, FileMetadata srcFileMetadata, String destination, boolean newFile) {
        this.config = config;
        this.backupTask = backupTask;

        this.relativePath = srcFileMetadata.getRelativePath();
        this.srcAbsolutePath = srcFileMetadata.getAbsolutePath();
        this.dstAbsolutePath = Paths.get(destination, relativePath.toString());
        this.fileSize = srcFileMetadata.getSize();
        this.newFile = newFile;
        this.srcFileMetadata = srcFileMetadata;
    }

    public String getDescription() {
        return "Copy Local File \"" + srcAbsolutePath.toString() + "\" to \"" + dstAbsolutePath.toString() + "\"";
    }

    public long getFileSize() {
        return fileSize;
    }

    public boolean isNewFile() {
        return newFile;
    }

    public void perform(FileCopyStatus fileCopyStatus) throws IOException, HttpException {
        File tmpFile = Files.createTempFile(Paths.get(backupTask.getTmp()), "tmp-local-", ".tmp").toFile();
        try (RandomAccessFile outputFile = new RandomAccessFile(tmpFile, "rw");) {
            outputFile.setLength(fileSize);
            FileChannel out = outputFile.getChannel();

            fileCopyStatus.startNewFile(fileSize);

            try (RandomAccessFile inputFile = new RandomAccessFile(srcAbsolutePath.toString(), "r")) {
                FileChannel in = inputFile.getChannel();
                long read;
                ByteBuffer buffer = ByteBuffer.allocate(config.getLocalFileChunkSize());
                while ((read = in.read(buffer)) > 0) {
                    buffer.flip();
                    // TODO Debug here, problems due to copy, maybe use transfers
                    out.write(buffer);
                    fileCopyStatus.addCopiedSize(read);
                    fileCopyStatus.printCopyProgress(false);
                    buffer = ByteBuffer.allocate(config.getLocalFileChunkSize());
                }
            }

            Files.setAttribute(tmpFile.toPath(), "creationTime", srcFileMetadata.getCreationTime());
            Files.setAttribute(tmpFile.toPath(), "lastModifiedTime", srcFileMetadata.getLastModified());
            Files.setAttribute(tmpFile.toPath(), "lastAccessTime", srcFileMetadata.getLastAccessTime());

            Files.move(tmpFile.toPath(), dstAbsolutePath, StandardCopyOption.ATOMIC_MOVE);

            fileCopyStatus.printCopyProgress(false);
            PrintHelper.println();
        }
    }

    @Override
    public String toString() {
        return getDescription();
    }
}
