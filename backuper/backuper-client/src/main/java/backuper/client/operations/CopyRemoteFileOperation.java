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
import backuper.client.dto.Configuration;
import backuper.common.dto.FileMetadata;
import backuper.common.helpers.HttpHelper;
import backuper.common.helpers.HttpHelper.Response;
import utils.ThreadUtils;

public class CopyRemoteFileOperation implements Operation {
    private Configuration configuration;
    private Path relativePath;
    private Path dstAbsolutePath;
    private long fileSize;
    private boolean newFile;

    private FileMetadata srcFileMetadata;

    public CopyRemoteFileOperation(Configuration configuration, FileMetadata srcFileMetadata, String destination, boolean newFile) {
        this.configuration = configuration;
        relativePath = srcFileMetadata.getRelativePath();
        dstAbsolutePath = Paths.get(destination, relativePath.toString());
        fileSize = srcFileMetadata.getSize();
        this.newFile = newFile;
        this.srcFileMetadata = srcFileMetadata;
    }

    @Override
    public String getDescription() {
        return "Copy Remote File \""
                + srcFileMetadata.getResourceHostPort() + srcFileMetadata.getResourceName() + "/" + srcFileMetadata.getRelativePath().toString()
                + "\" to \"" + dstAbsolutePath.toString() + "\"";
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

           int chunkSize = configuration.getRemoteFileChunkSize();
           for (long i = 0; i < fileSize; i += chunkSize) {
               String requestUrl = srcFileMetadata.getResourceHostPort() + "file-data";
               List<NameValuePair> postData = new ArrayList<>();
               postData.add(new BasicNameValuePair("resource", srcFileMetadata.getResourceName()));
               postData.add(new BasicNameValuePair("token", srcFileMetadata.getToken()));
               postData.add(new BasicNameValuePair("path", srcFileMetadata.getRelativePath().toString()));
               postData.add(new BasicNameValuePair("start", String.valueOf(i)));
               postData.add(new BasicNameValuePair("length", String.valueOf(chunkSize)));

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
