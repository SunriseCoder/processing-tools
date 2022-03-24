package app.misc;

import java.util.Scanner;

import core.dto.youtube.YoutubeResult;
import core.dto.youtube.YoutubeVideo;
import core.youtube.YoutubeVideoHandler;

public class DownloadSingleYoutubeVideoApp {
    public static void main(String[] args) throws Exception {
        System.out.print("Enter Youtube Video URL or Video ID: ");
        Scanner scanner = new Scanner(System.in);
        String input = scanner.next();

        YoutubeVideoHandler handler = new YoutubeVideoHandler();
        String videoId = input.toLowerCase().startsWith("http") ? handler.parseVideoId(input) : input;

        YoutubeVideo video = new YoutubeVideo();
        video.setVideoId(videoId);
        System.out.print("Downloading video: 1 of 1 : " + video.getChannelId() + " - " + video + "... ");
        YoutubeResult result = handler.downloadVideo(video, "single-video", "tmp");
        System.out.println(result.queued ? " Queued" : result.completed ? " Done" : " Failed");

        System.out.println("Starting to handle download Queue");
        handler.takeCareOfOTFQueue(null);
        System.out.println("Queue is done");

        System.out.println("\nProgram is done, type anything and then press Enter to close");
        scanner.next();
        scanner.close();
    }
}
