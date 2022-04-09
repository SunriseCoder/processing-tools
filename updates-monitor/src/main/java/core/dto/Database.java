package core.dto;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;

import core.dto.youtube.YoutubeChannel;
import core.dto.youtube.YoutubePlaylist;
import core.dto.youtube.YoutubeVideo;
import utils.FileUtils;

public class Database {
    private Map<String, YoutubeChannel> youtubeChannels;
    private Map<String, YoutubePlaylist> youtubePlaylists;
    private Map<String, YoutubeVideo> youtubeVideos;

    @JsonIgnore
    private YoutubeChannel defaultYoutubeChannel;

    public Database() {
        youtubeChannels = new HashMap<>();
        youtubePlaylists = new HashMap<>();
        youtubeVideos = new HashMap<>();

        defaultYoutubeChannel = new YoutubeChannel("Default");
        defaultYoutubeChannel.setTitle("Default");
        String defaultChannelFoldername = "download/Default";
        defaultYoutubeChannel.setFoldername(defaultChannelFoldername);
        FileUtils.createFolderIfNotExists(defaultChannelFoldername);
    }

    public Map<String, YoutubeChannel> getYoutubeChannels() {
        return youtubeChannels;
    }

    public YoutubeChannel getYoutubeChannel(String channelId) {
        YoutubeChannel channel = youtubeChannels.get(channelId);
        return channel;
    }

    public YoutubeChannel getDefaultYoutubeChannel() {
        return defaultYoutubeChannel;
    }

    public void addYoutubeChannel(YoutubeChannel channel) {
        youtubeChannels.put(channel.getChannelId(), channel);
    }

    public void setYoutubeChannels(Map<String, YoutubeChannel> youtubeChannels) {
        this.youtubeChannels = youtubeChannels;
    }

    public Map<String, YoutubePlaylist> getYoutubePlaylists() {
        return youtubePlaylists;
    }

    public void addYoutubePlaylist(YoutubePlaylist youtubePlaylist) {
        youtubePlaylists.put(youtubePlaylist.getPlaylistId(), youtubePlaylist);
    }

    public void setYoutubePlaylists(Map<String, YoutubePlaylist> youtubePlaylists) {
        this.youtubePlaylists = youtubePlaylists;
    }

    public void addYoutubeVideo(YoutubeVideo youtubeVideo) {
        youtubeVideos.put(youtubeVideo.getVideoId(), youtubeVideo);
    }

    public void linkEntities() {
        // Linking Youtube Videos with Youtube Channels
        youtubeChannels.values().stream()
                .forEach(e -> e.cleanup());
        youtubeVideos.values().stream()
                .forEach(youtubeVideo -> {
                    YoutubeChannel youtubeChannel = youtubeChannels.get(youtubeVideo.getChannelId());
                    if (youtubeChannel != null) {
                        youtubeChannel.addVideo(youtubeVideo);
                    } else {
                        youtubeVideo.setChannelId(defaultYoutubeChannel.getChannelId());
                        defaultYoutubeChannel.addVideo(youtubeVideo);
                    }
                });

        // Linking Youtube Playlists with Channels
        youtubePlaylists.values().stream()
                .forEach(e -> e.cleanup());
        youtubePlaylists.values().stream()
                .forEach(youtubePlaylist -> {
                    YoutubeChannel youtubeChannel = youtubeChannels.get(youtubePlaylist.getChannelId());
                    if (youtubeChannel != null) {
                        youtubeChannel.addPlaylist(youtubePlaylist);
                    }

                    // Linking Youtube Playlists with Videos
                    youtubePlaylist.getVideoIds().stream()
                            .forEach(videoId -> {
                                YoutubeVideo youtubeVideo = youtubeVideos.get(videoId);
                                if (youtubeVideo != null) {
                                    youtubePlaylist.addVideo(youtubeVideo);
                                } else {
                                    youtubeVideo = new YoutubeVideo();
                                    youtubeVideo.setVideoId(videoId);
                                    youtubeVideo.setChannelId(defaultYoutubeChannel.getChannelId());
                                    defaultYoutubeChannel.addVideo(youtubeVideo);
                                }
                            });
                });
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

    public void setYoutubeVideos(Map<String, YoutubeVideo> youtubeVideos) {
        this.youtubeVideos = youtubeVideos;
    }
}
