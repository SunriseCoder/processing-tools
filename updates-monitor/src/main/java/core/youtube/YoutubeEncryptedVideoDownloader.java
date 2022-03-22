package core.youtube;

import util.YoutubeDLUtils;

public class YoutubeEncryptedVideoDownloader extends YoutubeOrdinaryVideoDownloader {
    @Override
    protected String getFreshDownloadUrl(String videoId, int iTag) {
        String downloadURL = YoutubeDLUtils.getDownloadURL(videoId, iTag);
        return downloadURL;
    }
}
