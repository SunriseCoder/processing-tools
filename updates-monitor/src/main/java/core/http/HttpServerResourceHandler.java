package core.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import org.jboss.com.sun.net.httpserver.HttpExchange;
import org.jboss.com.sun.net.httpserver.HttpHandler;

public class HttpServerResourceHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        System.out.println("Requested Resource: " + exchange.getRequestURI().getPath());

        // Resource location
        String resourceName = "/".equals(exchange.getRequestURI().getPath())
                ? "/index.html" : exchange.getRequestURI().getPath();
        resourceName = "web/validation/manual" + resourceName;

        // Looking for the Resource
        URL systemResource = ClassLoader.getSystemResource(resourceName);
        if (systemResource == null) {
            exchange.sendResponseHeaders(404, 0);
            return;
        }

        // Sending Content-Length
        long contentLength = systemResource.openConnection().getContentLengthLong();
        exchange.sendResponseHeaders(200, contentLength);

        // Copy data from File to Response
        InputStream inputStream = systemResource.openStream();
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
        exchange.close();
    }
}
