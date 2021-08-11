package backuper.server.handlers;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.Message;
import org.apache.hc.core5.http.nio.AsyncRequestConsumer;
import org.apache.hc.core5.http.nio.AsyncServerRequestHandler;
import org.apache.hc.core5.http.nio.entity.BasicAsyncEntityConsumer;
import org.apache.hc.core5.http.nio.support.BasicRequestConsumer;
import org.apache.hc.core5.http.protocol.HttpContext;

import backuper.common.dto.FileMetadataRemote;
import backuper.common.helpers.HttpHelper;
import backuper.server.FileServer;
import utils.JSONUtils;
import utils.NumberUtils;

public class FileListRequestHandler implements AsyncServerRequestHandler<Message<HttpRequest, byte[]>> {
    private FileServer fileServer;

    public FileListRequestHandler(FileServer fileServer) {
        this.fileServer = fileServer;
    }

    @Override
    public AsyncRequestConsumer<Message<HttpRequest, byte[]>> prepare(
            HttpRequest request, EntityDetails entityDetails, HttpContext context) throws HttpException {
        return new BasicRequestConsumer<>(new BasicAsyncEntityConsumer());
    }

    @Override
    public void handle(Message<HttpRequest, byte[]> message, ResponseTrigger responseTrigger, HttpContext context) throws HttpException, IOException {
        HttpRequest request = message.getHead();

        if (!HttpHelper.validateRequestMethod(request, "POST", responseTrigger, context)) {
            return;
        }

        Map<String, String> params = HttpHelper.parsePostParams(new String(message.getBody()));
        if (!HttpHelper.validateNonEmptyParams(params, new String[] { "resource", "token" }, responseTrigger, context)) {
            return;
        }

        String resourceName = params.get("resource");
        if (!fileServer.hasResource(resourceName)) {
            HttpHelper.sendHttpResponse(404, "Resorce not found", ContentType.TEXT_HTML, responseTrigger, context);
            return;
        }

        String token = params.get("token");
        if (!fileServer.hasToken(token)) {
            HttpHelper.sendHttpResponse(403, "Invalid token", ContentType.TEXT_HTML, responseTrigger, context);
            return;
        }

        System.out.println("Request: " + fileServer.getUserByToken(params.get("token")).getLogin() + ": " + resourceName);

        if (!fileServer.hasAccess(resourceName, token)) {
            HttpHelper.sendHttpResponse(403, "Access denied", ContentType.TEXT_HTML, responseTrigger, context);
            return;
        }

        List<FileMetadataRemote> fileList = fileServer.getFileList(resourceName, token);
        String json = JSONUtils.toJSON(fileList);
        json += "\n";
        HttpHelper.sendHttpResponse(200, json, ContentType.APPLICATION_JSON, responseTrigger, context);

        System.out.println("FileList - " + resourceName + " - " + fileList.size() + " file(s) - " + NumberUtils.humanReadableSize(json.length()) + "b");
    }
}
