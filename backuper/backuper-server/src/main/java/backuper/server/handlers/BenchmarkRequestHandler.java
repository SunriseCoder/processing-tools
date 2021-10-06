package backuper.server.handlers;

import java.io.IOException;
import java.util.Map;
import java.util.Random;

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
import utils.FormattingUtils;

public class BenchmarkRequestHandler implements AsyncServerRequestHandler<Message<HttpRequest, byte[]>> {
    private FileServer fileServer;
    private Random random;

    public BenchmarkRequestHandler(FileServer fileServer) {
        this.fileServer = fileServer;
        this.random = new Random();
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
        if (!HttpHelper.validateNonEmptyParams(params, new String[] { "token", "length" }, responseTrigger, context)) {
            return;
        }

        String token = params.get("token");
        if (!fileServer.hasToken(token)) {
            HttpHelper.sendHttpResponse(403, "Invalid token", ContentType.TEXT_HTML, responseTrigger, context);
            return;
        }

        int length = 0;
        try {
            length = Integer.parseInt(params.get("length"));
        } catch (Exception e) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Request: ").append(fileServer.getUserByToken(params.get("token")).getLogin())
                .append(": Benchmark: ").append(FormattingUtils.humanReadableSize(length)).append(" (")
                .append(length).append(" bytes) ...");
        System.out.println(sb);

        byte[] fileData = new byte[length];
        random.nextBytes(fileData);

        HttpHelper.sendHttpResponse(200, fileData, ContentType.DEFAULT_BINARY, responseTrigger, context);
    }
}
