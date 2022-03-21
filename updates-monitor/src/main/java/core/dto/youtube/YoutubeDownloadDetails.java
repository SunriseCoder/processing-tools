package core.dto.youtube;

public class YoutubeDownloadDetails {
    private String videoId;
    private String downloadFilePrefix;
    private String temporaryFilePath;

    private long contentLength;
    private String fileExtension;
    private String downloadURL;
    private int iTag;

    public String getVideoId() {
        return videoId;
    }

    public YoutubeDownloadDetails setVideoId(String videoId) {
        this.videoId = videoId;
        return this;
    }

    public String getDownloadFilePrefix() {
        return downloadFilePrefix;
    }

    public YoutubeDownloadDetails setDownloadFilePrefix(String downloadFilePrefix) {
        this.downloadFilePrefix = downloadFilePrefix;
        return this;
    }

    public String getTemporaryFilePath() {
        return temporaryFilePath;
    }

    public YoutubeDownloadDetails setTemporaryFilePath(String temporaryFilePath) {
        this.temporaryFilePath = temporaryFilePath;
        return this;
    }

    public long getContentLength() {
        return contentLength;
    }

    public YoutubeDownloadDetails setContentLength(long contentLength) {
        this.contentLength = contentLength;
        return this;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public YoutubeDownloadDetails setFileExtension(String fileExtension) {
        this.fileExtension = fileExtension;
        return this;
    }

    public String getDownloadUrl() {
        return downloadURL;
    }

    public YoutubeDownloadDetails setDownloadURL(String downloadURL) {
        this.downloadURL = downloadURL;
        return this;
    }

    public int getITag() {
        return iTag;
    }

    public YoutubeDownloadDetails setITag(int iTag) {
        this.iTag = iTag;
        return this;
    }
}
