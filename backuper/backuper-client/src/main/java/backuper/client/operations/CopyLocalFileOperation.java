package backuper.client.operations;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.hc.core5.http.HttpException;

import backuper.client.FileCopyStatus;
import backuper.client.config.Configuration;
import backuper.common.dto.FileMetadata;
import backuper.common.helpers.PrintHelper;

public class CopyLocalFileOperation {
    private Configuration configuration;

    private Path relativePath;
    private Path srcAbsolutePath;
    private Path dstAbsolutePath;
    private long fileSize;
    private boolean newFile;

    private FileMetadata srcFileMetadata;

    public CopyLocalFileOperation(Configuration configuration, FileMetadata srcFileMetadata, String destination, boolean newFile) {
        this.configuration = configuration;
        relativePath = srcFileMetadata.getRelativePath();
        srcAbsolutePath = srcFileMetadata.getAbsolutePath();
        dstAbsolutePath = Paths.get(destination, relativePath.toString());
        fileSize = srcFileMetadata.getSize();
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
        try (RandomAccessFile outputFile = new RandomAccessFile(dstAbsolutePath.toString(), "rw");) {
            outputFile.setLength(fileSize);
            FileChannel out = outputFile.getChannel();

            fileCopyStatus.startNewFile(fileSize);

            try (RandomAccessFile inputFile = new RandomAccessFile(srcAbsolutePath.toString(), "r")) {
                FileChannel in = inputFile.getChannel();
                long read;
                ByteBuffer buffer = ByteBuffer.allocate(configuration.getLocalFileChunkSize());
                while ((read = in.read(buffer)) > 0) {
                    buffer.flip();
                    // TODO Debug here, problems due to copy, maybe use transfers
                    out.write(buffer);
                    fileCopyStatus.addCopiedSize(read);
                    fileCopyStatus.printCopyProgress(false);
                    buffer = ByteBuffer.allocate(configuration.getLocalFileChunkSize());
                }
            }

            Files.setAttribute(dstAbsolutePath, "creationTime", srcFileMetadata.getCreationTime());
            Files.setAttribute(dstAbsolutePath, "lastModifiedTime", srcFileMetadata.getLastModified());
            Files.setAttribute(dstAbsolutePath, "lastAccessTime", srcFileMetadata.getLastAccessTime());

            // TODO Add copy to the temporary file (in the temporary folder as well) first and then at this place just move it on the place

            fileCopyStatus.printCopyProgress(false);
            PrintHelper.println();
        }
    }

    @Override
    public String toString() {
        return getDescription();
    }
}
