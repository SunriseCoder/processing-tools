package core.youtube;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.fasterxml.jackson.databind.JsonNode;

import core.dto.YoutubeVideo;
import util.DownloadUtils;
import util.FFMPEGUtils;
import util.PageParsing;
import utils.FileUtils;
import utils.JSONUtils;

public class YoutubeVideoHandler {
    private static final Pattern VIDEO_URL_PATTERN = Pattern.compile("^https?:\\/\\/www.youtube.com\\/watch\\?v=([0-9A-Za-z_-]+)&?.*$");

    private YoutubeOrdinaryFileDownloader youtubeOrdinaryFileDownloader;

    public YoutubeVideoHandler() {
        youtubeOrdinaryFileDownloader = new YoutubeOrdinaryFileDownloader();
    }

    public String parseVideoId(String url) {
        Matcher matcher = VIDEO_URL_PATTERN.matcher(url);
        if (matcher.matches() && matcher.groupCount() > 0) {
            String channelId = matcher.group(1);
            return channelId;
        } else {
            throw new RuntimeException("URL: (" + url + ") is not a Youtube Video");
        }
    }

    public YoutubeResult downloadVideo(YoutubeVideo video, String downloadChannelPath, String temporaryFolderPath) throws Exception {
        YoutubeResult result = new YoutubeResult();

        // Downloading Video Page
        String urlString = "https://www.youtube.com/watch?v=" + video.getVideoId();
        DownloadUtils.Response response = DownloadUtils.downloadPageByGet(urlString, null);
        if (response.responseCode == 404) {
            result.notFound = true;
            return result;
        } else if (response.responseCode != 200) {
            throw new IOException("Error downloading page: \"" + urlString + "\", HTTP Status Code: " + response.responseCode);
        }
        Document videoPage = Jsoup.parse(response.body);

        // Fetching Video Details
        List<String> videoDetailsScriptSection = PageParsing.exctractSectionsFromPage(videoPage, "script", "var ytInitialPlayerResponse = ");
        if (videoDetailsScriptSection.size() > 1) {
            throw new IllegalStateException("More than 1 section has been found when the only section is expected, probably Youtube API was changed");
        }
        String videoDetailsJsonString = JSONUtils.extractJsonSubstringFromString(videoDetailsScriptSection.get(0));
        JsonNode playerResponseNode = JSONUtils.parseJSON(videoDetailsJsonString);
        JsonNode videoDetailsNode = playerResponseNode.get("videoDetails");
        video.setTitle(videoDetailsNode.get("title").asText());
        video.setDescription(videoDetailsNode.get("shortDescription").asText());
        video.setDurationInSeconds(Integer.parseInt(videoDetailsNode.get("lengthSeconds").asText()));
        String videoUploadDate = playerResponseNode.get("microformat").get("playerMicroformatRenderer").get("uploadDate").asText();
        video.setUploadDate(videoUploadDate);
        String videoFilename = video.getVideoId() + "_" + FileUtils.getSafeFilename(video.getTitle()) + ".mp4";
        video.setFilename(videoFilename);

        // Fetching Media Formats
        JsonNode streamingDataNode = playerResponseNode.get("streamingData");
        List<VideoFormat> videoFormats = fetchVideoFormats(streamingDataNode);
        List<AudioFormat> audioFormats = fetchAudioFormats(streamingDataNode);

        if (videoFormats.size() < 1 || audioFormats.size() < 1) {
            System.out.println("Unsupported video format: " + video.getVideoId());
            result.unsupported = true;
            return result;
        }

        // Downloading Files itself
        String videoFileDownloadPrefix = temporaryFolderPath + "/" + video.getVideoId() + "_video";
        String audioFileDownloadPrefix = temporaryFolderPath + "/" + video.getVideoId() + "_audio";
        String temporaryFilePath = temporaryFolderPath + "/" + video.getVideoId();

        // Downloading Video file
        System.out.print("\n\tDownloading video... ");

        YoutubeResult downloadVideoResult = null;
        VideoFormat videoFormat = videoFormats.get(0);
        switch (videoFormat.type) {
        case OrdinaryFile:
            downloadVideoResult = youtubeOrdinaryFileDownloader.download(video.getVideoId(), videoFileDownloadPrefix, temporaryFilePath, videoFormat);
            break;
        case OTF_Stream:
            // TODO Implement
            break;
        case Encrypted:
            // TODO Implement
            break;
        default:
            System.out.println("Unsupported video format: " + video.getVideoId() + ", format type: " + videoFormat.type.name());
            result.unsupported = true;
            return result;
        }
        if (downloadVideoResult == null) {
            System.out.println("Unsupported video format: " + video.getVideoId());
            result.unsupported = true;
            return result;
        }
        if (downloadVideoResult.notFound) {
            System.out.print("Not found...");
            result.notFound = true;
            return result;
        }
        result.completed = downloadVideoResult.completed;

        // Downloading Audio file
        if (!result.completed) {
            return result;
        }
        System.out.print("\n\tDownloading audio... ");
        AudioFormat audioFormat = audioFormats.get(0);
        YoutubeResult downloadAudioResult = youtubeOrdinaryFileDownloader.download(video.getVideoId(), audioFileDownloadPrefix, temporaryFilePath, audioFormat);
        if (downloadAudioResult.notFound) {
            System.out.print("Not found...");
            result.notFound = true;
            return result;
        }
        result.completed &= downloadAudioResult.completed;

        // Combine Video and Audio via ffmpeg
        if (!result.completed) {
            return result;
        }
        System.out.print("\n\tCombining video and audio tracks via ffmpeg... ");
        String videoTrackPath = downloadVideoResult.resultFile.getAbsolutePath();
        String audioTrackPath = downloadAudioResult.resultFile.getAbsolutePath();
        String ffmpegResultPath = temporaryFolderPath + "/" + video.getVideoId() + "_combined.mp4";
        result.completed &= FFMPEGUtils.combineVideoAndAudio(videoTrackPath, audioTrackPath, ffmpegResultPath);
        result.completed &= downloadVideoResult.resultFile.delete();
        result.completed &= downloadAudioResult.resultFile.delete();
        System.out.print(result.completed ? "Done" : "Failed");

        // Moving Temp file to Destination folder
        if (!result.completed) {
            return result;
        }
        System.out.print("\n\tMoving temporary file to the channel folder... ");
        result.completed &= FileUtils.moveFile(new File(ffmpegResultPath), new File(downloadChannelPath, video.getFilename()));
        System.out.print(result.completed ? "Done" : "Failed");

        return result;
    }

    private List<VideoFormat> fetchVideoFormats(JsonNode streamingDataNode) {
        JsonNode adaptiveFormatsNode = streamingDataNode.get("adaptiveFormats");

        List<VideoFormat> videoFormats = new ArrayList<>();

        for (JsonNode formatNode : adaptiveFormatsNode) {
            if (formatNode.get("mimeType").asText().startsWith("video")) {
                VideoFormat format = new VideoFormat();

                if (!formatNode.has("url")) {
                    format.type = VideoFormat.Types.Encrypted;
                } else if (formatNode.has("type") && "FORMAT_STREAM_TYPE_OTF".equals(formatNode.get("type").asText())) {
                    format.type = VideoFormat.Types.OTF_Stream;
                } else {
                    format.type = VideoFormat.Types.OrdinaryFile;
                }

                format.mimeType = formatNode.get("mimeType").asText();
                if (format.mimeType.startsWith("video/mp4")) {
                    format.fileExtension = "mp4";
                } else if (format.mimeType.startsWith("video/webm")) {
                    format.fileExtension = "webm";
                } else {
                    throw new IllegalArgumentException("Unsupported MimeType: " + format.mimeType);
                }

                format.iTag = formatNode.get("itag").asInt();
                if (formatNode.has("url")) {
                    format.downloadURL = formatNode.get("url").asText();
                }
                if (formatNode.has("contentLength")) {
                    format.contentLength = formatNode.get("contentLength").asLong();
                }
                format.bitrate = formatNode.get("bitrate").asInt();
                if (formatNode.has("width")) {
                    format.width = formatNode.get("width").asInt();
                }
                if (formatNode.has("height")) {
                    format.height = formatNode.get("height").asInt();
                }
                if (formatNode.has("fps")) {
                    format.fps = formatNode.get("fps").asInt();
                }

                videoFormats.add(format);
            }
        }

        videoFormats.sort((a, b) -> {
            if (a.height != b.height) {
                return b.height - a.height;
            }

            if ("mp4".equals(a.fileExtension) && !a.fileExtension.equals(b.fileExtension)) {
                return -1;
            }
            if ("mp4".equals(b.fileExtension) && !a.fileExtension.equals(b.fileExtension)) {
                return 1;
            }

            if (a.fps != b.fps) {
                return b.fps - a.fps;
            }

            return b.bitrate - a.bitrate;
        });

        return videoFormats;
    }

    private List<AudioFormat> fetchAudioFormats(JsonNode streamingDataNode) {
        JsonNode adaptiveFormatsNode = streamingDataNode.get("adaptiveFormats");

        List<AudioFormat> audioFormats = new ArrayList<>();

        for (JsonNode formatNode : adaptiveFormatsNode) {
            // Strange, but happens sometimes
            if (!formatNode.has("url")) {
                continue;
            }

            if (formatNode.get("mimeType").asText().startsWith("audio")) {
                AudioFormat format = new AudioFormat();

                format.mimeType = formatNode.get("mimeType").asText();
                if (format.mimeType.startsWith("audio/mp4")) {
                    format.fileExtension = "m4a";
                } else if (format.mimeType.startsWith("audio/webm")) {
                    format.fileExtension = "opus";
                } else {
                    // Unsupported video format
                    continue;
                }

                format.iTag = formatNode.get("itag").asInt();
                format.downloadURL = formatNode.get("url").asText();
                format.contentLength = formatNode.get("contentLength").asLong();
                format.bitrate = formatNode.get("bitrate").asInt();
                format.sampleRate = formatNode.get("audioSampleRate").asInt();
                format.channels = formatNode.get("audioChannels").asInt();

                audioFormats.add(format);
            }
        }

        audioFormats.sort((a, b) -> {
            if (a.sampleRate != b.sampleRate) {
                return b.sampleRate - a.sampleRate;
            } else if (a.channels != b.channels) {
                return b.channels - a.channels;
            }

            if ("m4a".equals(a.fileExtension) && !a.fileExtension.equals(b.fileExtension)) {
                return -1;
            }
            if ("m4a".equals(b.fileExtension) && !a.fileExtension.equals(b.fileExtension)) {
                return 1;
            }

            return b.bitrate - a.bitrate;
        });

        return audioFormats;
    }


    public static class Result {
        public boolean unsupported;
        public File resultFile;
        public boolean completed;
        public boolean notFound;
    }

    public static class Format {
        @SuppressWarnings("unused")
        protected int iTag;

        protected int bitrate;

        protected String downloadURL;
        protected long contentLength;
        protected String mimeType;
        protected String fileExtension;

        @Override
        public String toString() {
            return "[" + fileExtension + ", " + (bitrate / 1024) + " kbps]";
        }
    }

    public static class VideoFormat extends Format {
        private Types type;
        private int width;
        private int height;
        private int fps;

        @Override
        public String toString() {
            return "[" + fileExtension + ", " + width + "x" + height + "@" + fps + ", " + (bitrate / 1024) + " kbps, " + type.name() + "]";
        }

        public static enum Types {
            OrdinaryFile, OTF_Stream, Encrypted
        }
    }

    public static class AudioFormat extends Format {
        private int sampleRate;
        private int channels;

        @Override
        public String toString() {
            return "[" + fileExtension + ", " + sampleRate + "x" + channels + ", " + (bitrate / 1024) + " kbps]";
        }
    }
}
