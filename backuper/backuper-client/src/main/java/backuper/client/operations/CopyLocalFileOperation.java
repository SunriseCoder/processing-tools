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
import backuper.client.dto.Configuration;
import backuper.common.dto.FileMetadata;

public class CopyLocalFileOperation implements Operation {
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

    @Override
    public String getDescription() {
        return "Copy Local File \"" + srcAbsolutePath.toString() + "\" to \"" + dstAbsolutePath.toString() + "\"";
    }

    @Override
    public long getCopyFileSize() {
        return fileSize;
    }

    @Override
    public String getRelativePath() {
        return relativePath.toString();
    }

    public boolean isNewFile() {
        return newFile;
    }

    @Override
    public void perform(FileCopyStatus fileCopyStatus) throws IOException, HttpException {
        try (RandomAccessFile outputFile = new RandomAccessFile(dstAbsolutePath.toString(), "rw");) {

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
                   fileCopyStatus.printCopyProgress();
                   buffer = ByteBuffer.allocate(configuration.getLocalFileChunkSize());
               }
           }

           Files.setAttribute(dstAbsolutePath, "creationTime", srcFileMetadata.getCreationTime());
           Files.setAttribute(dstAbsolutePath, "lastModifiedTime", srcFileMetadata.getLastModified());
           Files.setAttribute(dstAbsolutePath, "lastAccessTime", srcFileMetadata.getLastAccessTime());
       }
    }

    @Override
    public String toString() {
        return getDescription();
    }
}
