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

        YoutubeVideoHandler handler = new YoutubeVideoHandler();
        handler.setPrintProgress(true);
        String videoId = input.toLowerCase().startsWith("http") ? handler.parseVideoId(input) : input;

        YoutubeVideo video = new YoutubeVideo();
        video.setVideoId(videoId);
        handler.downloadVideo(video, "single-video", "tmp");

        System.out.println("\nProgram is done, type anything and then press Enter to close");
        scanner.next();
        scanner.close();
    }
}
