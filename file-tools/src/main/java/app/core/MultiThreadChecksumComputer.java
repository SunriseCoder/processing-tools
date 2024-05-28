package app.core;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import app.core.dto.FileMetadata;
import app.digest.XorProvider;
import app.structures.Pair;
import app.utils.FormattingUtils;
import app.utils.MathUtils;
import app.utils.ThreadUtils;

public class MultiThreadChecksumComputer {
    private List<String> checksumAlgorithms;

    public MultiThreadChecksumComputer(List<String> checksumAlgorithms) {
        this.checksumAlgorithms = checksumAlgorithms;

        Security.addProvider(new XorProvider());
    }

    public void computeChecksums(List<FileMetadata> files) throws NoSuchAlgorithmException {
        Map<String, List<FileMetadata>> filesMap = new HashMap<>();

        for (FileMetadata file : files) {
            Path path = Paths.get(file.getAbsolutePath());
            String key = path.getRoot().toString();

            List<FileMetadata> fileList = filesMap.get(key);
            if (fileList == null) {
                fileList = new ArrayList<>();
                filesMap.put(key, fileList);
            }
            fileList.add(file);
        }

        List<Entry<String, List<FileMetadata>>> fileLists = filesMap.entrySet().stream()
                .sorted((a, b) -> a.getKey().compareTo(b.getKey()))
                .collect(Collectors.toList());

        int numberOfThreads = filesMap.size();
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);

        List<Pair<ComputeChecksumTask, Future<?>>> tasks = new ArrayList<>();
        for (Entry<String, List<FileMetadata>> entry : fileLists) {
            ComputeChecksumTask task = new ComputeChecksumTask(checksumAlgorithms, entry.getKey(), entry.getValue());
            Future<?> future = executorService.submit(task);
            tasks.add(new Pair<>(task, future));
        }

        boolean finished = false;
        //int lastLineLength = 0;
        do {
            ThreadUtils.sleep(1000);

            StringBuilder sb = new StringBuilder();
            finished = true;
            for (Pair<ComputeChecksumTask, Future<?>> task : tasks) {
                if (!task.getValue().isDone()) {
                    sb.append(task.getKey().getStatistics()).append(" | ");
                    finished = false;
                }
            }
            //int currentLineLength = sb.length();
            //StringUtils.appendStringBuilderAtTheEnd(sb, " ", lastLineLength);
            System.out.println(sb.toString());
            //lastLineLength = currentLineLength;
        } while (!finished);
    }

    private static class ComputeChecksumTask implements Runnable {
        private static final Logger LOGGER = LogManager.getLogger(ComputeChecksumTask.class);

        private String fsRoot;
        private List<FileMetadata> fileList;
        private final AtomicLong totalFileSize;
        private final AtomicLong totalProgress;
        private final AtomicLong startTime;

        private Map<String, MessageDigest> messageDigests;

        private ComputeChecksumTask(List<String> checksumAlgorithms, String fsRoot, List<FileMetadata> fileList) throws NoSuchAlgorithmException {
            this.fsRoot = fsRoot;
            this.fileList = fileList;
            totalFileSize = new AtomicLong(fileList.stream().mapToLong(m -> m.getSize()).sum());
            totalProgress = new AtomicLong(0);
            startTime = new AtomicLong(0);

            messageDigests = new HashMap<>();
            for (String algorithm : checksumAlgorithms) {
                MessageDigest messageDigest = MessageDigest.getInstance(algorithm);
                messageDigest.reset();
                messageDigests.put(algorithm, messageDigest);
            }
        }

        @Override
        public void run() {
            LOGGER.info("Starting Compute Checksums Task for " + fsRoot);

            startTime.set(System.currentTimeMillis());

            for (FileMetadata file : fileList) {
                LOGGER.debug("Starting ComputeChecksum " + fsRoot + " for file ("
                        + FormattingUtils.humanReadableSizeBi(file.getSize()) + "): " + file.getAbsolutePath());
                long currentProgress = totalProgress.get();
                try {
                    computeFileChecksum(file);
                    LOGGER.debug("ComputeChecksum is successful for " + fsRoot + " for file: " + file.getAbsolutePath());
                } catch (IOException e) {
                    String message = "Error due to Computing Checksums for file: " + file.getAbsolutePath();
                    LOGGER.error(message, e);
                    totalProgress.set(currentProgress + file.getSize());
                }
            }
        }

        private void computeFileChecksum(FileMetadata fileMetadata) throws IOException {
            File file = new File(fileMetadata.getAbsolutePath());
            try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(file))) {
                long fileSize = file.length();
                byte[] buffer = new byte[128 * 1024];
                long currentFileReadTotal = 0;
                while (currentFileReadTotal < fileSize) {
                    // Reading Data from Disk
                    int read = in.read(buffer);
                    currentFileReadTotal += read;

                    // Adding Data to all MessageDigests
                    for (MessageDigest messageDigest : messageDigests.values()) {
                        messageDigest.update(buffer, 0, read);
                    }

                    // Add Read Size to Statistics
                    totalProgress.addAndGet(read);
                }

                // Finalizing MessageDigests and putting Checksums to FileMetadata
                Map<String, String> checksums = new HashMap<>();
                for (Entry<String, MessageDigest> messageDigestEntry : messageDigests.entrySet()) {
                    String algorithm = messageDigestEntry.getKey();
                    MessageDigest messageDigest = messageDigestEntry.getValue();
                    String checksum = new BigInteger(1, messageDigest.digest()).toString(16);
                    checksums.put(algorithm, checksum);
                }

                fileMetadata.addChecksums(checksums);
                fileMetadata.suggestSave();
            }
        }

        // D:\ - 1.3Tb of 7.5Tb - 240Mb/s - ETA: 1:12:25:42
        public String getStatistics() {
            StringBuilder sb = new StringBuilder();

            // FS Root
            sb.append(fsRoot).append(" - ");

            // Progress
            sb.append(FormattingUtils.humanReadableSizeBi(totalFileSize.get() - totalProgress.get())).append("B - ");

            // Speed
            long now = System.currentTimeMillis();
            long duration = now - startTime.get();
            if (duration < 1) {
                sb.append("???B/s - ETA: ???");
                return sb.toString();
            }

            long progress = totalProgress.get();
            long speed = MathUtils.roundToLong((double) 1000 * progress / duration);
            sb.append(FormattingUtils.humanReadableSizeBi(speed)).append("B/s - ");

            // ETA
            if (progress < 1) {
                sb.append("???");
                return sb.toString();
            }

            long eta = MathUtils.roundToLong((double) (totalFileSize.get() - totalProgress.get()) / speed);
            sb.append(FormattingUtils.humanReadableTimeS(eta));

            return sb.toString();
        }
    }
}
