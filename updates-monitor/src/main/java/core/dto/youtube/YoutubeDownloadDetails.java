package core.dto.youtube;

public class YoutubeDownloadDetails {
    private String videoId;
    private String temporaryFilePath;

    private YoutubeVideoFormat videoFormat;
    private YoutubeAudioFormat audioFormat;

    public String getVideoId() {
        return videoId;
    }

    public YoutubeDownloadDetails setVideoId(String videoId) {
        this.videoId = videoId;
        return this;
    }

    public String getTemporaryFilePath() {
        return temporaryFilePath;
    }

    public YoutubeDownloadDetails setTemporaryFilePath(String temporaryFilePath) {
        this.temporaryFilePath = temporaryFilePath;
        return this;
    }

    public YoutubeVideoFormat getVideoFormat() {
        return videoFormat;
    }

    public YoutubeDownloadDetails setVideoFormat(YoutubeVideoFormat videoFormat) {
        this.videoFormat = videoFormat;
        return this;
    }

    public YoutubeAudioFormat getAudioFormat() {
        return audioFormat;
    }

    public YoutubeDownloadDetails setAudioFormat(YoutubeAudioFormat audioFormat) {
        this.audioFormat = audioFormat;
        return this;
    }
}
