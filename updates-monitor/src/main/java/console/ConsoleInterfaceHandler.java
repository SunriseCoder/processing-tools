package console;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.TimeZone;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.StdDateFormat;

import core.dto.Database;
import core.dto.VideoPreview;
import core.dto.youtube.YoutubeChannel;
import core.dto.youtube.YoutubePlaylist;
import core.dto.youtube.YoutubeResult;
import core.dto.youtube.YoutubeVideo;
import core.dto.youtube.YoutubeVideoFormatTypes;
import core.youtube.YoutubeChannelHandler;
import core.youtube.YoutubePlaylistHandler;
import core.youtube.YoutubePlaylistHandler.Result;
import core.youtube.YoutubeVideoHandler;
import function.LambdaCommand;
import util.FFMPEGUtils;
import utils.FileUtils;
import utils.JSONUtils;
import utils.MathUtils;
import utils.ThreadUtils;

public class ConsoleInterfaceHandler {
    private static final String DATABASE_FOLDER = "database";
    private static final String DOWNLOAD_FOLDER = "download";
    private static final String PREVIEW_FOLDER = "preview";
    private static final String LOGS_FOLDER = "logs";
    private static final String TEMPORARY_FOLDER = "tmp";

    private Scanner scanner;
    private File databaseFile;
    private Database database;

    private YoutubeVideoHandler youtubeVideoHandler = new YoutubeVideoHandler();

    public ConsoleInterfaceHandler() {
        scanner = new Scanner(System.in);
    }

    public void start() throws Exception {
        System.out.println("Application is starting...");

        FileUtils.createFolderIfNotExists(DATABASE_FOLDER);
        FileUtils.createFolderIfNotExists(DOWNLOAD_FOLDER);
        FileUtils.createFolderIfNotExists(PREVIEW_FOLDER);
        FileUtils.createFolderIfNotExists(LOGS_FOLDER);
        FileUtils.createFolderIfNotExists(TEMPORARY_FOLDER);

        FileUtils.cleanupFolder(LOGS_FOLDER);
        FileUtils.cleanupFolder(TEMPORARY_FOLDER);

        System.out.println("Loading database...");
        databaseFile = new File(DATABASE_FOLDER, "database.json");
        if (databaseFile.exists()) {
            try {
                TypeReference<Database> typeReference = new TypeReference<Database>() {};
                database = JSONUtils.loadFromDisk(databaseFile, typeReference);
                database.linkEntities();
            } catch (Exception e) {
                System.out.println("Could not read database from file " + databaseFile.getAbsolutePath() + ", creating a new one");
                e.printStackTrace();
                database = new Database();
            }
        } else {
            System.out.println("Database file " + databaseFile.getAbsolutePath() + " not found, creating a new Database");
            database = new Database();
        }

        System.out.println("Application started successfully");

        mainMenu();
    }

    private void mainMenu() throws Exception {
        String input;
        while (true) {
            System.out.print("Select action: [1] Status, [2] Add resource, [4] Print Video list "
                    + "[5] Check updates, [6] Scan playlists, [7] Scan videos, [8] Re-scan videos, [9] Download files, [0] Exit\n"
                    + "[11] Fix Downloaded, [12] Make Previews ");
            input = scanner.next();
            switch (input) {
            case "1":
                printStatus();
                break;
            case "2":
                addResource();
                break;
            case "4":
                printVideoList();
                break;
            case "5":
                checkUpdates();
                break;
            case "6":
                scanPlaylists();
                break;
            case "7":
                scanVideoDetails();
                break;
            case "8":
                rescanVideoDetails();
                break;
            case "9":
                downloadAllFiles();
                break;
            case "0":
                System.out.println("Exiting...");
                scanner.close();
                System.exit(0);
            case "11":
                fixDownloaded();
                break;
            case "12":
                makePreviews();
                break;
            default:
                System.out.println("Unsupported command, please try again");
            }
        }
    }

    private void printStatus() {
        // Youtube Videos by Channels
        System.out.println("Youtube channels:");
        Collection<YoutubeChannel> youtubeChannels = database.getYoutubeChannels().values();
        for (YoutubeChannel channel : youtubeChannels) {
            System.out.println("\t" + channel);
            System.out.println("\t\t" + channel.getStatusString());
        }
        System.out.println(youtubeChannels.size() + " channel(s) total");

        System.out.println("Youtube Playlists: " + database.getYoutubePlaylists().size());

        // Youtube Videos Total
        StringBuilder sb = new StringBuilder();
        sb.append("Youtube Videos: ")
            .append("Not Scanned: ").append(database.getYoutubeNotScannedVideos().size())
            .append(", Not Downloaded: ").append(database.getYoutubeVideos().values().stream().filter(e -> !e.isDownloaded()).count())
            .append(", Done: ").append(database.getYoutubeVideos().values().stream().filter(e -> e.isDownloaded()).count())
            .append(", Total: ").append(database.getYoutubeVideos().size());
        System.out.println(sb);

        // Youtube Videos by Types
        sb = new StringBuilder();
        sb.append("Youtube Videos (Non-Downloaded) by Type: ");
        for (YoutubeVideoFormatTypes youtubeVideoType : YoutubeVideoFormatTypes.values()) {
            sb.append(youtubeVideoType.name()).append(": ")
                .append(database.getYoutubeVideos().values().stream()
                    .filter(e -> !e.isDownloaded() && youtubeVideoType.equals(e.getVideoFormatType())).count())
                .append(", ");
        }
        sb.delete(sb.length() - 2, sb.length());
        System.out.println(sb);
    }

    private void addResource() {
        String input;
        boolean inputAcceptedFlag = false;
        while (!inputAcceptedFlag) {
            System.out.println("Please enter resource URL or [0] for previous menu: ");
            input = scanner.next();
            input = input.trim();
            if ("0".equalsIgnoreCase(input)) {
                inputAcceptedFlag = true;
            } else if (YoutubeChannelHandler.isYoutubeChannelURL(input)) {
                // Adding Youtube Channel
                try {
                    System.out.print("\tDownloading channel info for URL: " + input + "... ");
                    YoutubeChannelHandler.Result youtubeChannelFetchResult = YoutubeChannelHandler.fetchNewChannel(input);
                    if (youtubeChannelFetchResult.channelRestricted) {
                        System.out.println("Channel is restricted");
                    } else if (youtubeChannelFetchResult.channelNotFound) {
                        System.out.println("Channel not found");
                    } else {
                        System.out.println("Done");
                        YoutubeChannel youtubeChannel = youtubeChannelFetchResult.youtubeChannel;
                        if (database.getYoutubeChannel(youtubeChannel.getChannelId()) == null) {
                            // Adding Youtube Channel to the Database
                            String foldername = youtubeChannel.getChannelId() + "_" + FileUtils.getSafeFilename(youtubeChannel.getTitle());
                            FileUtils.createFolderIfNotExists(DOWNLOAD_FOLDER, foldername);
                            youtubeChannel.setFoldername(foldername);
                            database.addYoutubeChannel(youtubeChannel);
                            saveDatabase();
                            System.out.println("\tYoutube Channel \"" + youtubeChannel + "\" has beed added successfully");
                        } else {
                            System.out.println("\tYoutube Channel \"" + youtubeChannel + "\" is already in the database");
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Could not add (" + input + ") as Youtube channel becase of: " + e.getMessage());
                }
            } else {
                inputAcceptedFlag = false;
                System.out.println("Unsupported resource URL: (" + input + ")");
            }
        }
    }

    private void printVideoList() throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("Choose YoutubeVideoType for list:\n")
                .append("[-2] All non-scanned\n")
                .append("[-1] All non-downloaded\n");

        for (YoutubeVideoFormatTypes type : YoutubeVideoFormatTypes.values()) {
            sb.append("[").append(type.ordinal()).append("] - ").append(type.name()).append("\n");
        }
        sb.append("[").append(YoutubeVideoFormatTypes.values().length).append("] - Exit to previous menu");
        System.out.println(sb);

        int input = Integer.parseInt(scanner.next());
        if (input == YoutubeVideoFormatTypes.values().length) {
            return;
        }

        List<YoutubeVideo> videos;
        switch (input) {
        case -2:
            videos = database.getYoutubeVideos().values().stream()
                    .filter(e -> !e.isScanned()).collect(Collectors.toList());
            break;
        case -1:
            videos = database.getYoutubeVideos().values().stream()
                    .filter(e -> !e.isDownloaded()).collect(Collectors.toList());
            break;
        default:
            YoutubeVideoFormatTypes type = YoutubeVideoFormatTypes.values()[input];
            videos = database.getYoutubeVideos().values().stream()
                    .filter(e -> type.equals(e.getVideoFormatType())).collect(Collectors.toList());
        }

        for (YoutubeVideo video : videos) {
            YoutubeChannel channel = database.getYoutubeChannel(video.getChannelId());
            System.out.println("Channel: " + (channel == null ? video.getChannelId() : channel) + ", video: " + video);
        }
        System.out.println("Videos total: " + videos.size());
    }

    private void checkUpdates() throws IOException {
        System.out.println("Updating Youtube Channels...");
        Iterator<Entry<String, YoutubeChannel>> youtubeChannelsIterator = database.getYoutubeChannels().entrySet().iterator();
        while (youtubeChannelsIterator.hasNext()) {
            Entry<String, YoutubeChannel> channelEntry = youtubeChannelsIterator.next();
            YoutubeChannel channel = channelEntry.getValue();
            boolean updateSuccess = false;
            boolean hasNewData = false;
            int attempts = 10;
            do {
                try {
                    System.out.print("\tUpdating Youtube Channel " + channel + "... ");
                    YoutubeChannelHandler.Result updateResult = YoutubeChannelHandler.checkUpdates(channel);
                    if (updateResult.channelRestricted) {
                        System.out.println("Channel is restricted");
                    } else if (updateResult.channelNotFound) {
                        System.out.println("Channel not found on Youtube!!!");
                    } else {
                        if (!updateResult.newTitle.equals(updateResult.oldTitle)) {
                            channel.setTitle(updateResult.newTitle);

                            // Renaming Channel Download Folder
                            String channelNewFoldername = channel.getChannelId() + "_" + FileUtils.getSafeFilename(updateResult.newTitle);
                            String channelOldFoldername = channel.getFoldername();
                            channel.setFoldername(channelNewFoldername);
                            if (channelOldFoldername != null && new File(DOWNLOAD_FOLDER, channelOldFoldername).exists()) {
                                FileUtils.renameOrCreateFileOrFolder(new File(DOWNLOAD_FOLDER, channelOldFoldername),
                                        new File(DOWNLOAD_FOLDER, channelNewFoldername));
                            } else {
                                FileUtils.createFolderIfNotExists(DOWNLOAD_FOLDER, channelNewFoldername);
                            }

                            System.out.println("\t\tYoutube Channel " + channel.getChannelId()
                                    + " Title was changed from \"" + updateResult.oldTitle + "\" to \"" + updateResult.newTitle + "\"");
                        }

                        // Update details about new videos
                        if (!updateResult.newVideos.isEmpty() || !updateResult.newPlaylists.isEmpty()) {
                            hasNewData = true;
                            for (YoutubeVideo youtubeVideo : updateResult.newVideos) {
                                database.addYoutubeVideo(youtubeVideo);
                            }
                            for (YoutubePlaylist youtubePlaylist : updateResult.newPlaylists) {
                                database.addYoutubePlaylist(youtubePlaylist);
                            }
                            System.out.println("\tFound: "
                                    + updateResult.newPlaylists.size() + " new playlist(s), "
                                    + updateResult.newVideos.size() + " new video(s)");
                        }

                    }
                    updateSuccess = true;
                } catch (Exception e) {
                    System.out.println("Error due to update channel: " + channel + ": " + e.getMessage());
                    e.printStackTrace();
                    ThreadUtils.sleep(5000);
                    attempts--;
                    if (attempts == 0) {
                        System.out.println("No attempts to retry left, skipping channel for now...");
                    }
                }
            } while (!updateSuccess);
            if (hasNewData) {
                database.linkEntities();
                saveDatabase();
            }
        }
    }

    private void scanPlaylists() throws IOException {
        List<YoutubePlaylist> playlists = new ArrayList<>(database.getYoutubePlaylists().values());
        for (int i = 0; i < playlists.size(); i++) {
            YoutubePlaylist playlist = playlists.get(i);
            YoutubeChannel channel = database.getYoutubeChannel(playlist.getChannelId());
            System.out.print("Scanning playlist: " + (i + 1) + " of " + playlists.size() + ": "
                    + (channel == null ? null : channel) + " --- " + playlist + "... ");
            Result result = YoutubePlaylistHandler.checkUpdates(playlist);
            if (result.playlistNotFound) {
                System.out.println("Failed");
            } else {
                System.out.println("Done");
            }
        }
        database.linkEntities();
        saveDatabase();
        System.out.println("Youtube Playlist scan is done");
    }

    private void scanVideoDetails() throws IOException {
        List<YoutubeVideo> videos = new ArrayList<>(database.getYoutubeNotScannedVideos().values());
        scanVideoDetails(videos);
    }

    private void rescanVideoDetails() throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("Choose YoutubeVideoType for rescan:\n");
        for (YoutubeVideoFormatTypes type : YoutubeVideoFormatTypes.values()) {
            sb.append("[").append(type.ordinal()).append("] - ").append(type.name()).append("\n");
        }
        sb.append("[").append(YoutubeVideoFormatTypes.values().length).append("] - Exit to previous menu");
        System.out.println(sb);

        int input = Integer.parseInt(scanner.next());
        if (input == YoutubeVideoFormatTypes.values().length) {
            return;
        }

        YoutubeVideoFormatTypes type = YoutubeVideoFormatTypes.values()[input];
        List<YoutubeVideo> videos = database.getYoutubeVideos().values().stream()
                .filter(e -> type.equals(e.getVideoFormatType())).collect(Collectors.toList());

        scanVideoDetails(videos);
    }

    private void scanVideoDetails(List<YoutubeVideo> videos) throws IOException {
        System.out.println("Scanning video details, to go: " + videos.size() + " video(s)...");

        for (int i = 0; i < videos.size(); i++) {
            YoutubeVideo video = videos.get(i);
            System.out.print("\tScanning video: " + (i + 1) + " of " + videos.size() + " : " + video.getVideoId() + "... ");
            YoutubeResult result = youtubeVideoHandler.scanVideo(video);
            if (result.notFound) {
                System.out.println("Failed. Response Code: 404 - Page not found");
            } else {
                saveDatabase();
                System.out.println("Done. Title: " + video.getTitle());
            }
        }

        System.out.println("Scanning video details is done");
    }

    private void downloadAllFiles() throws Exception {
        List<YoutubeVideo> youtubeVideos = database.getYoutubeVideos().values().stream()
                .filter(e -> !e.isDownloaded()).collect(Collectors.toList());
        downloadYoutubeVideos(youtubeVideos);
    }

    private void downloadYoutubeVideos(List<YoutubeVideo> videos) throws Exception {
        System.out.println("Downloading Youtube videos: " + videos.size() + " video(s) to go...");

        LambdaCommand saveDatabaseCommand = () -> {
            try {
                saveDatabase();
            } catch (IOException e) {
                e.printStackTrace();
            }
        };

        for (int i = 0; i < videos.size(); i++) {
            YoutubeVideo youtubeVideo = videos.get(i);
            YoutubeChannel youtubeChannel = database.getYoutubeChannel(youtubeVideo.getChannelId());
            YoutubeResult result = new YoutubeResult();

            // Downloading files and giving Tasks
            int attempts = 5;
            while (!result.completed && !result.notFound && attempts > 0) {
                System.out.print("Downloading video: " + (i + 1) + " of " + videos.size() + " : " + youtubeChannel + " - " + youtubeVideo + "... ");

                try {
                    String downloadPath = DOWNLOAD_FOLDER + "/" + youtubeChannel.getFoldername();
                    FileUtils.createFolderIfNotExists(downloadPath);
                    result = youtubeVideoHandler.downloadVideo(youtubeVideo, downloadPath, TEMPORARY_FOLDER);
                    if (result.queued) {
                        System.out.println("\n\tVideo has been queued: " + youtubeVideo);
                        break;
                    }

                    if (result.unsupported) {
                        System.out.println("Unsupported video format type: " + youtubeVideo.getVideoFormatType().name()
                                + " for video: " + youtubeVideo.getVideoId() + ", skipping...");
                        break;
                    }

                    if (result.notFound) {
                        System.out.println("Page or track not found for Video: " + youtubeVideo.getVideoId() + ", skipping...");
                        break;
                    }
                } catch (Exception e) {
                    System.out.println("Error: " + e.getMessage());
                    e.printStackTrace();
                    ThreadUtils.sleep(5000);
                }
                System.out.println();
                attempts--;
            }

            saveDatabase();

            // Checking OTF Streams Downloading Queue
            youtubeVideoHandler.doOTFQueueIteration(saveDatabaseCommand);
            System.out.println("Videos in the queued: " + youtubeVideoHandler.getQueueSize());
        }

        youtubeVideoHandler.takeCareOfOTFQueue(saveDatabaseCommand);
    }

    private void fixDownloaded() throws IOException {
        ArrayList<YoutubeVideo> videos = new ArrayList<>(database.getYoutubeVideos().values().stream()
                .filter(e -> e.isDownloaded() && !e.isDeleted()).collect(Collectors.toList()));

        int fixedCounter = 0;
        for (int i = 0; i < videos.size(); i++) {
            YoutubeVideo video = videos.get(i);
            System.out.print("Checking video " + (i + 1) + " of " + videos.size() + " - " + video + "... ");
            File videoFile = new File(video.getFilename());
            if (!videoFile.exists()) {
                System.out.print("Fixing... ");
                YoutubeChannel channel = database.getYoutubeChannel(video.getChannelId());
                String fixedFilename = DOWNLOAD_FOLDER + "/" + channel.getFoldername() + "/" + video.getFilename();
                videoFile = new File(fixedFilename);
                if (videoFile.exists()) {
                    video.setFilename(fixedFilename);
                    System.out.println("Fixed");
                    fixedCounter++;
                } else {
                    System.out.println("Couldn't fix file for video: " + video);
                    System.exit(-1);
                }
            } else {
                if (videoFile.length() != video.getFileSize()) {
                    video.setFileSize(videoFile.length());
                    fixedCounter++;
                }
                System.out.println(" Ok");
            }
        }

        if (fixedCounter > 0) {
            saveDatabase();
        }
        System.out.println("Fixing downloaded videos is done, fixed videos: " + fixedCounter);
    }

    private void makePreviews() throws IOException {
        // Retrieving Preview Database
        TypeReference<List<VideoPreview>> typeReference = new TypeReference<List<VideoPreview>>() {};
        File previewDatabaseFile = new File(DATABASE_FOLDER, "previews.json");
        List<VideoPreview> previews = previewDatabaseFile.exists()
                ? JSONUtils.loadFromDisk(previewDatabaseFile, typeReference) : new ArrayList<>();
        HashSet<String> previewSet = new HashSet<>(previews.stream().map(e -> e.getVideoId()).collect(Collectors.toList()));

        // Retrieving Videos From Database
        System.out.println("Retrieving video data from Database...");
        ArrayList<YoutubeVideo> videos = new ArrayList<>();
        database.getYoutubeVideos().values().stream()
                .filter(e -> e.isDownloaded() && !e.isDeleted() && !previewSet.contains(e.getVideoId()))
                .forEach(e -> videos.add(e)); // Changed this way due to some sorting problems

        System.out.println("Sorting videos by size descending...");
        videos.sort((a, b) -> MathUtils.sign(b.getFileSize() - a.getFileSize()));

        System.out.println("Starting making video previews...");
        for (int i = 0; i < videos.size(); i++) {
            YoutubeVideo video = videos.get(i);
            String previewResultPath = PREVIEW_FOLDER + "/" + video.getVideoId() + ".jpg";
            System.out.print("\tMaking preview for video " + (i + 1) + " of " + videos.size() + ": " + video + "... ");

            File previewResultFile = new File(previewResultPath);
            if (previewResultFile.exists()) {
                System.out.println("Already exists, skipping");
                continue;
            }

            // Making Preview Image
            double duration = FFMPEGUtils.getVideoDuration(video.getFilename());
            int interval = Math.min(MathUtils.roundToInt(duration / 28), 120);
            interval = Math.max(interval, 1);
            int cols = 6;
            int rows = MathUtils.ceilToInt(duration / interval / cols);
            boolean result = FFMPEGUtils.makePreview(video.getFilename(), previewResultPath, interval, cols, rows);
            if (!result) {
                System.out.println("Failed: FFMPEG error");
                System.exit(-1);
            }

            // Adding to Preview Database
            String title = database.getYoutubeVideos().get(video.getVideoId()).getTitle();
            VideoPreview preview = new VideoPreview(video.getVideoId(), previewResultPath, title);
            previews.add(preview);
            JSONUtils.saveToDisk(previews, previewDatabaseFile);
            System.out.println("Successful");
        }
    }

    private void saveDatabase() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(mapper.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(Visibility.ANY)
                .withGetterVisibility(Visibility.NONE)
                .withSetterVisibility(Visibility.NONE)
                .withCreatorVisibility(Visibility.NONE));
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.setDateFormat(new StdDateFormat().withColonInTimeZone(true));
        mapper.setTimeZone(TimeZone.getDefault());
        mapper.writerWithDefaultPrettyPrinter().writeValue(databaseFile, database);
    }
}
