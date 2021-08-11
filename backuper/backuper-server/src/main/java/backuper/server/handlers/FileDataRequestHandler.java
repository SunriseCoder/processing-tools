package backuper.server.handlers;

import java.io.IOException;
import java.net.URLDecoder;
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

import backuper.common.helpers.HttpHelper;
import backuper.server.FileServer;

public class FileDataRequestHandler implements AsyncServerRequestHandler<Message<HttpRequest, byte[]>> {
    private FileServer fileServer;

    public FileDataRequestHandler(FileServer fileServer) {
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
        if (!HttpHelper.validateNonEmptyParams(params, new String[] { "resource", "token", "path", "start", "length" }, responseTrigger, context)) {
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

        System.out.println("Request: " + fileServer.getUserByToken(params.get("token")).getLogin() + ": " + resourceName
                + " - " + params.get("path") + ", start: " + params.get("start") + ", length: " + params.get("length") + "... ");

        if (!fileServer.hasAccess(resourceName, token)) {
            HttpHelper.sendHttpResponse(403, "Access denied", ContentType.TEXT_HTML, responseTrigger, context);
            return;
        }

        String path = URLDecoder.decode(params.get("path"), "UTF-8");
        long start = Long.parseLong(params.get("start"));
        int length = Integer.parseInt(params.get("length"));
        byte[] fileData = fileServer.getFileData(resourceName, path, start, length);

        HttpHelper.sendHttpResponse(200, fileData, ContentType.DEFAULT_BINARY, responseTrigger, context);
    }
}
