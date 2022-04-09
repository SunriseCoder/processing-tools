package core.youtube;

import java.io.File;
import java.io.PrintWriter;

import core.dto.youtube.YoutubeDownloadDetails;
import core.dto.youtube.YoutubeResult;
import core.dto.youtube.YoutubeVideo;
import util.DownloadUtils;
import util.YoutubeDLUtils;
import utils.FileUtils;

public class YoutubeNonAdaptiveVideoDownloader extends AbstractYoutubeFileDownloader {

    @Override
    protected YoutubeResult download(YoutubeVideo video, YoutubeDownloadDetails downloadDetails) throws Exception {
        YoutubeResult result = new YoutubeResult();
        logger = new PrintWriter("logs/" + getClass().getName() + "-" + video.getVideoId() + "-logging.log");

        // Creating Result File
        result.resultFile = new File(video.getFilename());
        if (result.resultFile.exists()) {
            result.resultFile.delete();
        }

        // Downloading Video+Audio Track
        System.out.print("\n\tDownloading video+audio track... ");
        String videoFilename = downloadDetails.getTemporaryFilePath() + ".mp4";
        File tempFile = new File(videoFilename);
        String downloadUrl = getFreshDownloadUrl(video.getVideoId(), 0);
        long contentLength = DownloadUtils.getContentLength(downloadUrl);
        log("Downloading Video: Content-Length = " + contentLength);
        result.completed = downloadFile(tempFile, contentLength, downloadDetails.getVideoId(), 0);
        System.out.print(result.completed ? " Done" : " Failed");
        if (!result.completed) {
            return result;
        }

        // Moving Temporary File to Result File
        result.completed &= FileUtils.moveFile(tempFile, result.resultFile);
        if (result.completed) {
            video.setDownloaded(true);
            video.setFileSize(result.resultFile.length());
        }

        logger.close();
        return result;
    }

    @Override
    protected String getFreshDownloadUrl(String videoId, int iTag) {
        String downloadURL = YoutubeDLUtils.getNonAdaptiveDownloadURL(videoId);
        return downloadURL;
    }
}
