package backuper.client.operations;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.message.BasicNameValuePair;

import backuper.client.FileCopyStatus;
import backuper.common.dto.FileMetadata;
import backuper.common.helpers.HttpHelper;
import backuper.common.helpers.HttpHelper.Response;
import utils.ThreadUtils;

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
        return "Copy file \"" + (srcFileMetadata.isRemote()
                ? srcFileMetadata.getResourceHostPort() + srcFileMetadata.getResourceName() + "/" + srcFileMetadata.getRelativePath().toString()
                : srcAbsolutePath.toString()) + "\" to \"" + dstAbsolutePath.toString() + "\"";
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

           if (srcFileMetadata.isRemote()) {
               for (long i = 0; i < fileSize; i += COPY_BUFFER_SIZE) {
                   String requestUrl = srcFileMetadata.getResourceHostPort() + "file-data";
                   List<NameValuePair> postData = new ArrayList<>();
                   postData.add(new BasicNameValuePair("resource", srcFileMetadata.getResourceName()));
                   postData.add(new BasicNameValuePair("token", srcFileMetadata.getToken()));
                   postData.add(new BasicNameValuePair("path", srcFileMetadata.getRelativePath().toString()));
                   postData.add(new BasicNameValuePair("start", String.valueOf(i)));
                   postData.add(new BasicNameValuePair("length", String.valueOf(COPY_BUFFER_SIZE)));

                   Response response = null;
                do {
                       response = HttpHelper.sendPostRequest(requestUrl, postData);
                       if (response.getCode() != 200) {
                           System.out.println("Got response: " + response.getCode() + " " + new String(response.getData()));
                           ThreadUtils.sleep(5000);
                       }
                   } while (response == null || response.getCode() != 200);

                   byte[] responseData = response.getData();
                   ByteBuffer buffer = ByteBuffer.wrap(responseData);
                   out.write(buffer);
                   fileCopyStatus.addCopiedSize(responseData.length);
                   fileCopyStatus.printCopyProgress();
               }
           } else {
               try (RandomAccessFile inputFile = new RandomAccessFile(srcAbsolutePath.toString(), "r")) {
                   FileChannel in = inputFile.getChannel();
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
