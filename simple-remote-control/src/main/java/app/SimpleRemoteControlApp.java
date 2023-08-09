package app;

import java.io.IOException;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpServer;

import core.Configuration;
import core.http.AjaxHandler;
import core.http.IndexHandler;
import core.http.NotFoundHandler;

@SuppressWarnings("restriction")
public class SimpleRemoteControlApp {

    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(Configuration.getParameter("port"));
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);

        server.createContext("/", new  IndexHandler());
        server.createContext("/ajax", new  AjaxHandler());
        server.createContext("/favicon.ico", new  NotFoundHandler());

        //server.setExecutor(threadPoolExecutor);

        server.start();

        System.out.println("Server started on port " + port);
    }
}
