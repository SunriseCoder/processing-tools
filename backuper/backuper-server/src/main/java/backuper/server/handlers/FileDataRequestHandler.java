package backuper.server.handlers;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import backuper.common.helpers.HttpHelper;
import backuper.server.FileServer;

@SuppressWarnings("restriction")
public class FileDataRequestHandler implements HttpHandler {
    private FileServer fileServer;

    public FileDataRequestHandler(FileServer fileServer) {
        this.fileServer = fileServer;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!HttpHelper.validateMethod(exchange, "POST")) {
            return;
        }

        Map<String, String> params = HttpHelper.parsePostParams(exchange);
        if (!HttpHelper.validateNonEmptyParams(params, new String[] { "resource", "token", "path", "start", "length" }, exchange)) {
            return;
        }

        String resource = params.get("resource");
        if (!fileServer.hasResource(resource)) {
            HttpHelper.sendResponse(exchange, 404, "Resorce not found");
            return;
        }

        String token = params.get("token");
        if (!fileServer.hasToken(token)) {
            HttpHelper.sendResponse(exchange, 403, "Invalid token");
            return;
        }

        if (!fileServer.hasAccess(resource, token)) {
            HttpHelper.sendResponse(exchange, 403, "Access denied");
            return;
        }

        String path = params.get("path");
        long start = Long.parseLong(params.get("start"));
        int length = Integer.parseInt(params.get("length"));
        byte[] fileData = fileServer.getFileData(resource, path, start, length);

        exchange.sendResponseHeaders(200, fileData.length);
        OutputStream response = exchange.getResponseBody();
        response.write(fileData);
        response.close();
    }
}
