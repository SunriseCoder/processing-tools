package backuper.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.message.BasicNameValuePair;

import backuper.client.config.CopySettings;
import backuper.client.config.RemoteResource;
import backuper.common.helpers.HttpHelper;
import backuper.common.helpers.HttpHelper.Response;
import utils.FormattingUtils;
import utils.MathUtils;
import utils.ThreadUtils;
import wrappers.ThreadSafeBooleanWrapper;

public class BenchmarkRemote {
    private static final String BENCHMARK_RESOURCE_URL = "benchmark";

    public static synchronized CopySettings doBenchmark(RemoteResource remoteResource) {
        System.out.println("Calculating Copy Chunk Size...");
        int copyChunkSize = calculateCopyChunkSize(remoteResource);
        int maxConnections = calculateMaxConnections(remoteResource, copyChunkSize);

        CopySettings copySettings = new CopySettings();
        copySettings.setCopyChunkSize(copyChunkSize);
        copySettings.setMaxConnections(maxConnections);

        return copySettings;
    }

    private static int calculateCopyChunkSize(RemoteResource remoteResource) {
        int chunkSize = 0;

        for (int p = 12; p <= 22; p++) {
            chunkSize = MathUtils.roundToInt(Math.pow(2, p));

            System.out.print("Testing " + FormattingUtils.humanReadableSize(chunkSize) + "b: ");

            long startTime;
            boolean success;
            do {
                startTime = System.currentTimeMillis();
                try {
                    for (int i = 0; i < 10; i++) {
                        doRequest(remoteResource, chunkSize);
                    }
                    success = true;
                } catch (Exception e) {
                    success = false;
                }
            } while (!success);

            long endTime = System.currentTimeMillis();
            long averateTime = MathUtils.roundToLong((double) (endTime - startTime) / 10);
            System.out.println(" done, average time is: " + FormattingUtils.humanReadableTimeMS(averateTime));
            if (averateTime > 5000) {
                System.out.println("This is a good value, using Chunk Size: " + FormattingUtils.humanReadableSize(chunkSize));
                break;
            }
        }

        return chunkSize;
    }

    private static int calculateMaxConnections(RemoteResource remoteResource, int copyChunkSize) {
        int[] theadCounts = new int[] { 3, 5, 10, 20, 30, 50, 75, 100, 150, 200 };

        int lastStable = theadCounts[0];
        long lastSpeed = 1;

        for (int threadCount : theadCounts) {
            long currentSpeed = performMultithreadingBenchmark(remoteResource, threadCount, copyChunkSize);
            if (currentSpeed == 0) {
                System.out.println("Some connection problems occured with this test, using the previous values");
                break;
            }

            double speedIncrease = (double) currentSpeed / lastSpeed;
            if (speedIncrease < 1.2) {
                System.out.println("Too small speed increase, using the previous value: " + lastStable + " Threads");
                break;
            }

            lastStable = threadCount;
            lastSpeed = currentSpeed;
        }

        return lastStable;
    }

    private static long performMultithreadingBenchmark(RemoteResource remoteResource, int threadCount, int copyChunkSize) {
        StringBuilder sb = new StringBuilder();
        sb.append("Starting 1+ minute test using ").append(threadCount).append(" Threads and Chunk Size ")
                .append(FormattingUtils.humanReadableSize(copyChunkSize)).append("b ...");
        System.out.println(sb);
        long speed = 0;

        for (int attempt = 0; attempt < 10; attempt++) {
            System.out.print("attempt " + (attempt + 1) + " ");

            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            List<Future<?>> futures = new ArrayList<>();
            FileCopyStatus fileCopyStatus = new FileCopyStatus();
            fileCopyStatus.reset();
            long startTime = System.currentTimeMillis();
            ThreadSafeBooleanWrapper success = new ThreadSafeBooleanWrapper(true);
            boolean done = false;
            while (!done || !futures.isEmpty()) {
                // Checking if the time is over
                if (!done) {
                    done = System.currentTimeMillis() - startTime >= 60000;
                }

                // Removing finished Futures
                Iterator<Future<?>> futuresIterator = futures.iterator();
                while (futuresIterator.hasNext()) {
                    Future<?> future = futuresIterator.next();
                    if (future.isDone()) {
                        futuresIterator.remove();
                    }
                }

                // Adding new Futures
                if (success.value() && !done) {
                    for (int i = futures.size(); i < threadCount * 2; i++) {
                        Future<?> future = executor.submit(() -> {
                            try {
                                doRequest(remoteResource, copyChunkSize);
                                fileCopyStatus.addCopiedSize(copyChunkSize);
                                System.out.print(".");
                            } catch (Exception e) {
                                success.setValue(false);
                            }
                        });
                        futures.add(future);
                    }
                }

                ThreadUtils.sleep(10);
            }

            executor.shutdown();

            if (success.value()) {
                long now = System.currentTimeMillis();
                long duration = now - fileCopyStatus.getCurrentFileStartTime();
                speed = MathUtils.roundToLong((double) fileCopyStatus.getAllFilesCopiedSize() * 1000 /  duration);
                System.out.println(" Successful. The speed is " + FormattingUtils.humanReadableSize(speed) + "b/s");
                break;
            } else {
                System.out.println(" Failed due to data transfer errors");
            }
        }

        return speed;
    }

    private static void doRequest(RemoteResource remoteResource, int chunkSize) throws IOException, HttpException {
        Random rnd = new Random();

        List<NameValuePair> postData = new ArrayList<>();
        postData.add(new BasicNameValuePair("resource", BENCHMARK_RESOURCE_URL));
        postData.add(new BasicNameValuePair("token", remoteResource.getToken()));
        postData.add(new BasicNameValuePair("path", UUID.randomUUID().toString()));
        postData.add(new BasicNameValuePair("start", String.valueOf(rnd.nextInt(Integer.MAX_VALUE))));
        postData.add(new BasicNameValuePair("length", String.valueOf(chunkSize)));

        String requestUrl = remoteResource.getServerUrl() + BENCHMARK_RESOURCE_URL;
        Response response = HttpHelper.sendPostRequest(requestUrl, postData);
        if (response == null || (response != null && response.getCode() != 200)) {
            throw new HttpException();
        }
    }
}
