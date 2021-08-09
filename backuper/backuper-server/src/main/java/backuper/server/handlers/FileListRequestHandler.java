package backuper.server.handlers;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import backuper.common.dto.FileMetadataRemote;
import backuper.common.helpers.HttpHelper;
import backuper.server.FileServer;
import utils.JSONUtils;
import utils.NumberUtils;

@SuppressWarnings("restriction")
public class FileListRequestHandler implements HttpHandler {
    private FileServer fileServer;

    public FileListRequestHandler(FileServer fileServer) {
        this.fileServer = fileServer;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!HttpHelper.validateMethod(exchange, "POST")) {
            return;
        }

        Map<String, String> params = HttpHelper.parsePostParams(exchange);
        if (!HttpHelper.validateNonEmptyParams(params, new String[] { "resource", "token" }, exchange)) {
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

        System.out.println("Request: " + params.get("resource") + ", " + fileServer.getUserByToken(params.get("token")).getLogin());

        if (!fileServer.hasAccess(resource, token)) {
            HttpHelper.sendResponse(exchange, 403, "Access denied");
            return;
        }

        List<FileMetadataRemote> fileList = fileServer.getFileList(resource, token);
        String json = JSONUtils.toJSON(fileList);
        HttpHelper.sendResponse(exchange, 200, json);

        System.out.println("FileList - " + resource + " - " + fileList.size() + " file(s) - " + NumberUtils.humanReadableSize(json.length()) + "b");
    }
}
