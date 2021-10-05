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

import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.message.BasicNameValuePair;

import backuper.client.FileCopyStatus;
import backuper.client.config.Configuration;
import backuper.common.dto.FileMetadata;
import backuper.common.helpers.HttpHelper;
import backuper.common.helpers.HttpHelper.Response;
import utils.CloseUtils;
import utils.ThreadUtils;

public class CopyRemoteFileOperation {
    private Configuration config;
    private Path relativePath;
    private Path dstAbsolutePath;
    private long fileSize;
    private boolean newFile;

    private FileMetadata srcFileMetadata;

    private FileCopyStatus fileCopyStatus;

    private RandomAccessFile outputFile;
    private FileChannel outputChannel;

    private String requestUrl;
    private String resourceName;
    private String token;
    private String path;
    private int chunkSize;
    private long nextChunkStart;

    public CopyRemoteFileOperation(Configuration config, FileMetadata srcFileMetadata, String destination, boolean newFile) {
        this.config = config;
        relativePath = srcFileMetadata.getRelativePath();
        dstAbsolutePath = Paths.get(destination, relativePath.toString());
        fileSize = srcFileMetadata.getSize();
        this.newFile = newFile;
        this.srcFileMetadata = srcFileMetadata;
    }

    public String getDescription() {
        return "Copy Remote File \""
                + srcFileMetadata.getResourceHostPort() + srcFileMetadata.getResourceName() + "/" + srcFileMetadata.getRelativePath().toString()
                + "\" to \"" + dstAbsolutePath.toString() + "\"";
    }

    public long getFileSize() {
        return fileSize;
    }

    public boolean isNewFile() {
        return newFile;
    }

    public void prepare(FileCopyStatus fileCopyStatus) {
        System.out.println("Starting " + getDescription());
        this.fileCopyStatus = fileCopyStatus;
        fileCopyStatus.printLastLineCleanup();

        try {
            outputFile = new RandomAccessFile(dstAbsolutePath.toString(), "rw");
            outputFile.setLength(fileSize);
            outputChannel = outputFile.getChannel();
        } catch (IOException e) {
            e.printStackTrace();
            CloseUtils.close(outputChannel);
            CloseUtils.close(outputFile);
        }

        requestUrl = srcFileMetadata.getResourceHostPort() + "file-data";
        resourceName = srcFileMetadata.getResourceName();
        token = srcFileMetadata.getToken();
        path = srcFileMetadata.getRelativePath().toString();
        chunkSize = config.getRemoteFileMaxChunkSize();
        nextChunkStart = 0;
    }

    public boolean hasNextChunk() {
        return nextChunkStart < (fileSize - 1);
    }

    public CopyChunkTask getNextChunk() {
        CopyChunkTask task = new CopyChunkTask(requestUrl, resourceName, token, path, nextChunkStart, chunkSize, outputChannel, fileCopyStatus);
        nextChunkStart += chunkSize;
        return task;
    }

    public void finish() throws IOException {
        CloseUtils.close(outputChannel);
        CloseUtils.close(outputFile);

        Files.setAttribute(dstAbsolutePath, "creationTime", srcFileMetadata.getCreationTime());
        Files.setAttribute(dstAbsolutePath, "lastModifiedTime", srcFileMetadata.getLastModified());
        Files.setAttribute(dstAbsolutePath, "lastAccessTime", srcFileMetadata.getLastAccessTime());

        // TODO Add copy to the temporary file (in the temporary folder as well) first and then at this place just move it on the place

        fileCopyStatus.printLastLineCleanup();
        System.out.println("Finished " + getDescription());
    }

    @Override
    public String toString() {
        return getDescription();
    }

    public static class CopyChunkTask implements Runnable {
        private String requestUrl;
        private String resourceName;
        private String token;
        private String path;
        private long start;
        private long length;

        private FileChannel out;
        private FileCopyStatus fileCopyStatus;

        public CopyChunkTask(String requestUrl, String resourceName, String token, String path, long start, long length,
                FileChannel out, FileCopyStatus fileCopyStatus) {
            this.requestUrl = requestUrl;
            this.resourceName = resourceName;
            this.token = token;
            this.path = path;
            this.start = start;
            this.length = length;

            this.out = out;
            this.fileCopyStatus = fileCopyStatus;
        }

        @Override
        public void run() {
            List<NameValuePair> postData = new ArrayList<>();
            postData.add(new BasicNameValuePair("resource", resourceName));
            postData.add(new BasicNameValuePair("token", token));
            postData.add(new BasicNameValuePair("path", path));
            postData.add(new BasicNameValuePair("start", String.valueOf(start)));
            postData.add(new BasicNameValuePair("length", String.valueOf(length)));

            Response response = null;
            do {
                try {
                    response = HttpHelper.sendPostRequest(requestUrl, postData);
                } catch (Exception e) {
                    e.printStackTrace();
                    ThreadUtils.sleep(5000);
                }

                if (response != null && response.getCode() != 200) {
                    System.out.println("Got response: " + response.getCode() + " " + new String(response.getData()));
                    ThreadUtils.sleep(5000);
                }
            } while (response == null || response.getCode() != 200);

            byte[] responseData = response.getData();
            ByteBuffer buffer = ByteBuffer.wrap(responseData);
            saveToDisk(buffer, start, out);
            fileCopyStatus.addCopiedSize(responseData.length);
        }

        private static synchronized void saveToDisk(ByteBuffer buffer, long start, FileChannel out) {
            try {
                out.write(buffer, start);
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Error due to writing file, exiting...");
                System.exit(-1);
            }
        }
    }
}
