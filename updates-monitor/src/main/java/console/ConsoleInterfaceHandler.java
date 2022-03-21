package console;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.TimeZone;

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

    private YoutubeVideoHandler youtubeVideoHandler = new YoutubeVideoHandler();

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
            System.out.print("Select action: [1] Status, [2] Add resource, "
                    + "[5] Check updates, [7] Scan videos, [8] Export download links, [9] Download files, [0] Exit ");
            input = scanner.next();
            switch (input) {
            case "1":
                printStatus();
                break;
            case "2":
                addResource();
                break;
            case "5":
                checkUpdates();
                break;
            case "7":
                scanVideoDetails();
                break;
            case "8":
                exportDownloadLinks();
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
        // Youtube Videos by Channels
        System.out.println("Youtube channels:");
        Collection<YoutubeChannel> youtubeChannels = database.getYoutubeChannels().values();
        for (YoutubeChannel channel : youtubeChannels) {
            System.out.println("\t" + channel);
            System.out.println("\t\t" + channel.getStatusString());
        }
        System.out.println(youtubeChannels.size() + " channel(s) total");

        // Youtube Videos Total
        System.out.println("Youtube videos: "
                + database.getYoutubeNotScannedVideos().size() + " not scanned, "
                + database.getYoutubeVideosByVideoFormatType(YoutubeVideoFormatTypes.OrdinaryFile).size() + " ordinary, "
                + database.getYoutubeVideosByVideoFormatType(YoutubeVideoFormatTypes.OTF_Stream).size() + " otf, "
                + database.getYoutubeVideosByVideoFormatType(YoutubeVideoFormatTypes.Encrypted).size() + " encrypted, "
                + database.getYoutubeVideosByVideoFormatType(YoutubeVideoFormatTypes.NotAdaptive).size() + " not adaptive, "
                + database.getYoutubeNotDownloadedVideos().size() + " not downloaded, "
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
        Map<String, YoutubeVideo> notScannedVideos = database.getYoutubeNotScannedVideos();
        System.out.println("Scanning video details, to go: " + notScannedVideos.size() + " video(s)...");

        List<YoutubeVideo> videos = new ArrayList<>(notScannedVideos.values());
        for (int i = 0; i < videos.size(); i++) {
            YoutubeVideo video = videos.get(i);
            System.out.print("\tScanning video: " + i + " of " + videos.size() + " : " + video.getVideoId() + "... ");
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

    private void exportDownloadLinks() {
        // TODO Auto-generated method stub
    }

    private void downloadAllFiles() throws IOException {
        System.out.print("Downloading Youtube videos... ");

        Map<String, YoutubeVideo> youtubeVideos = database.getYoutubeNotDownloadedVideos();
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
            }

            saveDatabase();
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
