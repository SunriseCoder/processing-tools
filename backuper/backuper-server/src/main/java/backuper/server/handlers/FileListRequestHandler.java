package backuper.server.handlers;

import java.io.IOException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import backuper.server.FileServer;
import backuper.server.helpers.StreamHelper;
import utils.JSONUtils;

@SuppressWarnings("restriction")
public class FileListRequestHandler implements HttpHandler {

    public FileListRequestHandler(FileServer fileServer) {
        // TODO Auto-generated constructor stub
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // TODO Auto-generated method stub
        String requestBody = StreamHelper.readFromInputStream(exchange.getRequestBody());
        FileListRequest request = JSONUtils.parseJSON(requestBody, new TypeReference<FileListRequest>() {});
        System.out.println("Hello!");
    }

    private static class FileListRequest {
        String resource;
        String token;

        public FileListRequest() {}

        public String getResource() {
            return resource;
        }

        public void setResource(String resource) {
            this.resource = resource;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }
    }
}
