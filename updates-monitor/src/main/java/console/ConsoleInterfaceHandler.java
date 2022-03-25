package console;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
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

import core.Database;
import core.dto.youtube.YoutubeChannel;
import core.dto.youtube.YoutubeResult;
import core.dto.youtube.YoutubeVideo;
import core.dto.youtube.YoutubeVideoFormatTypes;
import core.youtube.YoutubeChannelHandler;
import core.youtube.YoutubeVideoHandler;
import function.LambdaCommand;
import utils.FileUtils;
import utils.JSONUtils;
import utils.ThreadUtils;

public class ConsoleInterfaceHandler {
    private static final String DATABASE_FOLDER = "database";
    private static final String DOWNLOAD_FOLDER = "download";
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
                    + "[5] Check updates, [7] Scan videos, [8] Re-scan videos, [9] Download files, [0] Exit\n"
                    + "[11] Fix Downloaded ");
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
                    if (youtubeChannelFetchResult.channelNotFound) {
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
            int attempts = 10;
            do {
                try {
                    System.out.print("\tUpdating Youtube Channel " + channel + "... ");
                    YoutubeChannelHandler.Result updateResult = YoutubeChannelHandler.checkUpdates(channel);
                    if (updateResult.channelNotFound) {
                        System.out.println("Channel not found on Youtube!!!");
                    } else {
                        System.out.println("Successfully");
                        if (!updateResult.newTitle.equals(updateResult.oldTitle)) {
                            channel.setTitle(updateResult.newTitle);

                            // Renaming Channel Download Folder
                            String channelNewFoldername = channel.getChannelId() + "_" + FileUtils.getSafeFilename(updateResult.newTitle);
                            String channelOldFoldername = channel.getFoldername();
                            channel.setFoldername(channelNewFoldername);
                            FileUtils.renameOrCreateFileOrFolder(new File(DOWNLOAD_FOLDER, channelOldFoldername), new File(DOWNLOAD_FOLDER, channelNewFoldername));

                            System.out.println("\t\tYoutube Channel " + channel.getChannelId() + " Title was changed from \""
                                            + updateResult.oldTitle + "\" to \"" + updateResult.newTitle + "\"");
                        }

                        // Update details about new videos
                        if (updateResult.newVideos.size() > 0) {
                            for (YoutubeVideo youtubeVideo : updateResult.newVideos) {
                                database.addYoutubeVideo(youtubeVideo);
                            }
                            System.out.println("\t\tFound " + updateResult.newVideos.size() + " new video(s)");
                        }

                        // TODO Add update details about playlists
                        database.linkEntities();
                        saveDatabase();
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
        }
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
                .filter(e -> e.isDownloaded()).collect(Collectors.toList()));

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
                System.out.println(" Ok");
            }
        }

        if (fixedCounter > 0) {
            saveDatabase();
        }
        System.out.println("Fixing downloaded videos is done, fixed videos: " + fixedCounter);
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
