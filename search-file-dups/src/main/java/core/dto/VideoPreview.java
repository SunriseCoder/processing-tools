package core.dto;

public class VideoPreview {
    private String videoId;
    private String title;
    private String filename;
    private boolean processed;

    public VideoPreview() {
        // Default constructor
    }

    public VideoPreview(String videoId, String filename, String title) {
        this.videoId = videoId;
        this.filename = filename;
        this.title = title;
    }

    public String getVideoId() {
        return videoId;
    }

    public void setVideoId(String videoId) {
        this.videoId = videoId;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isProcessed() {
        return processed;
    }

    public void setProcessed(boolean processed) {
        this.processed = processed;
    }
}
