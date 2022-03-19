package app.misc;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class DownloadFileBenchmarkApp {

    public static void main(String[] args) throws IOException {
        System.out.print("Enter Downlaod URL: ");
        Scanner scanner = new Scanner(System.in);
        String input = scanner.next();

        URL url = new URL(input);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(10 * 1000);
        connection.setReadTimeout(60 * 1000);
        int responseCode = connection.getResponseCode();
        if (responseCode != 200) {
            System.out.println("Response code: " + responseCode);
            System.exit(-1);
        }
        long contentLength = connection.getContentLengthLong();
        System.out.println("Response code: " + responseCode + ", Content-length: " + contentLength);

        byte[] buffer = new byte[4096];
        try (InputStream is = connection.getInputStream()) {
            long totalRead = 0;
            long reportedRead = 0;
            long startTime = System.currentTimeMillis();
            long lastTimeReport = startTime;
            long now;
            int read = 0;
            while (read != -1) {
                read = is.read(buffer);
                if (read > 0) {
                    totalRead += read;
                }
                now = System.currentTimeMillis();
                long timeDelta = now - lastTimeReport;
                if (timeDelta >= 100) {
                    System.out.println("Time delta: " + timeDelta + ", Read: " + (totalRead - reportedRead));
                    reportedRead = totalRead;
                    lastTimeReport = now;
                }

                long timeBigDelta = now - startTime;
                if (timeBigDelta >= 5000) {
                    System.out.println("Time big delta: " + timeBigDelta + ", Read: " + totalRead);
                    System.exit(0);
                }
            }
        }

        System.out.println("\nProgram is done, type anything and then press Enter to close");
        scanner.next();
        scanner.close();
    }
}
