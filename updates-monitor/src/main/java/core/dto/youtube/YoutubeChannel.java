package core.dto.youtube;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class YoutubeChannel {
    private String channelId;
    private String title;

    private String foldername;

    @JsonIgnore
    private Map<String, YoutubePlaylist> playlists;
    @JsonIgnore
    private Map<String, YoutubeVideo> videos;

    public YoutubeChannel() {
        this(null);
    }

    public YoutubeChannel(String channelId) {
        this.channelId = channelId;
        playlists = new HashMap<>();
        videos = new HashMap<>();
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public String getTitle() {
        return title;
    }

    public String getFoldername() {
        return foldername;
    }

    public void setFoldername(String foldername) {
        this.foldername = foldername;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void addPlaylist(YoutubePlaylist playlist) {
        playlists.put(playlist.getPlaylistId(), playlist);
    }

    public Map<String, YoutubeVideo> getVideos() {
        return videos;
    }

    public void addVideo(YoutubeVideo video) {
        videos.put(video.getVideoId(), video);
    }

    public void cleanup() {
        playlists.clear();
        videos.clear();
    }

    public boolean containsPlaylist(String playlistId) {
        boolean result = playlists.containsKey(playlistId);
        return result;
    }

    public boolean containsVideo(String videoId) {
        boolean result = videos.containsKey(videoId);
        return result;
    }

    @JsonIgnore
    public String getStatusString() {
        long completedVideos = videos.values().stream().filter(v -> v.isDownloaded()).count();
        StringBuilder sb = new StringBuilder();
        sb.append("Videos: ")
        .append(completedVideos).append(" done, ")
        .append(videos.size() - completedVideos).append(" new, ")
        .append(videos.size()).append(" total, ")
        .append(playlists.size()).append(" playlists");
        return sb.toString();
    }

    @Override
    public String toString() {
        return channelId + ": " + title;
    }
}
