package console;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

import com.fasterxml.jackson.core.type.TypeReference;

import core.Database;
import core.dto.YoutubeChannel;
import core.dto.YoutubeVideo;
import core.youtube.YoutubeChannelHandler;
import core.youtube.YoutubeResult;
import core.youtube.YoutubeVideoHandler;
import utils.FileUtils;
import utils.JSONUtils;
import utils.ThreadUtils;

public class ConsoleInterfaceHandler {
    private static final String DATABASE_FOLDER = "database";
    private static final String DOWNLOAD_FOLDER = "download";
    private static final String TEMPORARY_FOLDER = "tmp";

    private Scanner scanner;
    private File databaseFile;
    private Database database;

    public ConsoleInterfaceHandler() {
        scanner = new Scanner(System.in);
    }

    public void start() throws IOException {
        System.out.println("Application is starting...");

        FileUtils.createFolderIfNotExists(DATABASE_FOLDER);
        FileUtils.createFolderIfNotExists(DOWNLOAD_FOLDER);
        FileUtils.createFolderIfNotExists(TEMPORARY_FOLDER);

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

    private void mainMenu() throws IOException {
        String input;
        while (true) {
            System.out.print("Select action: [1] Status, [2] Add resource, [8] Check updates, [9] Download files, [0] Exit ");
            input = scanner.next();
            switch (input) {
            case "1":
                printStatus();
                break;
            case "2":
                addResource();
                break;
            case "8":
                checkUpdates();
                break;
            case "9":
                downloadAllFiles();
                break;
            case "0":
                System.out.println("Exiting...");
                scanner.close();
                System.exit(0);
            default:
                System.out.println("Unknown command, please try again");
            }
        }
    }

    private void printStatus() {
        System.out.println("Youtube channels:");
        Collection<YoutubeChannel> youtubeChannels = database.getYoutubeChannels().values();
        for (YoutubeChannel channel : youtubeChannels) {
            System.out.println("\t" + channel);
            System.out.println("\t\t" + channel.getStatusString());
        }
        System.out.println(youtubeChannels.size() + " channel(s) total");
        System.out.println("Youtube videos: " + database.getYoutubeNewVideos().size() + " new, "
                + database.getYoutubeDownloadedVideos().size() + " done, " + database.getYoutubeVideos().size() + " total");
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

    private void saveDatabase() throws IOException {
        JSONUtils.saveToDisk(database, databaseFile);
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

    private void downloadAllFiles() {
        System.out.print("Downloading Youtube videos... ");

        YoutubeVideoHandler youtubeVideoHandler = new YoutubeVideoHandler();

        Map<String, YoutubeVideo> youtubeVideos = database.getYoutubeNewVideos();
        System.out.println(youtubeVideos.size() + " video(s) to go...");
        Iterator<Entry<String, YoutubeVideo>> iterator = youtubeVideos.entrySet().iterator();
        while (iterator.hasNext()) {
            YoutubeVideo youtubeVideo = iterator.next().getValue();
            YoutubeChannel youtubeChannel = database.getYoutubeChannel(youtubeVideo.getChannelId());
            YoutubeResult result = new YoutubeResult();
            while (!result.completed && !result.notFound) {
                System.out.print("Downloading video: " + youtubeChannel + " - " + youtubeVideo + "... ");
                try {
                    String downloadPath = DOWNLOAD_FOLDER + "/" + youtubeChannel.getFoldername();
                    FileUtils.createFolderIfNotExists(downloadPath);
                    result = youtubeVideoHandler.downloadVideo(youtubeVideo, downloadPath, TEMPORARY_FOLDER);
                    if (result.unsupported) {
                        System.out.println("Unsupported video format: " + youtubeVideo.getVideoId() + ", skipping...");
                        break;
                    }
                    if (result.completed) {
                        youtubeVideo.setDownloaded(true);
                        database.getYoutubeDownloadedVideos().put(youtubeVideo.getVideoId(), youtubeVideo);
                        saveDatabase();
                        iterator.remove();
                    }
                } catch (Exception e) {
                    System.out.println("Error: " + e.getMessage());
                    e.printStackTrace();
                    ThreadUtils.sleep(5000);
                }
                System.out.println();
            }
        }
    }
}
