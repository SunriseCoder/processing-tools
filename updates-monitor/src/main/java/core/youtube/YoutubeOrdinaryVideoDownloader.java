package core.youtube;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.fasterxml.jackson.databind.JsonNode;

import core.dto.youtube.YoutubeDownloadDetails;
import core.dto.youtube.YoutubeResult;
import core.dto.youtube.YoutubeVideo;
import util.DownloadUtils;
import util.FFMPEGUtils;
import util.PageParsing;
import utils.FileUtils;
import utils.JSONUtils;
import utils.ThreadUtils;

public class YoutubeOrdinaryVideoDownloader extends AbstractYoutubeFileDownloader {

    @Override
    protected YoutubeResult download(YoutubeVideo video, YoutubeDownloadDetails downloadDetails) throws Exception {
        YoutubeResult result = new YoutubeResult();
        logger = new PrintWriter("logs/" + getClass().getName() + "-" + video.getVideoId() + "-logging.log");

        // Downloading Video and Audio Tracks
        YoutubeResult downloadVideoResult = downloadVideo(downloadDetails);
        YoutubeResult downloadAudioResult = downloadAudio(downloadDetails);
        if (!downloadVideoResult.completed || !downloadAudioResult.completed) {
            logger.close();
            return result;
        }

        // Combining Video and Audio Tracks into a single video file
        YoutubeResult combiningResult = combiningVideoAndAudio(downloadVideoResult.resultFile, downloadAudioResult.resultFile, downloadDetails);
        if (!combiningResult.completed) {
            logger.close();
            return result;
        }

        // Moving Temp file to Destination folder
        System.out.print("\n\tMoving temporary file to the channel folder... ");
        result.resultFile = new File(video.getFilename());
        result.completed = FileUtils.moveFile(combiningResult.resultFile, result.resultFile);
        System.out.print(result.completed ? "Done" : "Failed");
        if (result.completed) {
            video.setDownloaded(true);
        }

        logger.close();
        return result;
    }

    private YoutubeResult downloadVideo(YoutubeDownloadDetails downloadDetails) throws Exception {
        YoutubeResult result = new YoutubeResult();

        log("Downloading Video: Content-Length = " + downloadDetails.getVideoFormat().contentLength);

        // Creating Result File
        String videoFilename = downloadDetails.getTemporaryFilePath() + "_video." + downloadDetails.getVideoFormat().fileExtension;
        result.resultFile = new File(videoFilename);
        if (result.resultFile.exists()) {
            result.resultFile.delete();
        }

        // Downloading Video file
        System.out.print("\n\tDownloading video track... ");
        File tempFile = new File(downloadDetails.getTemporaryFilePath());
        long fileSize = downloadDetails.getVideoFormat().contentLength;
        result.completed = downloadFile(tempFile, fileSize, downloadDetails.getVideoId(), downloadDetails.getVideoFormat().iTag);
        System.out.print(result.completed ? " Done" : " Failed");
        if (!result.completed) {
            return result;
        }

        // Moving Temporary File to Result File
        result.completed &= FileUtils.moveFile(tempFile, result.resultFile);

        return result;
    }

    private YoutubeResult downloadAudio(YoutubeDownloadDetails downloadDetails) throws Exception {
        YoutubeResult result = new YoutubeResult();

        log("Downloading Audio: Content-Length = " + downloadDetails.getVideoFormat().contentLength);

        // Creating Result File
        String audioFilename = downloadDetails.getTemporaryFilePath() + "_audio." + downloadDetails.getAudioFormat().fileExtension;
        result.resultFile = new File(audioFilename);
        if (result.resultFile.exists()) {
            result.resultFile.delete();
        }

        // Downloading Audio file
        System.out.print("\n\tDownloading audio track... ");
        File tempFile = new File(downloadDetails.getTemporaryFilePath());
        long fileSize = downloadDetails.getAudioFormat().contentLength;
        result.completed = downloadFile(tempFile, fileSize, downloadDetails.getVideoId(), downloadDetails.getAudioFormat().iTag);
        System.out.print(result.completed ? " Done" : " Failed");
        if (!result.completed) {
            return result;
        }

        // Moving Temporary File to Result File
        result.completed &= FileUtils.moveFile(tempFile, result.resultFile);

        return result;
    }

    private YoutubeResult combiningVideoAndAudio(File videoFile, File audioFile, YoutubeDownloadDetails downloadDetails) {
        YoutubeResult result = new YoutubeResult();

        System.out.print("\n\tCombining video and audio tracks via ffmpeg... ");

        String ffmpegResultPath = downloadDetails.getTemporaryFilePath() + "_combined.mp4";
        result.resultFile = new File(ffmpegResultPath);
        result.completed = FFMPEGUtils.combineVideoAndAudio(videoFile.getAbsolutePath(),
                audioFile.getAbsolutePath(), ffmpegResultPath, downloadDetails.getVideoId());
        result.completed &= videoFile.delete();
        result.completed &= audioFile.delete();

        System.out.print(result.completed ? "Done" : "Failed");

        return result;
    }

    @Override
    protected String getFreshDownloadUrl(String videoId, int iTag) {
        boolean successful = false;
        String result = null;
        do {
            try {
                String urlString = "https://www.youtube.com/watch?v=" + videoId;
                DownloadUtils.Response response = DownloadUtils.downloadPageByGet(urlString, null);
                if (response.responseCode == 404) {
                    throw new IOException("404 Not found by refresihng download URL");
                } else if (response.responseCode != 200) {
                    throw new IOException("Error refreshing URL: \"" + urlString + "\", HTTP Status Code: " + response.responseCode);
                }
                Document videoPage = Jsoup.parse(response.body);

                // Fetching Video Details
                List<String> videoDetailsScriptSection = PageParsing.exctractSectionsFromPage(videoPage, "script", "var ytInitialPlayerResponse = ");
                if (videoDetailsScriptSection.size() > 1) {
                    throw new IllegalStateException("More than 1 section has been found when the only section is expected, probably Youtube API was changed");
                }
                String videoDetailsJsonString = JSONUtils.extractJsonSubstringFromString(videoDetailsScriptSection.get(0));
                JsonNode playerResponseNode = JSONUtils.parseJSON(videoDetailsJsonString);

                // Fetching Media Formats
                JsonNode streamingDataNode = playerResponseNode.get("streamingData");
                JsonNode adaptiveFormatsNode = streamingDataNode.get("adaptiveFormats");
                for (JsonNode formatNode : adaptiveFormatsNode) {
                    if (formatNode.get("itag").asInt() == iTag) {
                        result = formatNode.get("url").asText();
                        successful = true;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                ThreadUtils.sleep(5000);
            }
        } while (!successful || result == null || result.isEmpty());

        return result;
    }
}
