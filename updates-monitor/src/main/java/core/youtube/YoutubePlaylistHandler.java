package core.youtube;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.fasterxml.jackson.databind.JsonNode;

import core.dto.Configuration;
import core.dto.youtube.YoutubePlaylist;
import core.dto.youtube.YoutubeVideo;
import util.DownloadUtils;
import util.DownloadUtils.Response;
import util.PageParsing;
import utils.JSONUtils;

public class YoutubePlaylistHandler {
    private static final Pattern PLAYLIST_URL_PATTERN = Pattern
            .compile("^https?:\\/\\/www.youtube.com\\/playlist\\?list=([0-9A-Za-z_-]+)&?.*$");

    public static boolean isYoutubePlaylistURL(String url) {
        Matcher matcher = PLAYLIST_URL_PATTERN.matcher(url);
        boolean result = matcher.matches() && matcher.groupCount() == 1;
        return result;
    }

    public static Result fetchNewPlaylist(String url) throws IOException {
        Result result = new Result();

        YoutubePlaylist playlist = new YoutubePlaylist();
        result.youtubePlaylist = playlist;

        Result playlistIdResult = downloadPlaylistIdByCustomURL(url);
        if (playlistIdResult.playlistNotFound) {
            result.playlistNotFound = playlistIdResult.playlistNotFound;
            return result;
        }
        playlist.setPlaylistId(playlistIdResult.playlistId);

        Result downloadDetailsResult = downloadPlaylistDetails(playlist.getPlaylistId());
        if (downloadDetailsResult.playlistNotFound) {
            result.playlistNotFound = downloadDetailsResult.playlistNotFound;
            return result;
        }
        playlist.setChannelId(downloadDetailsResult.channelId);
        playlist.setTitle(downloadDetailsResult.newTitle);
        playlist.setDescription(downloadDetailsResult.description);

        return result;
    }

    public static String parsePlaylistId(String url) {
        Matcher matcher = PLAYLIST_URL_PATTERN.matcher(url);
        if (matcher.matches() && matcher.groupCount() > 0) {
            String channelId = matcher.group(1);
            return channelId;
        } else {
            throw new RuntimeException("URL: (" + url + ") is not a Youtube Playlist");
        }
    }

    public static Result checkUpdates(YoutubePlaylist playlist) throws IOException {
        Result result = new Result();
        result.oldTitle = playlist.getTitle();

        // Checking Channel Title
        Result downloadDetailsResult = downloadPlaylistDetails(playlist.getPlaylistId());
        String playlistTitle = downloadDetailsResult.newTitle;
        result.newTitle = playlistTitle;

        // Fetching Videos from Channel
        Result updatePlaylistVideosResult = updatePlaylistVideos(playlist);
        result.newVideos = updatePlaylistVideosResult.newVideos;
        result.playlistNotFound = updatePlaylistVideosResult.playlistNotFound;

        playlist.setChannelId(downloadDetailsResult.channelId);
        playlist.setTitle(downloadDetailsResult.newTitle);
        playlist.setDescription(downloadDetailsResult.description);
        // TODO Implement Playlists fetch
        // TODO All videos from playlists try to check, if they are already on the channel,
        //      if not (probably unlisted) - add them to channel for further download
        // TODO Need to think about videos from another channels, but in the playlists of current channels
        //      maybe add another channels, but with flag to not to download them

        return result;
    }

    private static Result downloadPlaylistDetails(String playlistId) throws IOException {
        Result result = new Result();
        String url = "https://www.youtube.com/playlist?list=" + playlistId;
        String youtubeCookies = Configuration.getYoutubeCookies();
        // TODO Refactor using do-while until success if there will be some problems in the future
        Response response = DownloadUtils.downloadPageByGet(url, null, youtubeCookies);
        if (response.responseCode == 404) {
            result.playlistNotFound = true;
            return result;
        }

        if (response.responseCode != 200) {
            System.out.println("\nResponse code: " + response.responseCode
                    + ", response body is " + (response.body == null ? "null" : "not null"));
        }

        Document playlistPage = Jsoup.parse(response.body);
        List<String> playlistScriptSections = PageParsing.exctractSectionsFromPage(playlistPage, "script", "var ytInitialData = ");
        if (playlistScriptSections.size() > 1) {
            throw new IllegalStateException("More than 1 section has been found when the only section is expected, probably Youtube API was changed");
        }
        String playlistsJsonString = JSONUtils.extractJsonSubstringFromString(playlistScriptSections.get(0));
        JsonNode jsonRootNode = JSONUtils.parseJSON(playlistsJsonString);

        JsonNode jsonNode = jsonRootNode.get("sidebar").get("playlistSidebarRenderer").get("items").get(1);
        jsonNode = jsonNode.get("playlistSidebarSecondaryInfoRenderer").get("videoOwner").get("videoOwnerRenderer");
        result.channelId = jsonNode.get("navigationEndpoint").get("browseEndpoint").get("browseId").asText();

        result.newTitle = jsonRootNode.get("metadata").get("playlistMetadataRenderer").get("title").asText();

        JsonNode descriptionNode = jsonRootNode.get("metadata").get("playlistMetadataRenderer").get("description");
        result.description = descriptionNode == null ? null : descriptionNode.asText();

        if (result.channelId == null || result.channelId.isEmpty() || result.newTitle == null || result.newTitle.isEmpty()) {
            throw new RuntimeException("Could not parse Youtube Playlist Details, page format probably was changed,"
                    + " please try newer version or contact developers to update parsing algorithm");
        }

        return result;
    }

    private static Result downloadPlaylistIdByCustomURL(String url) throws IOException {
        Result result = new Result();
        Response response = DownloadUtils.downloadPageByGet(url, null, Configuration.getYoutubeCookies());
        if (response.responseCode == 404) {
            result.playlistNotFound = true;
            return result;
        }

        Document channelPage = Jsoup.parse(response.body);
        List<String> playlistScriptSections = PageParsing.exctractSectionsFromPage(channelPage, "script", "var ytInitialData = ");
        if (playlistScriptSections.size() > 1) {
            throw new IllegalStateException("More than 1 section has been found when the only section is expected, probably Youtube API was changed");
        }
        String playlistsJsonString = JSONUtils.extractJsonSubstringFromString(playlistScriptSections.get(0));
        JsonNode jsonRootNode = JSONUtils.parseJSON(playlistsJsonString);
        JsonNode jsonNode = jsonRootNode.get("contents").get("twoColumnBrowseResultsRenderer").get("tabs").get(0);
        jsonNode = jsonNode.get("tabRenderer").get("content").get("sectionListRenderer").get("contents").get(0);
        jsonNode = jsonNode.get("itemSectionRenderer").get("contents").get(0).get("playlistVideoListRenderer").get("playlistId");
        String playlistId = jsonNode.asText();
        if (playlistId == null || playlistId.isEmpty()) {
            throw new RuntimeException("Could not parse Youtube Playlist ID, page format probably was changed,"
                    + " please try newer version or contact developers to update parsing algorithm");
        }

        result.playlistId = playlistId;
        return result;
    }

    private static Result updatePlaylistVideos(YoutubePlaylist playlist) throws IOException {
        Result result = new Result();
        result.newVideos = new ArrayList<>();

        // Parsing the first page with videos
        System.out.print("1 "); // Number of page we are parsing
        Result videoListResult = downloadVideosListFirstPage(playlist.getPlaylistId());
        if (videoListResult.playlistNotFound) {
            result.playlistNotFound = videoListResult.playlistNotFound;
            return result;
        }

        // Stop parsing video pages if we got down to already existing videos
        for (YoutubeVideo video : videoListResult.newVideos) {
            playlist.addVideoId(video.getVideoId());
            result.newVideos.add(video);
        }

        // Parsing the other pages
        int pageNumber = 2;
        while (videoListResult.continuationToken != null) {
            System.out.print(pageNumber++ + " ");
            videoListResult = downloadVideosListNextPage(playlist.getPlaylistId(), videoListResult);
            if (videoListResult.playlistNotFound) {
                result.playlistNotFound = videoListResult.playlistNotFound;
                return result;
            }

            for (YoutubeVideo video : videoListResult.newVideos) {
                if (playlist.containsVideo(video.getVideoId())) {
                    return result;
                }

                playlist.addVideoId(video.getVideoId());
                result.newVideos.add(video);
            }
        }

        return result;
    }

    private static Result downloadVideosListFirstPage(String playlistId) throws IOException {
        List<YoutubeVideo> videos = new ArrayList<>();
        Result result = new Result();
        result.playlistId = playlistId;
        result.newVideos = videos;

        String urlString = "https://www.youtube.com/playlist?list=" + playlistId;
        Response response = DownloadUtils.downloadPageByGet(urlString, null, Configuration.getYoutubeCookies());

        if (response.headers.get(null).get(0).split(" ")[1].equals("404")) {
            result.playlistNotFound = true;
            return result;
        }

        try {
            Document videosPage = Jsoup.parse(response.body);

            List<String> configScriptSections = PageParsing.exctractSectionsFromPage(videosPage, "script", "WEB_PLAYER_CONTEXT_CONFIGS");
            if (configScriptSections.size() > 1) {
                throw new IllegalStateException("More than 1 section has been found when the only section is expected, probably Youtube API was changed");
            }
            result.clientConfig = parseYoutubeConfigSection(configScriptSections.get(0));

            List<String> playlistScriptSections = PageParsing.exctractSectionsFromPage(videosPage, "script", "var ytInitialData = ");
            if (playlistScriptSections.size() > 1) {
                throw new IllegalStateException("More than 1 section has been found when the only section is expected, probably Youtube API was changed");
            }
            String playlistsJsonString = JSONUtils.extractJsonSubstringFromString(playlistScriptSections.get(0));
            JsonNode jsonRootNode = JSONUtils.parseJSON(playlistsJsonString);

            JsonNode jsonNode = jsonRootNode.get("contents").get("twoColumnBrowseResultsRenderer").get("tabs").get(0);
            jsonNode = jsonNode.get("tabRenderer").get("content").get("sectionListRenderer").get("contents").get(0);
            jsonNode = jsonNode.get("itemSectionRenderer").get("contents").get(0);

            if (jsonNode.has("playlistVideoListRenderer")) {
                JsonNode gridRendererNode = jsonNode.get("playlistVideoListRenderer");
                JsonNode itemsNode = gridRendererNode.get("contents");
                parseVideoItems(itemsNode, videos, result);
            }
        } catch (Exception e) {
            PrintWriter pw = new PrintWriter("error-page-dump.dat");
            pw.print(response.body);
            pw.flush();
            pw.close();
            throw new RuntimeException("Could not parse Youtube Playlist page, "
                    + "Youtube page design was probably changed: " + e.getMessage(), e);
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

    private static Result downloadVideosListNextPage(String playlistId, Result lastResult) throws IOException {
        List<YoutubeVideo> videos = new ArrayList<>();
        Result result = new Result();
        ClientConfig clientConfig = lastResult.clientConfig;
        result.clientConfig = clientConfig;
        result.playlistId = playlistId;
        result.newVideos = videos;

        try {
            String url = "https://www.youtube.com/youtubei/" + clientConfig.apiVersion
                    + "/browse?key=" + clientConfig.apiKey + "&prettyPrint=false";
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
            throw new RuntimeException("Could not parse Youtube Channel Videos page, "
                    + "Youtube page design was probably changed: " + e.getMessage(), e);
        }

        return result;
    }

    private static void parseVideoItems(JsonNode itemsNode, List<YoutubeVideo> videos, Result result) {
        for (JsonNode itemNode : itemsNode) {
            if (itemNode.has("playlistVideoRenderer")) {
                YoutubeVideo video = new YoutubeVideo();

                JsonNode videoNode = itemNode.get("playlistVideoRenderer");
                String videoId = videoNode.get("videoId").asText();
                video.setVideoId(videoId);

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
        public String playlistId;
        public String channelId;
        public String oldTitle;
        public String newTitle;
        public String description;

        public YoutubePlaylist youtubePlaylist;

        public boolean playlistNotFound;

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
