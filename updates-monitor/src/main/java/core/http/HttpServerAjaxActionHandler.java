package core.http;

import java.io.IOException;

import org.jboss.com.sun.net.httpserver.HttpExchange;
import org.jboss.com.sun.net.httpserver.HttpHandler;

import utils.HttpUtils;

public class HttpServerAjaxActionHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        System.out.println("Requested Ajax Action: " + exchange.getRequestURI().getPath());

        String query = exchange.getRequestURI().getQuery();
        String action = HttpUtils.extractGetParameterValue(query, "action");
        String id = HttpUtils.extractGetParameterValue(query, "id");

        PreviewUtils.addToList(action, id);
        PreviewUtils.setProcessed(id);

        exchange.sendResponseHeaders(200, 0);
        exchange.close();
    }
}
