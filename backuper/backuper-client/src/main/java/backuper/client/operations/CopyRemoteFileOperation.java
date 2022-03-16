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
import java.util.ArrayList;
import java.util.List;

import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.message.BasicNameValuePair;

import backuper.client.FileCopyStatus;
import backuper.client.config.BackupTask;
import backuper.common.dto.FileMetadata;
import backuper.common.helpers.HttpHelper;
import backuper.common.helpers.HttpHelper.Response;
import backuper.logger.Logger;
import utils.CloseUtils;
import utils.ThreadUtils;

public class CopyRemoteFileOperation {
    private BackupTask backupTask;

    private Path relativePath;
    private Path dstAbsolutePath;
    private long fileSize;
    private boolean newFile;

    private FileMetadata srcFileMetadata;

    private FileCopyStatus fileCopyStatus;

    private File tmpFile;
    private RandomAccessFile outputFile;
    private FileChannel outputChannel;

    private String requestUrl;
    private String resourceName;
    private String token;
    private String path;
    private int chunkSize;
    private long nextChunkStart;

    public CopyRemoteFileOperation(BackupTask backupTask, FileMetadata srcFileMetadata, String destination, boolean newFile) {
        this.backupTask = backupTask;

        this.relativePath = srcFileMetadata.getRelativePath();
        this.dstAbsolutePath = Paths.get(destination, relativePath.toString());
        this.fileSize = srcFileMetadata.getSize();
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
            this.tmpFile = Files.createTempFile(Paths.get(backupTask.getTmp()), "tmp-remote-", ".tmp").toFile();
            this.outputFile = new RandomAccessFile(tmpFile, "rw");
            this.outputFile.setLength(fileSize);
            this.outputChannel = outputFile.getChannel();
        } catch (IOException e) {
            e.printStackTrace();
            CloseUtils.close(outputChannel);
            CloseUtils.close(outputFile);
        }

        this.requestUrl = srcFileMetadata.getResourceHostPort() + "file-data";
        this.resourceName = srcFileMetadata.getResourceName();
        this.token = srcFileMetadata.getToken();
        this.path = srcFileMetadata.getRelativePath().toString();
        this.chunkSize = backupTask.getCopySettings().getCopyChunkSize();
        this.nextChunkStart = 0;
    }

    public boolean hasNextChunk() {
        return nextChunkStart < (fileSize - 1);
    }

    public CopyChunkTask createNextCopyChunkTask() {
        long length = Math.min(chunkSize, fileSize - nextChunkStart);
        CopyChunkTask task = new CopyChunkTask(requestUrl, resourceName, token, path, nextChunkStart, length, outputChannel, fileCopyStatus);
        nextChunkStart += length;
        return task;
    }

    public void finish() throws IOException {
        CloseUtils.close(outputChannel);
        CloseUtils.close(outputFile);

        Files.setAttribute(tmpFile.toPath(), "creationTime", srcFileMetadata.getCreationTime());
        Files.setAttribute(tmpFile.toPath(), "lastModifiedTime", srcFileMetadata.getLastModified());
        Files.setAttribute(tmpFile.toPath(), "lastAccessTime", srcFileMetadata.getLastAccessTime());

        Files.move(tmpFile.toPath(), dstAbsolutePath, StandardCopyOption.ATOMIC_MOVE);

        fileCopyStatus.printLastLineCleanup();
        System.out.println("Finished " + getDescription());
    }

    @Override
    public String toString() {
        return getDescription();
    }

    public static class CopyChunkTask implements Runnable {
        private String requestUrl;
        private String resourceUrl;
        private String token;
        private String path;
        private long start;
        private long length;

        private FileChannel out;
        private FileCopyStatus fileCopyStatus;

        public CopyChunkTask(String requestUrl, String resourceUrl, String token, String path, long start, long length,
                FileChannel out, FileCopyStatus fileCopyStatus) {
            this.requestUrl = requestUrl;
            this.resourceUrl = resourceUrl;
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
            postData.add(new BasicNameValuePair("resource", resourceUrl));
            postData.add(new BasicNameValuePair("token", token));
            postData.add(new BasicNameValuePair("path", path));
            postData.add(new BasicNameValuePair("start", String.valueOf(start)));
            postData.add(new BasicNameValuePair("length", String.valueOf(length)));

            boolean success = false;
            do {
                try {
                    log("1-start", null);

                    Response response = HttpHelper.sendPostRequest(requestUrl, postData);

                    log("2-request-sent", null);

                    if (response != null && response.getCode() != 200) {
                        System.out.println("Got response: " + response.getCode() + " " + new String(response.getData()));
                        ThreadUtils.sleep(5000);
                        continue;
                    }

                    log("3-response-is-valid", null);

                    byte[] responseData = response.getData();
                    if (responseData.length != length) {
                        continue;
                    }

                    log("4-data-length-is-valid", null);

                    ByteBuffer buffer = ByteBuffer.wrap(responseData);
                    saveToDisk(buffer, start, out);

                    log("5-saved-to-disk", null);

                    fileCopyStatus.addCopiedSize(responseData.length);

                    log("6-added-copy-size", null);

                    success = true;

                    log("7-success-true", null);
                } catch (Exception e) {
                    log("0-exception", e);
                    e.printStackTrace();
                    ThreadUtils.sleep(5000);
                }
            } while (!success);

            log("8-task-end", null);
        }

        private void log(String stage, Exception e) {
            StringBuilder sb = new StringBuilder();
            sb.append("{\"stage\": \"").append(stage).append("\"")
                    .append(", \"path\": \"").append(path).append("\"")
                    .append(", \"start\": \"").append(start).append("\"")
                    .append(", \"length\": \"").append(length).append("\"")
                    .append(", \"error\": \"").append(e == null ? null : e.toString()).append("\"")
                    .append("}");
            Logger.trace(sb.toString());
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
