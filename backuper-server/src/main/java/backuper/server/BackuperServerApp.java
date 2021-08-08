package backuper.server;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sun.net.httpserver.HttpServer;

import backuper.server.config.Configuration;
import backuper.server.handlers.FileDataRequestHandler;
import backuper.server.handlers.FileListRequestHandler;
import backuper.server.handlers.FileSumRequestHandler;
import utils.JSONUtils;

@SuppressWarnings("restriction")
public class BackuperServerApp {

    public static void main(String[] args) throws IOException {
        Configuration config = JSONUtils.loadFromDisk(new File("config.json"), new TypeReference<Configuration>() {});
        config.linkObjects();
        FileServer fileServer = new FileServer(config);

        int serverPort = config.getServerPort();
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(serverPort), 0);

        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor)Executors.newFixedThreadPool(10);
        httpServer.setExecutor(threadPoolExecutor);

        httpServer.createContext("/file-list", new FileListRequestHandler(fileServer));
        httpServer.createContext("/file-sum", new FileSumRequestHandler(fileServer));
        httpServer.createContext("/file-data", new FileDataRequestHandler(fileServer));

        httpServer.start();
        System.out.println("Server started on port " + serverPort);
    }
}
