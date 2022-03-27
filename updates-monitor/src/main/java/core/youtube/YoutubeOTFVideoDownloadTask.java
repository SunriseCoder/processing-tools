package core.youtube;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.Callable;

import core.dto.Configuration;
import core.dto.youtube.YoutubeDownloadDetails;
import core.dto.youtube.YoutubeResult;
import core.dto.youtube.YoutubeVideo;
import util.DownloadUtils;
import util.DownloadUtils.Response;
import util.FFMPEGUtils;
import util.YoutubeDLUtils;
import utils.FileUtils;

public class YoutubeOTFVideoDownloadTask extends AbstractYoutubeFileDownloader implements Callable<YoutubeResult> {
    private YoutubeVideo video;
    private YoutubeDownloadDetails downloadDetails;

    public YoutubeOTFVideoDownloadTask(YoutubeVideo video, YoutubeDownloadDetails downloadDetails) {
        this.video = video;
        this.downloadDetails = downloadDetails;
    }

    public YoutubeVideo getVideo() {
        return video;
    }

    @Override
    public YoutubeResult call() throws Exception {
        YoutubeResult result = new YoutubeResult();
        logger = new PrintWriter("logs/" + getClass().getName() + "-" + video.getVideoId() + "-logging.log");

        logger.print("\n\tDownloading MPD Manifest... ");
        String manifestString = getMPDManifest(downloadDetails.getVideoId());
        result.completed = manifestString != null;
        logger.print(result.completed ? "Done" : "Failed");
        if (!result.completed) {
            result.reason = "Manifest download failed";
            return result;
        }

        logger.print("\n\tSaving MPD Manifest... ");
        String manifestFilename = downloadDetails.getTemporaryFilePath() + "_manifest.mpd";
        result.completed &= FileUtils.saveToFile(manifestString, manifestFilename);
        logger.print(result.completed ? "Done" : "Failed");
        if (!result.completed) {
            result.reason = "Manifest was not saved";
            return result;
        }

        logger.print("\n\tDownloading Stream via FFMPEG... ");
        String videoTempFilename = downloadDetails.getTemporaryFilePath() + ".mp4";
        result.completed &= FFMPEGUtils.muxMPDMManifest(manifestFilename, videoTempFilename, downloadDetails);
        if (!result.completed) {
            logger.print("FFMPEG failed, trying youtube-dl... ");
            result.completed |= downloadViaYoutubeDL(videoTempFilename);
        }
        logger.print(result.completed ? "Done" : "Failed");
        if (!result.completed) {
            result.reason = "FFMPEG and youtube-dl download failed";
            return result;
        }

        logger.print("\n\tMoving temporary file to the channel folder... ");
        result.resultFile = new File(video.getFilename());
        result.completed &= FileUtils.moveFile(new File(videoTempFilename), result.resultFile);
        new File(manifestFilename).delete();
        logger.print(result.completed ? "Done" : "Failed");
        if (result.completed) {
            video.setDownloaded(true);
            video.setFileSize(result.resultFile.length());
        } else {
            result.reason = "Move from temp to destination failed";
        }

        logger.close();
        return result;
    }

    private String getMPDManifest(String videoId) throws IOException {
        String manifestURL = YoutubeDLUtils.getOTFManifestURL(videoId);
        if (manifestURL == null) {
            throw new IOException("Couldn't get MPD-Manifest URL for video: " + videoId);
        }

        Response manifestResponse = DownloadUtils.downloadPageByGet(manifestURL, null, Configuration.getYoutubeCookies());
        if (manifestResponse.responseCode != 200) {
            throw new IOException("Response Code: " + manifestResponse.responseCode);
        }
        String manifestString = manifestResponse.body;

        return manifestString;
    }

    private boolean downloadViaYoutubeDL(String resultFilename) {
        boolean result;

        // Downloading Video Track
        String format = String.valueOf(downloadDetails.getVideoFormat().iTag);
        String videoTrackFilename = downloadDetails.getTemporaryFilePath()
                + "_video." + downloadDetails.getVideoFormat().fileExtension;
        String logSuffix = video.getVideoId() + "-video";
        result = YoutubeDLUtils.downloadVideo(video.getVideoId(), format, videoTrackFilename, logSuffix);
        File videoTrackFile = new File(videoTrackFilename);
        if (!result) {
            if (videoTrackFile.exists()) {
                videoTrackFile.delete();
            }
            return result;
        }

        // Downloading Audio Track
        format = String.valueOf(downloadDetails.getAudioFormat().iTag);
        String audioTrackFilename = downloadDetails.getTemporaryFilePath()
                + "_audio." + downloadDetails.getAudioFormat().fileExtension;
        logSuffix = video.getVideoId() + "-audio";
        result = YoutubeDLUtils.downloadVideo(video.getVideoId(), format, audioTrackFilename, logSuffix);
        File audioTrackFile = new File(audioTrackFilename);
        if (!result) {
            if (audioTrackFile.exists()) {
                audioTrackFile.delete();
            }
            return result;
        }

        // Muxing via FFMPEG
        String ffmpegResultFilename = downloadDetails.getTemporaryFilePath() + "_combined.mp4";
        File ffmpegResultFile = new File(ffmpegResultFilename);
        result = FFMPEGUtils.combineVideoAndAudio(videoTrackFilename, audioTrackFilename,
                ffmpegResultFilename, downloadDetails.getVideoId());
        if (!result) {
            if (ffmpegResultFile.exists()) {
                ffmpegResultFile.delete();
            }
            return result;
        }

        // Moving Temp File to Result File
        File resultFile = new File(resultFilename);
        if (resultFile.exists()) {
            resultFile.delete();
        }
        result = ffmpegResultFile.renameTo(resultFile);

        // Clean up
        result &= videoTrackFile.delete();
        result &= audioTrackFile.delete();

        return result;
    }

    @Override
    protected YoutubeResult download(YoutubeVideo video, YoutubeDownloadDetails downloadDetails) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    protected String getFreshDownloadUrl(String videoId, int iTag) {
        throw new UnsupportedOperationException();
    }
}
