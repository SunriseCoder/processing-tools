package core.youtube;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.fasterxml.jackson.databind.JsonNode;

import core.youtube.YoutubeVideoHandler.Format;
import download.DownloadProgressPrinter;
import download.DownloadTask;
import download.DownloadTask.Result;
import util.DownloadUtils;
import util.PageParsing;
import utils.FileUtils;
import utils.JSONUtils;
import utils.MathUtils;
import utils.ThreadUtils;

public class YoutubeOrdinaryFileDownloader {
    private static final int MAX_YOUTUBE_DOWNLOAD_THREADS = 100;
    private static final int MIN_CHUNK_SIZE = 64 * 1024;
    private static final int MAX_CHUNK_SIZE = 1024 * 1024;
    private static final int UPDATE_PROGRESS_INTERVAL = 1000;

    private ExecutorService executorService;
    private DownloadProgressPrinter progressPrinter;
    private PrintWriter logger;

    public YoutubeOrdinaryFileDownloader() {
        this.executorService = Executors.newFixedThreadPool(MAX_YOUTUBE_DOWNLOAD_THREADS);
        this.progressPrinter = new DownloadProgressPrinter(UPDATE_PROGRESS_INTERVAL);
    }

    public synchronized YoutubeResult download(String videoId, String downloadFilePrefix, String temporaryFilePath, Format format)
            throws IOException, InterruptedException, ExecutionException {
        YoutubeResult result = new YoutubeResult();

        logger = new PrintWriter("ordinary-file-downloading-logging.log");
        log("Content-Length = " + format.contentLength);

        String videoFilename = downloadFilePrefix + "." + format.fileExtension;
        File resultFile = new File(videoFilename);
        result.resultFile = resultFile;
        if (resultFile.exists()) {
            resultFile.delete();
        }

        String baseDownloadUrl = format.downloadURL;
        File tempFile = new File(temporaryFilePath);
        try (RandomAccessFile tempFileRAF = new RandomAccessFile(tempFile, "rw")) {
            long fileSize = format.contentLength;
            tempFileRAF.setLength(fileSize);
            progressPrinter.start(fileSize);

            List<Chunk> activeQueue = new ArrayList<>();
            List<Chunk> failedChunks = new ArrayList<>();
            long generatedTasksSize = 0;

            while (generatedTasksSize < fileSize || !activeQueue.isEmpty() || !failedChunks.isEmpty()) {
                long remainingSize = fileSize - generatedTasksSize;
                remainingSize += failedChunks.stream().mapToLong(e -> e.size).sum();
                int chunkSize = remainingSize >= MAX_CHUNK_SIZE * MAX_YOUTUBE_DOWNLOAD_THREADS ? MAX_CHUNK_SIZE :
                        remainingSize <= MIN_CHUNK_SIZE * MAX_YOUTUBE_DOWNLOAD_THREADS ? MIN_CHUNK_SIZE :
                        MathUtils.ceilToInt((double) remainingSize / MAX_YOUTUBE_DOWNLOAD_THREADS);

                // Refreshing Download URL
                if (generatedTasksSize < fileSize || !failedChunks.isEmpty()) {
                    baseDownloadUrl = getFreshDownloadUrl(videoId, format.iTag);
                    log("Refreshed link");
                }

                // Generating new Tasks party
                while (activeQueue.size() < MAX_YOUTUBE_DOWNLOAD_THREADS && (!failedChunks.isEmpty() || generatedTasksSize < fileSize)) {
                    Chunk chunk;
                    if (!failedChunks.isEmpty()) {
                        chunk = failedChunks.get(0);
                        failedChunks.remove(0);
                        log(chunk + " - recreating failed task");
                    } else {
                        chunkSize = (int) Math.min(chunkSize, fileSize - generatedTasksSize);
                        chunk = new Chunk(generatedTasksSize, chunkSize);
                        generatedTasksSize += chunkSize;
                        log(chunk + " - creating task");
                    }

                    DownloadTask task = createTask(baseDownloadUrl, chunk);
                    chunk.future = executorService.submit(task);
                    activeQueue.add(chunk);
                }

                // Checking completed Tasks
                while (activeQueue.size() > 0) {
                    Iterator<Chunk> iterator = activeQueue.iterator();
                    while (iterator.hasNext()) {
                        Chunk chunk = iterator.next();
                        if (!chunk.future.isDone()) {
                            continue;
                        }

                        Result chunkResult = chunk.future.get();
                        if (chunkResult.e != null || chunkResult.data == null || chunkResult.data.size() != chunk.size) {
                            failedChunks.add(chunk);
                            iterator.remove();
                            log(chunk + " failed, adding to failed tasks. [Response code=" + chunkResult.responseCode
                                    + ", e=" + (chunkResult.e == null ? null : chunkResult.e.getMessage())
                                    + ", data=" + (chunkResult.data == null ? null : chunkResult.data.size()));
                            continue;
                        }

                        // Saving data to the File
                        log(chunk + " - finished, saving to disk");
                        tempFileRAF.seek(chunk.start);
                        tempFileRAF.write(chunkResult.data.getStorage(), 0, chunk.size);
                        iterator.remove();
                        log(chunk + " - saved to disk");
                    }

                    // Printing progress
                    progressPrinter.printProgress(false);

                    // Sleeping
                    ThreadUtils.sleep(100);
                }
            }
            progressPrinter.printProgress(true);

            result.completed = true;
        }

        logger.close();

        result.completed &= FileUtils.moveFile(tempFile, resultFile);

        return result;
    }

    private void log(String message) {
        logger.println(new Date() + " " + message);
        logger.flush();
    }

    private String getFreshDownloadUrl(String videoId, int iTag) {
        boolean successful = false;
        String result = null;
        do {
            try {
                String urlString = "https://www.youtube.com/watch?v=" + videoId;
                DownloadUtils.Response response = DownloadUtils.downloadPageByGet(urlString, null);
                if (response.responseCode == 404) {
                    throw new IOException("404 Not found by refresihng download URL");
                } else if (response.responseCode != 200) {
                    throw new IOException("Error refreshing URL: \"" + urlString + "\", HTTP Status Code: " + response.responseCode);
                }
                Document videoPage = Jsoup.parse(response.body);

                // Fetching Video Details
                List<String> videoDetailsScriptSection = PageParsing.exctractSectionsFromPage(videoPage, "script", "var ytInitialPlayerResponse = ");
                if (videoDetailsScriptSection.size() > 1) {
                    throw new IllegalStateException("More than 1 section has been found when the only section is expected, probably Youtube API was changed");
                }
                String videoDetailsJsonString = JSONUtils.extractJsonSubstringFromString(videoDetailsScriptSection.get(0));
                JsonNode playerResponseNode = JSONUtils.parseJSON(videoDetailsJsonString);

                // Fetching Media Formats
                JsonNode streamingDataNode = playerResponseNode.get("streamingData");
                JsonNode adaptiveFormatsNode = streamingDataNode.get("adaptiveFormats");
                for (JsonNode formatNode : adaptiveFormatsNode) {
                    if (formatNode.get("itag").asInt() == iTag) {
                        result = formatNode.get("url").asText();
                        successful = true;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                ThreadUtils.sleep(5000);
            }
        } while (!successful || result == null || result.isEmpty());

        return result;
    }

    private DownloadTask createTask(String baseDownloadURL, Chunk chunk) {
        String downloadUrl = baseDownloadURL + "&range=" + chunk.start + "-" + (chunk.start + chunk.size - 1);
        DownloadTask task = new DownloadTask(downloadUrl, progressPrinter);
        return task;
    }

    private static final class Chunk {
        private long start;
        private int size;
        private Future<download.DownloadTask.Result> future;

        public Chunk(long start, int size) {
            this.start = start;
            this.size = size;
        }

        @Override
        public String toString() {
            return "Chunk[start=" + start + ", size=" + size + "]";
        }
    }
}
