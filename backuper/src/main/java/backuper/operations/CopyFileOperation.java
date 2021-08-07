package backuper.operations;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import backuper.FileCopyStatus;
import backuper.dto.FileMetadata;

public class CopyFileOperation implements Operation {
    private static final int COPY_BUFFER_SIZE = 1024 * 1024;

    private Path relativePath;
    private Path srcAbsolutePath;
    private Path dstAbsolutePath;
    private long fileSize;
    private boolean newFile;

    private FileMetadata srcFileMetadata;

    public CopyFileOperation(FileMetadata srcFileMetadata, String destination, boolean newFile) {
        relativePath = srcFileMetadata.getRelativePath();
        srcAbsolutePath = srcFileMetadata.getAbsolutePath();
        dstAbsolutePath = Paths.get(destination, relativePath.toString());
        fileSize = srcFileMetadata.getSize();
        this.newFile = newFile;
        this.srcFileMetadata = srcFileMetadata;
    }

    @Override
    public String getDescription() {
        return "Copy file \"" + srcAbsolutePath.toString() + "\" to \"" + dstAbsolutePath.toString() + "\"";
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
    public void perform(FileCopyStatus fileCopyStatus) throws IOException {
        try (RandomAccessFile inputFile = new RandomAccessFile(srcAbsolutePath.toString(), "r");
                RandomAccessFile outputFile = new RandomAccessFile(dstAbsolutePath.toString(), "rw");) {

           FileChannel in = inputFile.getChannel();
           FileChannel out = outputFile.getChannel();

           fileCopyStatus.setCurrentFileCopiedSize(0);
           fileCopyStatus.setCurrentFileTotalSize(fileSize);
           fileCopyStatus.printCopyProgress();

           long read;
           ByteBuffer buffer = ByteBuffer.allocate(COPY_BUFFER_SIZE);
           while ((read = in.read(buffer)) > 0) {
               buffer.flip();
               // TODO Debug here, problems due to copy, maybe use transfers
               out.write(buffer);
               fileCopyStatus.addCopiedSize(read);
               fileCopyStatus.printCopyProgress();
               buffer = ByteBuffer.allocate(COPY_BUFFER_SIZE);
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
