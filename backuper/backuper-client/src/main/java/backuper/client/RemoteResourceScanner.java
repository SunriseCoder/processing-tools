package backuper.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.message.BasicNameValuePair;

import com.fasterxml.jackson.core.type.TypeReference;

import backuper.client.config.RemoteResource;
import backuper.common.dto.FileMetadata;
import backuper.common.dto.FileMetadataRemote;
import backuper.common.helpers.HttpHelper;
import backuper.common.helpers.HttpHelper.Response;
import utils.JSONUtils;

public class RemoteResourceScanner {
    private static final Pattern RESOURCE_PATTERN = Pattern.compile(
            "(http[s]?)://([A-Za-z0-9\\-]+)@([A-Za-z0-9\\-\\.]+):([0-9]+)/([A-Za-z0-9\\\\-\\\\.]+)");

    public synchronized Map<String, FileMetadata> scan(String url) throws IOException, HttpException {
        RemoteResource resource = parseRemoteResource(url);
        if (resource == null) {
            throw new RuntimeException("Remote resource url has invalid format: " + url + ", must be like: http://token@host:port/resource");
        }

        String requestUrl = resource.getServerUrl() + "file-list";
        List<NameValuePair> postData = new ArrayList<>();
        postData.add(new BasicNameValuePair("resource", resource.getResourceUrl()));
        postData.add(new BasicNameValuePair("token", resource.getToken()));
        Response response = HttpHelper.sendPostRequest(requestUrl, postData);
        int responseCode = response.getCode();
        String responseText = new String(response.getData());

        if (responseCode != 200) {
            System.out.println("Response: " + responseCode + " " + responseText);
            System.exit(-1);
        }

        List<FileMetadataRemote> remoteFileList = JSONUtils.parseJSON(responseText, new TypeReference<List<FileMetadataRemote>>() {});
        Map<String, FileMetadata> fileMetadataMap = new LinkedHashMap<>();
        remoteFileList.stream()
                .map(m -> new FileMetadata(m, resource.getServerUrl(), resource.getResourceUrl(), resource.getToken()))
                .forEachOrdered(m -> fileMetadataMap.put(m.getRelativePath().toString(), m));

        return fileMetadataMap;
    }

    public static synchronized RemoteResource parseRemoteResource(String url) {
        Matcher matcher = RESOURCE_PATTERN.matcher(url);
        if (!matcher.matches()) {
            return null;
        }

        String protocol = matcher.group(1);
        String token = matcher.group(2);
        String host = matcher.group(3);
        String port = matcher.group(4);
        String resourceName = matcher.group(5);

        RemoteResource resource = new RemoteResource(protocol, host, port, resourceName, token);
        return resource;
    }
}
