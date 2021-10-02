package backuper.client.operations;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.message.BasicNameValuePair;

import backuper.client.FileCopyStatus;
import backuper.client.config.Configuration;
import backuper.common.dto.FileMetadata;
import backuper.common.helpers.HttpHelper;
import backuper.common.helpers.HttpHelper.Response;
import utils.MathUtils;
import utils.ThreadUtils;

public class CopyRemoteFileOperation implements Operation {
    private Configuration config;
    private Path relativePath;
    private Path dstAbsolutePath;
    private long fileSize;
    private boolean newFile;

    private FileMetadata srcFileMetadata;

    public CopyRemoteFileOperation(Configuration config, FileMetadata srcFileMetadata, String destination, boolean newFile) {
        this.config = config;
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
            outputFile.setLength(fileSize);
            FileChannel out = outputFile.getChannel();
            fileCopyStatus.startNewFile(fileSize);

            int maxConnectionsNumber = config.getMaxConnectionsNumber();
            int maxFuturesNumber = maxConnectionsNumber * 2;
            ExecutorService executor = Executors.newFixedThreadPool(maxConnectionsNumber);
            List<Future<?>> futures = new ArrayList<>();

            String requestUrl = srcFileMetadata.getResourceHostPort() + "file-data";
            String resourceName = srcFileMetadata.getResourceName();
            String token = srcFileMetadata.getToken();
            String path = srcFileMetadata.getRelativePath().toString();
            int chunkSize = calculateChunkSize();
            long chunkStart = 0;

            while (chunkStart < fileSize || futures.size() > 0) {
                // Checking and cleaning up finished tasks
                Iterator<Future<?>> iterator = futures.iterator();
                while (iterator.hasNext()) {
                    Future<?> future = iterator.next();
                    if (future.isDone()) {
                        iterator.remove();
                    }
                }

                // Adding more tasks
                while (chunkStart < fileSize && futures.size() < maxFuturesNumber) {
                    GetChunkTask task = new GetChunkTask(requestUrl, resourceName, token, path, chunkStart, chunkSize, out, fileCopyStatus);
                    Future<?> future = executor.submit(task);
                    futures.add(future);
                    chunkStart += chunkSize;
                }

                if (futures.size() > 0) {
                    ThreadUtils.sleep(10);
                }
                fileCopyStatus.printCopyProgress();
            }

            executor.shutdown();

            Files.setAttribute(dstAbsolutePath, "creationTime", srcFileMetadata.getCreationTime());
            Files.setAttribute(dstAbsolutePath, "lastModifiedTime", srcFileMetadata.getLastModified());
            Files.setAttribute(dstAbsolutePath, "lastAccessTime", srcFileMetadata.getLastAccessTime());

            fileCopyStatus.printLastLineCleanup();
       }
    }

    private int calculateChunkSize() {
        int chunkSize;
        if (fileSize < config.getRemoteFileMinChunkSize() * config.getMaxConnectionsNumber()) {
            chunkSize = config.getRemoteFileMinChunkSize();
        } else if (fileSize > config.getRemoteFileMaxChunkSize() * config.getMaxConnectionsNumber()) {
            chunkSize = config.getRemoteFileMaxChunkSize();
        } else {
            chunkSize = MathUtils.ceilToInt((double) fileSize / config.getMaxConnectionsNumber());
        }
        return chunkSize;
    }

    @Override
    public String toString() {
        return getDescription();
    }

    private static class GetChunkTask implements Runnable {
        private String requestUrl;
        private String resourceName;
        private String token;
        private String path;
        private long start;
        private long length;

        private FileChannel out;
        private FileCopyStatus fileCopyStatus;

        public GetChunkTask(String requestUrl, String resourceName, String token, String path, long start, long length,
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
            fileCopyStatus.printCopyProgress();
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
