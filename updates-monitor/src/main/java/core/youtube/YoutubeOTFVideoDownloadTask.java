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
            return result;
        }

        logger.print("\n\tSaving MPD Manifest... ");
        String manifestFilename = downloadDetails.getTemporaryFilePath() + "_manifest.mpd";
        result.completed &= FileUtils.saveToFile(manifestString, manifestFilename);
        logger.print(result.completed ? "Done" : "Failed");
        if (!result.completed) {
            return result;
        }

        logger.print("\n\tDownloading Stream via FFMPEG... ");
        String ffmpegResultFilename = downloadDetails.getTemporaryFilePath() + ".mp4";
        result.completed &= FFMPEGUtils.muxMPDMManifest(manifestFilename, ffmpegResultFilename, downloadDetails);
        logger.print(result.completed ? "Done" : "Failed");
        if (!result.completed) {
            return result;
        }

        logger.print("\n\tMoving temporary file to the channel folder... ");
        result.resultFile = new File(video.getFilename());
        result.completed &= FileUtils.moveFile(new File(ffmpegResultFilename), result.resultFile);
        new File(manifestFilename).delete();
        logger.print(result.completed ? "Done" : "Failed");
        if (result.completed) {
            video.setDownloaded(true);
            video.setFileSize(result.resultFile.length());
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

    @Override
    protected YoutubeResult download(YoutubeVideo video, YoutubeDownloadDetails downloadDetails) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    protected String getFreshDownloadUrl(String videoId, int iTag) {
        throw new UnsupportedOperationException();
    }
}
