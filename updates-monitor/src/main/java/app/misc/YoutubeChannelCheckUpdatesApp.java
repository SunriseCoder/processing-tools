package app.misc;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;

import core.dto.youtube.YoutubeChannel;
import core.dto.youtube.YoutubePlaylist;
import core.dto.youtube.YoutubeVideo;
import core.youtube.YoutubeChannelHandler;
import core.youtube.YoutubeChannelHandler.Result;
import utils.JSONUtils;

public class YoutubeChannelCheckUpdatesApp {
    public static void main(String[] args) throws IOException {
        System.out.print("Enter Youtube Channel ID or URL: ");
        Scanner scanner = new Scanner(System.in);
        String input = scanner.next();
        scanner.close();

        Result channelResult = YoutubeChannelHandler.fetchNewChannel(input);
        if (channelResult.channelRestricted) {
            System.out.println("Channel is restricted");
            System.exit(-1);
        } else if (channelResult.channelNotFound) {
            System.out.println("Channel not found");
            System.exit(-1);
        }
        YoutubeChannel channel = channelResult.youtubeChannel;

        Result updatesResult = YoutubeChannelHandler.checkUpdates(channel);

        System.out.println("Done");

        List<YoutubePlaylist> playlists = updatesResult.newPlaylists;
        JSONUtils.saveToDisk(playlists, new File("playlists.json"));
        System.out.println("Playlists saved: " + playlists.size());

        List<YoutubeVideo> videos = updatesResult.newVideos;
        JSONUtils.saveToDisk(videos, new File("videos.json"));
        System.out.println("Videos saved: " + videos.size());
    }
}
