package app.misc;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;

import core.dto.youtube.YoutubePlaylist;
import core.dto.youtube.YoutubeVideo;
import core.youtube.YoutubePlaylistHandler;
import core.youtube.YoutubePlaylistHandler.Result;
import utils.JSONUtils;

public class YoutubePlaylistCheckUpdatesApp {
    public static void main(String[] args) throws IOException {
        System.out.print("Enter Youtube Playlist ID or URL: ");
        Scanner scanner = new Scanner(System.in);
        String input = scanner.next();
        scanner.close();

        Result playlistResult = YoutubePlaylistHandler.fetchNewPlaylist(input);
        if (playlistResult.playlistNotFound) {
            System.out.println("Playlist not found");
            System.exit(-1);
        }
        YoutubePlaylist playlist = playlistResult.youtubePlaylist;

        Result updatesResult = YoutubePlaylistHandler.checkUpdates(playlist);

        System.out.println("Done");

        List<YoutubeVideo> videos = updatesResult.newVideos;
        JSONUtils.saveToDisk(playlist, new File("playlist-videos.json"));
        System.out.println("Playlists saved: " + videos.size() + " video(s)");
    }
}
