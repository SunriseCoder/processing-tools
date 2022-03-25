package app.misc;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import org.jboss.com.sun.net.httpserver.HttpServer;

import core.http.HttpServerAjaxDelHandler;
import core.http.HttpServerAjaxKeepHandler;
import core.http.HttpServerAjaxListHandler;
import core.http.HttpServerPreviewResourceHandler;
import core.http.HttpServerResourceHandler;

public class VideoManualValidationApp {

    public static void main(String[] args) throws IOException {
        System.out.print("Starting Server... ");

        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);

        server.createContext("/", new HttpServerResourceHandler());
        server.createContext("/preview/", new HttpServerPreviewResourceHandler());
        server.createContext("/ajax_list", new HttpServerAjaxListHandler());
        server.createContext("/ajax_keep", new HttpServerAjaxKeepHandler());
        server.createContext("/ajax_del", new HttpServerAjaxDelHandler());

        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();

        System.out.println(" Started");
    }
}
