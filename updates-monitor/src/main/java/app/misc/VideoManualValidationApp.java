package app.misc;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import org.jboss.com.sun.net.httpserver.HttpServer;

import core.http.HttpServerAjaxActionHandler;
import core.http.HttpServerAjaxListHandler;
import core.http.HttpServerPreviewResourceHandler;
import core.http.HttpServerResourceHandler;

public class VideoManualValidationApp {

    public static void main(String[] args) throws IOException {
        int port = 8000;
        System.out.print("Starting Server on http://localhost:" + port + "/ ... ");

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/", new HttpServerResourceHandler());
        server.createContext("/preview/", new HttpServerPreviewResourceHandler());
        server.createContext("/ajax_list", new HttpServerAjaxListHandler());
        server.createContext("/ajax_action", new HttpServerAjaxActionHandler());

        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();

        System.out.println(" Started");
    }
}
