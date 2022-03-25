package core.dto.youtube;

public class YoutubeVideo {
    private String videoId;
    private String channelId;
    private String title;
    private String description;
    private int durationInSeconds;
    private String uploadDate;

    private boolean scanned = false;
    private boolean downloaded = false;

    private String filename;
    private long fileSize;
    private YoutubeVideoFormatTypes videoFormatType;

    public String getVideoId() {
        return videoId;
    }

    public void setVideoId(String videoId) {
        this.videoId = videoId;
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

    public int getDurationInSeconds() {
        return durationInSeconds;
    }

    public void setDurationInSeconds(int durationInSeconds) {
        this.durationInSeconds = durationInSeconds;
    }

    public String getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(String uploadDate) {
        this.uploadDate = uploadDate;
    }

    public boolean isScanned() {
        return scanned;
    }

    public void setScanned(boolean scanned) {
        this.scanned = scanned;
    }

    public boolean isDownloaded() {
        return downloaded;
    }

    public void setDownloaded(boolean downloaded) {
        this.downloaded = downloaded;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public YoutubeVideoFormatTypes getVideoFormatType() {
        return videoFormatType;
    }

    public void setVideoFormatType(YoutubeVideoFormatTypes videoFormatType) {
        this.videoFormatType = videoFormatType;
    }

    @Override
    public String toString() {
        return videoId + ": " + title;
    }
}
