package app;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;

import core.dto.YoutubeChannel;
import core.dto.YoutubeVideo;
import core.youtube.YoutubeChannelHandler;
import core.youtube.YoutubeChannelHandler.Result;
import utils.JSONUtils;

public class DownloadYoutubeChannelVideosListApp {
    public static void main(String[] args) throws IOException {
        System.out.print("Enter Youtube Channel URL: ");
        Scanner scanner = new Scanner(System.in);
        String input = scanner.next();
        scanner.close();

        Result channelResult = YoutubeChannelHandler.fetchNewChannel(input);
        if (channelResult.channelNotFound) {
            System.out.println("Channel not found");
            System.exit(-1);
        }
        YoutubeChannel channel = channelResult.youtubeChannel;

        Result updatesResult = YoutubeChannelHandler.checkUpdates(channel);
        List<YoutubeVideo> videos = updatesResult.newVideos;
        JSONUtils.saveToDisk(videos, new File("videos.json"));
        System.out.println("Done, " + videos.size() + " videos has been saved");
    }
}
