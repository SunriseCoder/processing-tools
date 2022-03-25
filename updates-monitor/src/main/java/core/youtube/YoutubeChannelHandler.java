package core.youtube;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.fasterxml.jackson.databind.JsonNode;

import core.dto.Configuration;
import core.dto.youtube.YoutubeChannel;
import core.dto.youtube.YoutubeVideo;
import util.DownloadUtils;
import util.DownloadUtils.Response;
import utils.JSONUtils;

public class YoutubeChannelHandler {
    private static final Pattern CHANNEL_URL_PATTERN = Pattern.compile("^https?://www.youtube.com/channel/([0-9A-Za-z_-]+)/?.*$");
    private static final Pattern CHANNEL_CUSTOM_URL_PATTERN = Pattern.compile("^https?://www.youtube.com/c/([^/\\s]+)/?.*$");
    private static final Pattern CHANNEL_USER_URL_PATTERN = Pattern.compile("^https?://www.youtube.com/user/([0-9A-Za-z_-]+)/?.*$");

    public static boolean isYoutubeChannelURL(String url) {
        Matcher matcher = CHANNEL_URL_PATTERN.matcher(url);
        boolean result = matcher.matches() && matcher.groupCount() == 1;

        matcher = CHANNEL_CUSTOM_URL_PATTERN.matcher(url);
        result |= matcher.matches() && matcher.groupCount() == 1;

        matcher = CHANNEL_USER_URL_PATTERN.matcher(url);
        result |= matcher.matches() && matcher.groupCount() == 1;

        return result;
    }

    public static Result fetchNewChannel(String url) throws IOException {
        Result result = new Result();

        YoutubeChannel channel = new YoutubeChannel();
        result.youtubeChannel = channel;

        Result channelIdResult = downloadChannelIdByCustomURL(url);
        if (channelIdResult.channelNotFound) {
            result.channelNotFound = channelIdResult.channelNotFound;
            return result;
        }
        channel.setChannelId(channelIdResult.channelId);

        Result downloadTitleResult = downloadChannelTitle(channel.getChannelId());
        if (downloadTitleResult.channelNotFound) {
            result.channelNotFound = downloadTitleResult.channelNotFound;
            return result;
        }
        channel.setTitle(downloadTitleResult.newTitle);

        return result;
    }

    public static String parseChannelId(String url) {
        Matcher matcher = CHANNEL_URL_PATTERN.matcher(url);
        if (matcher.matches() && matcher.groupCount() > 0) {
            String channelId = matcher.group(1);
            return channelId;
        } else {
            throw new RuntimeException("URL: (" + url + ") is not a Youtube Channel");
        }
    }

    public static Result checkUpdates(YoutubeChannel channel) throws IOException {
        Result result = new Result();
        result.oldTitle = channel.getTitle();

        // Checking Channel Title
        Result downloadTitleResult = downloadChannelTitle(channel.getChannelId());
        String channelTitle = downloadTitleResult.newTitle;
        result.newTitle = channelTitle;

        // Fetching Videos from Channel
        Result updateChannelVideosResult = updateChannelVideos(channel);
        result.newVideos = updateChannelVideosResult.newVideos;
        result.channelNotFound = result.channelNotFound;

        // TODO Implement Playlists fetch
        // TODO All videos from playlists try to check, if they are already on the channel,
        //      if not (probably unlisted) - add them to channel for further download
        // TODO Need to think about videos from another channels, but in the playlists of current channels
        //      maybe add another channels, but with flag to not to download them

        return result;
    }

    private static Result downloadChannelTitle(String channelId) throws IOException {
        Result result = new Result();
        Connection connection = Jsoup.connect("https://www.youtube.com/channel/" + channelId);
        if (connection.response().statusCode() == 404) {
            result.channelNotFound = true;
            return result;
        }

        Document channelPage = connection.get();
        String channelTitle = channelPage.select("meta[itemprop=name]").attr("content");
        if (channelTitle == null || channelTitle.isEmpty()) {
            throw new RuntimeException("Could not parse Youtube Channel Title, page format probably was changed,"
                    + " please try newer version or contact developers to update parsing algorithm");
        }
        result.newTitle = channelTitle;
        return result;
    }

    private static Result downloadChannelIdByCustomURL(String url) throws IOException {
        Result result = new Result();
        Response response = DownloadUtils.downloadPageByGet(url, null, Configuration.getYoutubeCookies());
        if (response.responseCode == 404) {
            result.channelNotFound = true;
            return result;
        }

        Document channelPage = Jsoup.parse(response.body);
        String channelId = channelPage.select("meta[itemprop=channelId]").attr("content");
        if (channelId == null || channelId.isEmpty()) {
            throw new RuntimeException("Could not parse Youtube Channel ID, page format probably was changed,"
                    + " please try newer version or contact developers to update parsing algorithm");
        }

        result.channelId = channelId;
        return result;
    }

    private static Result updateChannelVideos(YoutubeChannel channel) throws IOException {
        Result result = new Result();
        result.newVideos = new ArrayList<>();

        // Parsing the first page with videos
        System.out.print("1 "); // Number of page we are parsing
        Result videoListResult = downloadVideosListFirstPage(channel.getChannelId());
        if (videoListResult.channelNotFound) {
            result.channelNotFound = videoListResult.channelNotFound;
            return result;
        }

        // Stop parsing video pages if we got down to already existing videos
        for (YoutubeVideo video : videoListResult.newVideos) {
            if (channel.containVideo(video.getVideoId())) {
                return result;
            }

            channel.addVideo(video);
            result.newVideos.add(video);
        }

        // Parsing the other pages
        int pageNumber = 2;
        while (videoListResult.continuationToken != null) {
            System.out.print(pageNumber++ + " ");
            videoListResult = downloadVideosListNextPage(channel.getChannelId(), videoListResult);
            if (videoListResult.channelNotFound) {
                result.channelNotFound = videoListResult.channelNotFound;
                return result;
            }

            for (YoutubeVideo video : videoListResult.newVideos) {
                if (channel.containVideo(video.getVideoId())) {
                    return result;
                }

                channel.addVideo(video);
                result.newVideos.add(video);
            }
        }

        return result;
    }

    private static Result downloadVideosListFirstPage(String channelId) throws IOException {
        List<YoutubeVideo> videos = new ArrayList<>();
        Result result = new Result();
        result.channelId = channelId;
        result.newVideos = videos;

        String urlString = "https://www.youtube.com/channel/" + channelId + "/videos";
        Response response = DownloadUtils.downloadPageByGet(urlString, null, Configuration.getYoutubeCookies());

        if (response.headers.get(null).get(0).split(" ")[1].equals("404")) {
            result.channelNotFound = true;
            return result;
        }

        try {
            Document videosPage = Jsoup.parse(response.body);
            String pageData = null;
            Elements scriptNodes = videosPage.select("script");
            for (Element scriptNode : scriptNodes) {
                if (scriptNode.data().contains("WEB_PLAYER_CONTEXT_CONFIGS")) {
                    result.clientConfig = parseYoutubeConfigSection(scriptNode.data());
                }
                if (scriptNode.data().startsWith("var ytInitialData = ")) {
                    pageData = scriptNode.data();
                }
            }
            // Cutting off "var ytInitialData = " at the beginning and ";" at the end
            String jsonText = pageData.substring(20, pageData.length() - 1);
            JsonNode jsonRootNode = JSONUtils.parseJSON(jsonText);
            JsonNode jsonNode = jsonRootNode.get("contents");
            jsonNode = jsonNode.get("twoColumnBrowseResultsRenderer");
            jsonNode = jsonNode.get("tabs");
            jsonNode = jsonNode.get(1);
            jsonNode = jsonNode.get("tabRenderer");
            jsonNode = jsonNode.get("content");
            jsonNode = jsonNode.get("sectionListRenderer");
            jsonNode = jsonNode.get("contents");
            jsonNode = jsonNode.get(0);
            jsonNode = jsonNode.get("itemSectionRenderer");
            jsonNode = jsonNode.get("contents");
            jsonNode = jsonNode.get(0);

            if (jsonNode.has("gridRenderer")) {
                JsonNode gridRendererNode = jsonNode.get("gridRenderer");
                JsonNode itemsNode = gridRendererNode.get("items");
                parseVideoItems(itemsNode, videos, result);
            }
        } catch (Exception e) {
            PrintWriter pw = new PrintWriter("error-page-dump.dat");
            pw.print(response.body);
            pw.flush();
            pw.close();
            throw new RuntimeException("Could not parse Youtube Channel Videos page, Youtube page design was probably changed: " + e.getMessage(), e);
        }

        return result;
    }

    private static ClientConfig parseYoutubeConfigSection(String scriptNodeData) throws IOException {
        String jsonStringWithJunk = scriptNodeData.split("ytcfg.set\\(")[1];
        String jsonString = JSONUtils.trimAfterJsonEnds(jsonStringWithJunk);
        JsonNode jsonRootNode = JSONUtils.parseJSON(jsonString);

        ClientConfig clientConfig = new ClientConfig();
        clientConfig.apiKey = jsonRootNode.get("INNERTUBE_API_KEY").asText();
        clientConfig.apiVersion = jsonRootNode.get("INNERTUBE_API_VERSION").asText();
        clientConfig.clientName = jsonRootNode.get("INNERTUBE_CLIENT_NAME").asText();
        clientConfig.clientVersion = jsonRootNode.get("INNERTUBE_CLIENT_VERSION").asText();

        return clientConfig;
    }

    private static Result downloadVideosListNextPage(String channelId, Result lastResult) throws IOException {
        List<YoutubeVideo> videos = new ArrayList<>();
        Result result = new Result();
        ClientConfig clientConfig = lastResult.clientConfig;
        result.clientConfig = clientConfig;
        result.channelId = channelId;
        result.newVideos = videos;

        try {
//            String url = "https://www.youtube.com/browse_ajax?ctoken=" + URLEncoder.encode(lastResult.continuationToken, "UTF-8") + "&continuation="
//                    + URLEncoder.encode(lastResult.continuationToken, "UTF-8") + "&itct=" + URLEncoder.encode(lastResult.clickTrackingParams, "UTF-8");
            String url = "https://www.youtube.com/youtubei/" + clientConfig.apiVersion + "/browse?key=" + clientConfig.apiKey;
            String jsonText = new StringBuilder()
                    .append("{\n")
                    .append("    \"context\": {\n")
                    .append("        \"client\": {\n")
                    .append("            \"clientName\": \"").append(clientConfig.clientName).append("\",\n")
                    .append("            \"clientVersion\": \"").append(clientConfig.clientVersion).append("\"\n")
                    .append("        },\n")
                    .append("        \"clickTracking\": {\n")
                    .append("            \"clickTrackingParams\": \"").append(lastResult.clickTrackingParams).append("\"\n")
                    .append("        }\n")
                    .append("    },\n")
                    .append("    \"continuation\": \"").append(lastResult.continuationToken).append("\"\n")
                    .append("}").toString();
            Response response = DownloadUtils.downloadPageByPostJson(url, jsonText);
            jsonText = response.body;

            JsonNode jsonNode = JSONUtils.parseJSON(jsonText);
            jsonNode = jsonNode.get("onResponseReceivedActions").get(0);
            jsonNode = jsonNode.get("appendContinuationItemsAction");

            // Parsing Videos
            JsonNode itemNodes = jsonNode.get("continuationItems");
            parseVideoItems(itemNodes, videos, result);
        } catch (Exception e) {
            throw new RuntimeException("Could not parse Youtube Channel Videos page, Youtube page design was probably changed: " + e.getMessage(), e);
        }

        return result;
    }

    private static void parseVideoItems(JsonNode itemsNode, List<YoutubeVideo> videos, Result result) {
        for (JsonNode itemNode : itemsNode) {
            if (itemNode.has("gridVideoRenderer")) {
                YoutubeVideo video = new YoutubeVideo();
                video.setChannelId(result.channelId);

                JsonNode videoNode = itemNode.get("gridVideoRenderer");
                String videoId = videoNode.get("videoId").asText();
                video.setVideoId(videoId);

                String videoTitle = videoNode.get("title").get("runs").get(0).get("text").asText();
                video.setTitle(videoTitle);

                videos.add(video);
            } else if (itemNode.has("continuationItemRenderer")) {
                JsonNode continuationsNode = itemNode.get("continuationItemRenderer");
                JsonNode continuationEndpointNode = continuationsNode.get("continuationEndpoint");
                result.clickTrackingParams = continuationEndpointNode.get("clickTrackingParams").asText();
                JsonNode continuationCommandNode = continuationEndpointNode.get("continuationCommand");
                result.continuationToken = continuationCommandNode.get("token").asText();
            }
        }
    }

    public static class Result {
        public YoutubeChannel youtubeChannel;
        public boolean channelNotFound = false;
        public String oldTitle;
        public String newTitle;
        public String channelId;
        public List<YoutubeVideo> newVideos;
        public ClientConfig clientConfig;
        public String continuationToken;
        public String clickTrackingParams;
    }

    public static class ClientConfig {
        public String apiKey;
        public String apiVersion;
        public String clientName;
        public String clientVersion;

    }
}
