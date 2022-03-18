package core.youtube;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
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
import utils.FormattingUtils;
import utils.JSONUtils;

public class YoutubeVideoHandler {
    private static final Pattern VIDEO_URL_PATTERN = Pattern.compile("^https?:\\/\\/www.youtube.com\\/watch\\?v=([0-9A-Za-z_-]+)&?.*$");
    private static final int UPDATE_PROGRESS_INTERVAL = 100;

    private boolean printProgress;

    public void setPrintProgress(boolean printProgress) {
        this.printProgress = printProgress;
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

    public Result downloadVideo(YoutubeVideo video, String downloadChannelPath, String temporaryFolderPath) throws IOException {
        Result result = new Result();

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

        // Downloading Files itself
        String videoFileDownloadPrefix = temporaryFolderPath + "/" + video.getVideoId() + "_video";
        String audioFileDownloadPrefix = temporaryFolderPath + "/" + video.getVideoId() + "_audio";
        String temporaryFilePath = temporaryFolderPath + "/" + video.getVideoId();

        // Downloading Video file
        if (printProgress) {
            System.out.print("\n\tDownloading video... ");
        }
        Result downloadVideoResult = downloadFile(videoFileDownloadPrefix, temporaryFilePath, videoFormats);
        if (downloadVideoResult.notFound) {
            if (printProgress) {
                System.out.print("Not found...");
            }
            result.notFound = true;
            return result;
        }
        result.completed = downloadVideoResult.completed;

        // Downloading Audio file
        if (printProgress) {
            System.out.print("\n\tDownloading audio... ");
        }
        Result downloadAudioResult = downloadFile(audioFileDownloadPrefix, temporaryFilePath, audioFormats);
        if (downloadAudioResult.notFound) {
            if (printProgress) {
                System.out.print("Not found...");
            }
            result.notFound = true;
            return result;
        }
        result.completed &= downloadAudioResult.completed;

        // Combine Video and Audio via ffmpeg
        if (printProgress) {
            System.out.print("\n\tCombining video and audio tracks via ffmpeg... ");
        }
        String videoTrackPath = downloadVideoResult.resultFile.getAbsolutePath();
        String audioTrackPath = downloadAudioResult.resultFile.getAbsolutePath();
        String ffmpegResultPath = temporaryFolderPath + "/" + video.getVideoId() + "_combined.mp4";
        result.completed &= FFMPEGUtils.combineVideoAndAudio(videoTrackPath, audioTrackPath, ffmpegResultPath);
        if (printProgress) {
            System.out.print(result.completed ? "Done" : "Failed");
        }
        result.completed &= FileUtils.moveFile(new File(ffmpegResultPath), new File(downloadChannelPath, video.getFilename()));
        result.completed &= downloadVideoResult.resultFile.delete();
        result.completed &= downloadAudioResult.resultFile.delete();

        return result;
    }

    private List<VideoFormat> fetchVideoFormats(JsonNode streamingDataNode) {
        JsonNode adaptiveFormatsNode = streamingDataNode.get("adaptiveFormats");

        List<VideoFormat> videoFormats = new ArrayList<>();

        for (JsonNode formatNode : adaptiveFormatsNode) {
            // Strange, but happens sometimes
            if (!formatNode.has("url")) {
                continue;
            }

            if (formatNode.get("mimeType").asText().startsWith("video")) {
                VideoFormat format = new VideoFormat();

                format.mimeType = formatNode.get("mimeType").asText();
                if (format.mimeType.startsWith("video/mp4")) {
                    format.fileExtension = "mp4";
                } else if (format.mimeType.startsWith("video/webm")) {
                    format.fileExtension = "webm";
                } else {
                    // Unsupported video format
                    continue;
                }

                format.iTag = formatNode.get("itag").asInt();
                format.downloadURL = formatNode.get("url").asText();
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

    private Result downloadFile(String downloadFilePrefix, String temporaryFilePath, List<? extends Format> formats) throws IOException {
        Result result = new Result();

        File tempFile = new File(temporaryFilePath);
        // TODO Download resume support

        Iterator<? extends Format> formatIterator = formats.iterator();
        Format format = null;
        String videoFilename;
        File resultFile = null;
        HttpURLConnection connection = null;
        int responseCode = 404;
        while (responseCode == 404 && formatIterator.hasNext()) {
            format = formatIterator.next();

            videoFilename = downloadFilePrefix + "." + format.fileExtension;
            resultFile = new File(videoFilename);
            result.resultFile = resultFile;
            if (resultFile.exists()) {
                resultFile.delete();
            }

            URL url = new URL(format.downloadURL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(10 * 1000);
            connection.setReadTimeout(60 * 1000);
            responseCode = connection.getResponseCode();
            if (connection.getResponseCode() == 404) {
                format = formatIterator.next();
            }
        }

        if (responseCode == 404) {
            result.notFound = true;
            return result;
        }

        long contentLength = connection.getContentLengthLong();
        if (printProgress) {
            System.out.print(format + " ");
            System.out.print(connection.getResponseCode() + " " + connection.getResponseMessage() + "... ");
            System.out.print("Content-Length: " + contentLength + "... ");
        }
        byte[] buffer = new byte[65536];
        try (InputStream is = connection.getInputStream();
                OutputStream os = new FileOutputStream(tempFile)) {

            long fileSize = contentLength;

            long fileDownloadStartTime = System.currentTimeMillis();
            long lastStatusUpdate = fileDownloadStartTime;
            long readTotal = 0;
            int lastMessageLength = 0;

            int read = 0;
            while (read != -1) {
                read = is.read(buffer);
                if (read > 0) {
                    os.write(buffer, 0, read);
                    readTotal += read;
                }

                if (printProgress) {
                    long now = System.currentTimeMillis();
                    if (now - lastStatusUpdate >= UPDATE_PROGRESS_INTERVAL) {
                        long periodLength = now - fileDownloadStartTime;
                        // Calculating download speed
                        long speed = readTotal * 1000 / periodLength;

                        // Formatting progress message
                        StringBuilder message = new StringBuilder();
                        message.append(FormattingUtils.humanReadableSize(readTotal))
                                .append("b of ")
                                .append(FormattingUtils.humanReadableSize(fileSize) + "b (");
                        message.append(readTotal * 100 / fileSize).append("%), ");
                        message.append(FormattingUtils.humanReadableSize(speed) + "b/s");
                        while (message.length() < lastMessageLength) {
                            message.append(" ");
                        }

                        // Moving cursor at the beginning of the previous progress message
                        for (int i = 0; i < lastMessageLength; i++) {
                            System.out.print("\b");
                        }

                        // Printing the progress message
                        System.out.print(message);

                        lastMessageLength = message.length();
                        lastStatusUpdate = now;
                    }
                }
            }

            if (printProgress) {
                // Moving cursor at the beginning of the previous progress message
                for (int i = 0; i < lastMessageLength; i++) {
                    System.out.print("\b");
                }

                // Final progress report on download end
                long now = System.currentTimeMillis();
                long fileDownloadTime = now - fileDownloadStartTime;
                long speed = fileSize * 1000 / fileDownloadTime;
                StringBuilder message = new StringBuilder();
                message.append(FormattingUtils.humanReadableSize(fileSize))
                        .append("b in ")
                        .append(fileDownloadTime / 1000).append(" s, ");
                message.append("average speed: ")
                        .append(FormattingUtils.humanReadableSize(speed)).append("b/s");
                while (message.length() < lastMessageLength) {
                    message.append(" ");
                }
                System.out.print(message);
            }
        }

        if (tempFile.length() != contentLength) {
            throw new IOException("Incorrect file size after download, file: \"" + tempFile.getAbsolutePath()
                + "\", expected: " + contentLength + ", actual: " + tempFile.length());
        }

        result.completed = FileUtils.moveFile(tempFile, resultFile);

        return result;
    }

    public static class Result {
        public File resultFile;
        public boolean completed;
        public boolean notFound;
    }

    private static class Format {
        @SuppressWarnings("unused")
        protected int iTag;

        protected int bitrate;

        protected String downloadURL;
        protected String mimeType;
        protected String fileExtension;

        @Override
        public String toString() {
            return "[" + fileExtension + ", " + (bitrate / 1024) + " kbps]";
        }
    }

    private static class VideoFormat extends Format {
        private int width;
        private int height;
        private int fps;

        @Override
        public String toString() {
            return "[" + fileExtension + ", " + width + "x" + height + "@" + fps + ", " + (bitrate / 1024) + " kbps]";
        }
    }

    private static class AudioFormat extends Format {
        private int sampleRate;
        private int channels;

        @Override
        public String toString() {
            return "[" + fileExtension + ", " + sampleRate + "x" + channels + ", " + (bitrate / 1024) + " kbps]";
        }
    }
}
