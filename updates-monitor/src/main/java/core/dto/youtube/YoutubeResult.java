package core.dto.youtube;

import java.io.File;

import com.fasterxml.jackson.databind.JsonNode;

public class YoutubeResult {
    public boolean unsupported;
    public boolean notFound;
    public boolean completed;
    public boolean queued;

    public File resultFile;
    public JsonNode jsonNode;
    public YoutubeVideoFormat videoFormat;
    public YoutubeAudioFormat audioFormat;
}
