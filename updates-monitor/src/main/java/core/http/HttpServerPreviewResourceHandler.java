package core.http;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.jboss.com.sun.net.httpserver.HttpExchange;
import org.jboss.com.sun.net.httpserver.HttpHandler;

public class HttpServerPreviewResourceHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        System.out.println("Requested Preview: " + exchange.getRequestURI().getPath());

        // Resource location
        String resourceName = "." + exchange.getRequestURI().getPath();

        // Looking for the Resource
        File resourceFile = new File(resourceName);
        if (!resourceFile.exists()) {
            exchange.sendResponseHeaders(404, 0);
            return;
        }

        // Sending Content-Length
        long contentLength = resourceFile.length();
        exchange.sendResponseHeaders(200, contentLength);

        // Copy data from File to Response
        try (InputStream inputStream = new FileInputStream(resourceFile)) {
            OutputStream outputStream = exchange.getResponseBody();
            byte[] buffer = new byte[4096];
            int read;
            do {
                read = inputStream.read(buffer);
                if (read > 0) {
                    outputStream.write(buffer, 0, read);
                }
            } while (read > 0);
            outputStream.flush();
        }
        exchange.close();
    }
}
