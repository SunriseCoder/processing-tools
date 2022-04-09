package core.dto.youtube;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class YoutubePlaylist {
    private String playlistId;
    private String channelId;
    private String title;
    private String description;

    private List<String> videoIds;
    @JsonIgnore
    private Map<String, YoutubeVideo> videos;

    private boolean scanned;
    private boolean deleted;

    public YoutubePlaylist() {
        videoIds = new ArrayList<>();
        videos = new HashMap<>();
    }

    public String getPlaylistId() {
        return playlistId;
    }

    public void setPlaylistId(String playlistId) {
        this.playlistId = playlistId;
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

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getVideoIds() {
        return videoIds;
    }

    public void setVideoIds(List<String> videoIds) {
        this.videoIds = videoIds;
    }

    public void addVideoId(String videoId) {
        videoIds.add(videoId);
    }

    public Map<String, YoutubeVideo> getVideos() {
        return videos;
    }

    public void setVideos(Map<String, YoutubeVideo> videos) {
        this.videos = videos;
    }

    public void addVideo(YoutubeVideo video) {
        videos.put(video.getVideoId(), video);
    }

    public boolean isScanned() {
        return scanned;
    }

    public void setScanned(boolean scanned) {
        this.scanned = scanned;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public void cleanup() {
        videos.clear();
    }

    public boolean containsVideo(String videoId) {
        boolean result = videos.containsKey(videoId);
        return result;
    }

    @Override
    public String toString() {
        return playlistId + ": " + title;
    }
}
