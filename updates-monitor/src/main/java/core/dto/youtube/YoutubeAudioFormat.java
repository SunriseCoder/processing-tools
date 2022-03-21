package core.dto.youtube;

public class YoutubeAudioFormat {
    public int iTag;
    public String mimeType;
    public int bitrate;
    public int sampleRate;
    public int channels;

    public String fileExtension;
    public long contentLength;
    public String downloadURL;

    @Override
    public String toString() {
        return "[" + mimeType + ", " + sampleRate + "x" + channels + ", " + (bitrate / 1024) + " kbps]";
    }
}
