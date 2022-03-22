package core.youtube;

import java.io.File;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import core.dto.youtube.YoutubeDownloadDetails;
import core.dto.youtube.YoutubeResult;
import core.dto.youtube.YoutubeVideo;
import download.DownloadProgressPrinter;
import download.DownloadTask;
import download.DownloadTask.Result;
import utils.MathUtils;
import utils.ThreadUtils;

public abstract class AbstractYoutubeFileDownloader {
    protected static final int MAX_YOUTUBE_DOWNLOAD_THREADS = 100;
    protected static final int MIN_CHUNK_SIZE = 64 * 1024;
    protected static final int MAX_CHUNK_SIZE = 1024 * 1024;
    protected static final int UPDATE_PROGRESS_INTERVAL = 1000;

    protected ExecutorService executorService;
    protected DownloadProgressPrinter progressPrinter;
    protected PrintWriter logger;

    protected AbstractYoutubeFileDownloader() {
        this.executorService = Executors.newFixedThreadPool(MAX_YOUTUBE_DOWNLOAD_THREADS);
        this.progressPrinter = new DownloadProgressPrinter(UPDATE_PROGRESS_INTERVAL);
    }

    protected abstract YoutubeResult download(YoutubeVideo video, YoutubeDownloadDetails downloadDetails) throws Exception;

    protected boolean downloadFile(File file, long fileSize, String videoId, int iTag) throws Exception {
        boolean result = false;

        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            raf.setLength(fileSize);
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
                String baseDownloadUrl = null;
                if (generatedTasksSize < fileSize || !failedChunks.isEmpty()) {
                    baseDownloadUrl = getFreshDownloadUrl(videoId, iTag);
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
                        raf.seek(chunk.start);
                        raf.write(chunkResult.data.getStorage(), 0, chunk.size);
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

            result = true;
        }

        return result;
    }

    protected DownloadTask createTask(String baseDownloadURL, Chunk chunk) {
        String downloadUrl = baseDownloadURL + "&range=" + chunk.start + "-" + (chunk.start + chunk.size - 1);
        DownloadTask task = new DownloadTask(downloadUrl, progressPrinter);
        return task;
    }

    protected abstract String getFreshDownloadUrl(String videoId, int iTag);

    protected static final class Chunk {
        protected long start;
        protected int size;
        protected Future<download.DownloadTask.Result> future;

        public Chunk(long start, int size) {
            this.start = start;
            this.size = size;
        }

        @Override
        public String toString() {
            return "Chunk[start=" + start + ", size=" + size + "]";
        }
    }

    protected void log(String message) {
        logger.println(new Date() + " " + message);
        logger.flush();
    }
}
