package core.http;

import java.io.File;
import java.io.IOException;

import org.jboss.com.sun.net.httpserver.HttpExchange;
import org.jboss.com.sun.net.httpserver.HttpHandler;

import com.fasterxml.jackson.databind.JsonNode;

import utils.JSONUtils;

public class HttpServerAjaxListHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        System.out.println("Requested Ajax: " + exchange.getRequestURI().getPath());

        JsonNode json = JSONUtils.loadFromDisk(new File("database/previews.json"));
        String jsonString = json.toString();
        byte[] jsonBytes = jsonString.getBytes();

        exchange.sendResponseHeaders(200, jsonBytes.length);

        exchange.getResponseBody().write(jsonBytes);
        exchange.getResponseBody().flush();

        exchange.close();
    }
}
