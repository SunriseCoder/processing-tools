package core.http;

import java.io.IOException;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

@SuppressWarnings("restriction")
public class NotFoundHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        //System.out.println("Page not found: " + exchange.getRequestURI());
        exchange.sendResponseHeaders(404, 0);
        exchange.getResponseBody().close();
    }
}
