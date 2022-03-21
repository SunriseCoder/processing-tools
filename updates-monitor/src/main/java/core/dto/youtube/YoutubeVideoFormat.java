package core.dto.youtube;

public class YoutubeVideoFormat {
    public YoutubeVideoFormatTypes type;

    public int iTag;
    public String mimeType;
    public int bitrate;
    public int width;
    public int height;
    public int fps;

    public String fileExtension;
    public long contentLength;
    public String downloadURL;

    @Override
    public String toString() {
        return "[" + mimeType + ", " + width + "x" + height + "@" + fps + ", " + (bitrate / 1024) + " kbps, " + type.name() + "]";
    }
}
