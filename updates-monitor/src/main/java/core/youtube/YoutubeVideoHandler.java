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

import core.dto.youtube.YoutubeAudioFormat;
import core.dto.youtube.YoutubeDownloadDetails;
import core.dto.youtube.YoutubeResult;
import core.dto.youtube.YoutubeVideo;
import core.dto.youtube.YoutubeVideoFormat;
import core.dto.youtube.YoutubeVideoFormatTypes;
import util.DownloadUtils;
import util.PageParsing;
import utils.FileUtils;
import utils.JSONUtils;
import utils.ThreadUtils;

public class YoutubeVideoHandler {
    private static final Pattern VIDEO_URL_PATTERN = Pattern.compile("^https?:\\/\\/www.youtube.com\\/watch\\?v=([0-9A-Za-z_-]+)&?.*$");

    private AbstractYoutubeFileDownloader youtubeOrdinaryVideoDownloader;
    private AbstractYoutubeFileDownloader youtubeOFTVideoDownloader;
    private AbstractYoutubeFileDownloader youtubeEncryptedVideoDownloader;

    public YoutubeVideoHandler() {
        youtubeOrdinaryVideoDownloader = new YoutubeOrdinaryVideoDownloader();
        youtubeOFTVideoDownloader = new YoutubeOTFVideoDownloader();
        youtubeEncryptedVideoDownloader = new YoutubeEncryptedVideoDownloader();
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

    public YoutubeResult scanVideo(YoutubeVideo video) {
        YoutubeResult result = new YoutubeResult();

        boolean successful = false;
        do {
            // Downloading Video Page
            String urlString = "https://www.youtube.com/watch?v=" + video.getVideoId();
            try {
                DownloadUtils.Response response = DownloadUtils.downloadPageByGet(urlString, null);
                if (response.responseCode == 404) {
                    result.notFound = true;
                    return result;
                } else if (response.responseCode != 200) {
                    throw new IOException("\nError downloading page: \"" + urlString + "\", HTTP Status Code: " + response.responseCode);
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
                video.setChannelId(videoDetailsNode.get("channelId").asText());
                video.setTitle(videoDetailsNode.get("title").asText());
                video.setDescription(videoDetailsNode.get("shortDescription").asText());
                video.setDurationInSeconds(Integer.parseInt(videoDetailsNode.get("lengthSeconds").asText()));
                String videoUploadDate = playerResponseNode.get("microformat").get("playerMicroformatRenderer").get("uploadDate").asText();
                video.setUploadDate(videoUploadDate);

                JsonNode streamingDataNode = playerResponseNode.get("streamingData");
                List<YoutubeVideoFormat> videoFormats = fetchVideoFormats(streamingDataNode);
                YoutubeVideoFormat videoFormat = videoFormats.isEmpty() ? null : videoFormats.get(0);
                YoutubeVideoFormatTypes videoFormatType = videoFormat == null ? YoutubeVideoFormatTypes.NotAdaptive : videoFormat.type;
                video.setVideoFormatType(videoFormatType);

                video.setScanned(true);

                result.jsonNode = playerResponseNode;
                result.videoFormat = videoFormat;
                successful = true;
            } catch (Exception e) {
                System.out.println("\n" + e.getClass() + ": " + e.getMessage());
                e.printStackTrace();
                ThreadUtils.sleep(5000);
            }
        } while (!successful);

        return result;
    }

    public YoutubeResult downloadVideo(YoutubeVideo video, String downloadChannelPath, String temporaryFolderPath) throws Exception {
        YoutubeResult result = scanVideo(video);

        String videoFilename = downloadChannelPath + "/" + video.getVideoId() + "_" + FileUtils.getSafeFilename(video.getTitle()) + ".mp4";
        video.setFilename(videoFilename);

        // Fetching Media Formats
        JsonNode playerResponseNode = result.jsonNode;
        JsonNode streamingDataNode = playerResponseNode.get("streamingData");
        List<YoutubeAudioFormat> audioFormats = fetchAudioFormats(streamingDataNode);
        YoutubeVideoFormat videoFormat = result.videoFormat;
        YoutubeAudioFormat audioFormat = audioFormats.isEmpty() ? null : audioFormats.get(0);

        String temporaryFilePath = temporaryFolderPath + "/" + video.getVideoId();
        YoutubeDownloadDetails downloadDetails = new YoutubeDownloadDetails().setVideoId(video.getVideoId())
                .setTemporaryFilePath(temporaryFilePath)
                .setVideoFormat(videoFormat).setAudioFormat(audioFormat);

        // Downloading the Files
        switch (videoFormat.type) {
        case OrdinaryFile:
            result = youtubeOrdinaryVideoDownloader.download(video, downloadDetails);
            break;
        case OTF_Stream:
            result = youtubeOFTVideoDownloader.download(video, downloadDetails);
            break;
        case Encrypted:
            result = youtubeEncryptedVideoDownloader.download(video, downloadDetails);
            break;
        default:
            System.out.println("Unsupported video format: " + video.getVideoId()
                    + ", format type: " + (videoFormat.type == null ? null : videoFormat.type.name()));
            result.unsupported = true;
            return result;
        }

        return result;
    }

    private List<YoutubeVideoFormat> fetchVideoFormats(JsonNode streamingDataNode) {
        List<YoutubeVideoFormat> videoFormats = new ArrayList<>();
        if (streamingDataNode == null || !streamingDataNode.has("adaptiveFormats")) {
            return videoFormats;
        }

        JsonNode adaptiveFormatsNode = streamingDataNode.get("adaptiveFormats");
        for (JsonNode formatNode : adaptiveFormatsNode) {
            if (formatNode.get("mimeType").asText().startsWith("video")) {
                YoutubeVideoFormat format = new YoutubeVideoFormat();

                if (!formatNode.has("url")) {
                    if (formatNode.has("type") && "FORMAT_STREAM_TYPE_OTF".equals(formatNode.get("type").asText())) {
                        format.type = YoutubeVideoFormatTypes.OTF_Encrypted;
                    } else {
                        format.type = YoutubeVideoFormatTypes.Encrypted;
                    }
                } else if (formatNode.has("type") && "FORMAT_STREAM_TYPE_OTF".equals(formatNode.get("type").asText())) {
                    format.type = YoutubeVideoFormatTypes.OTF_Stream;
                } else {
                    format.type = YoutubeVideoFormatTypes.OrdinaryFile;
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

    private List<YoutubeAudioFormat> fetchAudioFormats(JsonNode streamingDataNode) {
        JsonNode adaptiveFormatsNode = streamingDataNode.get("adaptiveFormats");

        List<YoutubeAudioFormat> audioFormats = new ArrayList<>();

        for (JsonNode formatNode : adaptiveFormatsNode) {
            if (formatNode.get("mimeType").asText().startsWith("audio")) {
                YoutubeAudioFormat format = new YoutubeAudioFormat();

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
                if (formatNode.has("url")) {
                    format.downloadURL = formatNode.get("url").asText();
                }
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
}
