package core.dto.youtube;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class YoutubeChannel {
    private String channelId;
    private String title;

    private String foldername;

    @JsonIgnore
    private Map<String, YoutubeVideo> videos;

    public YoutubeChannel() {
        this(null);
    }

    public YoutubeChannel(String channelId) {
        this.channelId = channelId;
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

    public void addVideo(YoutubeVideo video) {
        videos.put(video.getVideoId(), video);
    }

    public boolean containVideo(String videoId) {
        boolean result = videos.containsKey(videoId);
        return result;
    }

    @JsonIgnore
    public String getStatusString() {
        long completedVideos = videos.values().stream().filter(v -> v.isDownloaded()).count();
        long newVideos = videos.size() - completedVideos;
        return "Videos: " + completedVideos + " done, " + newVideos + " new, " + videos.size() + " total";
    }

    @Override
    public String toString() {
        return channelId + ": " + title;
    }
}
