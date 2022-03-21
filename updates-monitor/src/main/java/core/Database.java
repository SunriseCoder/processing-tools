package core;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import core.dto.youtube.YoutubeChannel;
import core.dto.youtube.YoutubeVideo;
import core.dto.youtube.YoutubeVideoFormatTypes;

public class Database {
    private Map<String, YoutubeChannel> youtubeChannels;
    private Map<String, YoutubeVideo> youtubeVideos;

    public Database() {
        youtubeChannels = new HashMap<>();
        youtubeVideos = new HashMap<>();
    }

    public Map<String, YoutubeChannel> getYoutubeChannels() {
        return youtubeChannels;
    }

    public void addYoutubeChannel(YoutubeChannel channel) {
        youtubeChannels.put(channel.getChannelId(), channel);
    }

    public YoutubeChannel getYoutubeChannel(String channelId) {
        YoutubeChannel channel = youtubeChannels.get(channelId);
        return channel;
    }

    public void addYoutubeVideo(YoutubeVideo youtubeVideo) {
        youtubeVideos.put(youtubeVideo.getVideoId(), youtubeVideo);
    }

    public void linkEntities() {
        for (YoutubeVideo youtubeVideo : youtubeVideos.values()) {
            // Linking Youtube Videos with Youtube Channels
            YoutubeChannel youtubeChannel = youtubeChannels.get(youtubeVideo.getChannelId());
            if (youtubeChannel != null) {
                youtubeChannel.addVideo(youtubeVideo);
            }
        }
    }

    public Map<String, YoutubeVideo> getYoutubeVideos() {
        return youtubeVideos;
    }

    public Map<String, YoutubeVideo> getYoutubeNotScannedVideos() {
        Map<String, YoutubeVideo> result = youtubeVideos.values().stream()
                .filter(e -> !e.isScanned())
                .collect(Collectors.toMap(YoutubeVideo::getVideoId, Function.identity()));
        return result;
    }

    public Map<String, YoutubeVideo> getYoutubeNotDownloadedVideos() {
        Map<String, YoutubeVideo> result = youtubeVideos.values().stream()
                .filter(e -> !e.isDownloaded())
                .collect(Collectors.toMap(YoutubeVideo::getVideoId, Function.identity()));
        return result;
    }

    public Map<String, YoutubeVideo> getYoutubeDownloadedVideos() {
        Map<String, YoutubeVideo> result = youtubeVideos.values().stream()
                .filter(e -> e.isDownloaded())
                .collect(Collectors.toMap(YoutubeVideo::getVideoId, Function.identity()));
        return result;
    }

    public Map<String, YoutubeVideo> getYoutubeVideosByVideoFormatType(YoutubeVideoFormatTypes type) {
        Map<String, YoutubeVideo> result = youtubeVideos.values().stream()
                .filter(e -> type.equals(e.getVideoFormatType()))
                .collect(Collectors.toMap(YoutubeVideo::getVideoId, Function.identity()));
        return result;
    }
}
