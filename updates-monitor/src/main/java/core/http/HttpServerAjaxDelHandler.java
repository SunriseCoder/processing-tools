package core.http;

import java.io.IOException;

import org.jboss.com.sun.net.httpserver.HttpExchange;
import org.jboss.com.sun.net.httpserver.HttpHandler;

import utils.HttpUtils;

public class HttpServerAjaxDelHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        System.out.println("Requested Ajax: " + exchange.getRequestURI().getPath());

        String query = exchange.getRequestURI().getQuery();
        String id = HttpUtils.extractGetParameterValue(query, "id");

        PreviewUtils.addToDelete(id);
        PreviewUtils.setProcessed(id);

        exchange.sendResponseHeaders(200, 0);
        exchange.close();
    }
}
