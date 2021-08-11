package backuper.server;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.hc.core5.http.URIScheme;
import org.apache.hc.core5.http.impl.bootstrap.AsyncServerBootstrap;
import org.apache.hc.core5.http.impl.bootstrap.HttpAsyncServer;
import org.apache.hc.core5.http.protocol.HttpDateGenerator;
import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.core5.reactor.IOReactorConfig;
import org.apache.hc.core5.reactor.ListenerEndpoint;
import org.apache.hc.core5.util.TimeValue;

import com.fasterxml.jackson.core.type.TypeReference;

import backuper.server.config.Configuration;
import backuper.server.handlers.FileDataRequestHandler;
import backuper.server.handlers.FileListRequestHandler;
import utils.JSONUtils;

public class BackuperServerApp {

    public static void main(String[] args) throws Exception {
        Configuration config = JSONUtils.loadFromDisk(new File("config.json"), new TypeReference<Configuration>() {});
        config.linkObjects();
        FileServer fileServer = new FileServer(config);

        int serverPort = config.getServerPort();
        IOReactorConfig reactorConfig = IOReactorConfig.custom()
                .setSoTimeout(15, TimeUnit.SECONDS).setTcpNoDelay(true).build();
        HttpAsyncServer server = AsyncServerBootstrap.bootstrap()
                .setIOReactorConfig(reactorConfig)
                .register("/file-list", new FileListRequestHandler(fileServer))
                .register("/file-data", new FileDataRequestHandler(fileServer))
                .create();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                println("HTTP server shutting down");
                server.close(CloseMode.GRACEFUL);
            }
        });

        server.start();
        final Future<ListenerEndpoint> future = server.listen(new InetSocketAddress(serverPort), URIScheme.HTTP);
        final ListenerEndpoint listenerEndpoint = future.get();
        println("Listening on " + listenerEndpoint.getAddress());
        server.awaitShutdown(TimeValue.MAX_VALUE);
    }

    static final void println(final String msg) {
        System.out.println(HttpDateGenerator.INSTANCE.getCurrentDate() + " | " + msg);
    }
}
