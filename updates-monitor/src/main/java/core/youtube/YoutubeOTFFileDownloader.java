package core.youtube;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import core.dto.youtube.YoutubeDownloadDetails;
import core.dto.youtube.YoutubeResult;
import core.dto.youtube.YoutubeVideo;
import util.DownloadUtils;
import util.DownloadUtils.Response;
import util.FFMPEGUtils;
import util.YoutubeDLUtils;
import utils.FileUtils;

public class YoutubeOTFFileDownloader extends AbstractYoutubeFileDownloader {

    @Override
    protected YoutubeResult download(YoutubeVideo video, YoutubeDownloadDetails downloadDetails) throws Exception {
        YoutubeResult result = new YoutubeResult();

        logger = new PrintWriter("otf-stream-downloading-logging.log");

        System.out.print("\n\tDownloading MPD Manifest... ");
        String manifestString = getMPDManifest(downloadDetails.getVideoId());
        result.completed = manifestString != null;
        System.out.print(result.completed ? "Done" : "Failed");
        if (!result.completed) {
            logger.close();
            return result;
        }

        System.out.print("\n\tSaving MPD Manifest... ");
        String manifestFilename = downloadDetails.getTemporaryFilePath() + "_manifest.mpd";
        result.completed &= FileUtils.saveToFile(manifestString, manifestFilename);
        System.out.print(result.completed ? "Done" : "Failed");
        if (!result.completed) {
            logger.close();
            return result;
        }

        // Muxing MPD-Manifest using FFMPEG
        System.out.print("\n\tDownloading Stream via FFMPEG... ");
        String ffmpegResultFilename = downloadDetails.getTemporaryFilePath() + ".mp4";
        result.completed &= FFMPEGUtils.muxMPDMManifest(manifestFilename, ffmpegResultFilename, downloadDetails);
        System.out.print(result.completed ? "Done" : "Failed");
        if (!result.completed) {
            logger.close();
            return result;
        }

        // Moving Temp file to Destination folder
        System.out.print("\n\tMoving temporary file to the channel folder... ");
        result.resultFile = new File(video.getFilename());
        result.completed &= FileUtils.moveFile(new File(ffmpegResultFilename), result.resultFile);
        System.out.print(result.completed ? "Done" : "Failed");
        if (result.completed) {
            video.setDownloaded(true);
        }

        logger.close();
        return result;
    }

    private String getMPDManifest(String videoId) throws IOException {
        String manifestURL = YoutubeDLUtils.getOTFManifestURL(videoId);
        if (manifestURL == null) {
            throw new IOException("Couldn't get MPD-Manifest URL for video: " + videoId);
        }

        Response manifestResponse = DownloadUtils.downloadPageByGet(manifestURL, null);
        if (manifestResponse.responseCode != 200) {
            throw new IOException("Response Code: " + manifestResponse.responseCode);
        }
        String manifestString = manifestResponse.body;

        return manifestString;
    }

    @Override
    protected String getFreshDownloadUrl(String videoId, int iTag) {
        throw new UnsupportedOperationException("Operation not supported");
    }
}
