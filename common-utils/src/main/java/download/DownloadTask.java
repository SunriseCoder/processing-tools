package download;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Callable;

import adaptors.ByteArray;

public class DownloadTask implements Callable<download.DownloadTask.Result> {
    private String downloadUrl;
    private SimpleProgressListener progressListener;
    private long reportedSize;

    public DownloadTask(String downloadUrl, SimpleProgressListener progressListener) {
        this.downloadUrl = downloadUrl;
        this.progressListener = progressListener;
        this.reportedSize = 0;
    }

    @Override
    public Result call() throws Exception {
        Result result = new Result();

        try {
            URL url = new URL(downloadUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(5 * 1000);
            connection.setReadTimeout(10 * 1000);
            int responseCode = connection.getResponseCode();
            result.responseCode = responseCode;
            if (responseCode != 200) {
                return result;
            }

            int contentLength = connection.getContentLength();
            result.contentLength = contentLength;

            ByteArray byteArray = contentLength > 0 ? new ByteArray(contentLength) : new ByteArray();
            byte[] buffer = new byte[4096];
            try (InputStream is = connection.getInputStream()) {
                int read = 0;
                do {
                    read = is.read(buffer);
                    if (read > 0) {
                        byteArray.append(buffer, 0, read);
                        if (progressListener != null) {
                            progressListener.progress(read);
                            reportedSize += read;
                        }
                    }
                } while (read != -1);
            } catch (Exception e) {
                if (progressListener != null) {
                    progressListener.progress(-reportedSize);
                }
                result.e = e;
            }
            result.data = byteArray;
        } catch (Exception e) {
            if (progressListener != null) {
                progressListener.progress(-reportedSize);
            }
            result.e = e;
        }

        return result;
    }

    public static class Result {
        public int responseCode;
        public long contentLength;
        public ByteArray data;
        public Exception e;
    }
}
