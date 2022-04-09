package util;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import adaptors.ByteArray;

public class DownloadUtils {

    public static Response downloadPageByGet(String urlString, Map<String, String> headers, String cookie) throws IOException {
        Response response = new Response();

        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        if (headers != null) {
            for (Entry<String, String> headerEntry : headers.entrySet()) {
                connection.setRequestProperty(headerEntry.getKey(), headerEntry.getValue());
            }
        }
        if (cookie != null) {
            connection.setRequestProperty("Cookie", cookie);
        }
        response.responseCode = connection.getResponseCode();
        response.headers = connection.getHeaderFields();
        if (response.responseCode != 200) {
            return response;
        }

        ByteArray byteArray = new ByteArray();
        try (InputStream is = connection.getInputStream();) {
            int read = 0;
            byte[] buffer = new byte[4096];
            while (read > -1) {
                read = is.read(buffer);
                if (read > 0) {
                    byteArray.append(buffer, 0, read);
                }
            }
        }

        response.body = byteArray.createString("UTF-8");
        connection.disconnect();
        return response;
    }

    public static Response downloadPageByPostJson(String urlString, String jsonBody) throws IOException {
        Response response = new Response();
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(60 * 1000);
        connection.setReadTimeout(60 * 1000);
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Content-Length", String.valueOf(jsonBody.length()));
        connection.setUseCaches(false);

        connection.getOutputStream().write(jsonBody.getBytes());
        response.responseCode = connection.getResponseCode();
        if (response.responseCode != 200) {
            return response;
        }

        ByteArray byteArray = new ByteArray();
        try (InputStream is = connection.getInputStream();) {
            int read = 0;
            byte[] buffer = new byte[65536];
            while (read > -1) {
                read = is.read(buffer);
                if (read > 0) {
                    byteArray.append(buffer, 0, read);
                }
            }
        }

        response.responseCode = connection.getResponseCode();
        response.headers = connection.getHeaderFields();
        response.body = byteArray.createString("UTF-8");
        connection.disconnect();
        return response;
    }

    public static class Response {
        public int responseCode;
        public Map<String, List<String>> headers;
        public String body;
    }

    public static long getContentLength(String downloadUrl) throws IOException {
        URL url = new URL(downloadUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        int responseCode = connection.getResponseCode();
        if (responseCode != 200) {
            throw new IOException("HTTP Response Code: " + responseCode);
        }

        long contentLength = connection.getContentLengthLong();
        connection.disconnect();
        return contentLength;
    }
}
