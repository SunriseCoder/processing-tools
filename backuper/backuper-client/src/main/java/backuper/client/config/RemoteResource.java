package backuper.client.config;

public class RemoteResource {
    private String protocol;
    private String host;
    private String port;
    private String token;

    private String serverUrl;
    private String resourceUrl;

    public RemoteResource(String protocol, String host, String port, String resourceUrl, String token) {
        this.protocol = protocol;
        this.host = host;
        this.port = port;
        this.resourceUrl = resourceUrl;
        this.token = token;

        this.serverUrl = protocol + "://" + host + ":" + port + "/";
    }

    public String getProtocol() {
        return protocol;
    }

    public String getHost() {
        return host;
    }

    public String getPort() {
        return port;
    }

    public String getResourceUrl() {
        return resourceUrl;
    }

    public String getToken() {
        return token;
    }

    public String getServerUrl() {
        return serverUrl;
    }
}
