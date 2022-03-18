package app.misc;

import java.io.IOException;
import java.util.Scanner;

import core.dto.YoutubeVideo;
import core.youtube.YoutubeVideoHandler;

public class DownloadSingleYoutubeVideoApp {
    public static void main(String[] args) throws IOException {
        System.out.print("Enter Youtube Video URL or Video ID: ");
        Scanner scanner = new Scanner(System.in);
        String input = scanner.next();
        scanner.close();

        YoutubeVideoHandler handler = new YoutubeVideoHandler();
        String videoId = input.toLowerCase().startsWith("http") ? handler.parseVideoId(input) : input;

        YoutubeVideo video = new YoutubeVideo();
        video.setVideoId(videoId);
        handler.downloadVideo(video, "tmp", "tmp");
    }
}
