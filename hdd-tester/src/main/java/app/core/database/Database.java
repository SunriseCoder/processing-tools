package app.core.database;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;

import app.HddTesterApp;
import app.config.Configuration;
import app.config.Configuration.Keys;
import app.core.dto.fs.FileSystemElement;
import app.core.dto.fs.FileSystemFile;
import app.core.dto.fs.FileSystemFolder;
import app.core.file.FolderIterator;
import utils.ConsoleUtils;
import utils.ConsoleUtils.Option;
import utils.JSONUtils;

public class Database {
    private static final Logger LOGGER = LogManager.getLogger(HddTesterApp.class);

    private static final String FILENAME_CHARS = "23456789ABCEFGHJKLMNPRSTUVWXYZabcdefghkmnpqrstuvwxyz";
    private static final Random RND = new Random();

    @JsonIgnore
    private Map<String, FileSystemFile> files;
    private FileSystemFolder logicalRootFolder;
    private String lastUpdated;

    @JsonIgnore
    private DateFormat lastUpdatedDateFormatter;

    @JsonIgnore
    private long lastSaveTime;
    @JsonIgnore
    private long dbSaveIntervalInSeconds;

    public Database() {
        files = new HashMap<>();
        lastUpdatedDateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        dbSaveIntervalInSeconds = Long.parseLong(Configuration.getValue(Keys.DbSaveIntervalInSeconds));
        logicalRootFolder = new FileSystemFolder("/");
    }

    public Map<String, FileSystemFile> getFiles() {
        return files;
    }

    public String getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated() {
        this.lastUpdated = lastUpdatedDateFormatter.format(new Date());
    }

    public void addFile(FileSystemFile file) {
        String path = file.getPath();
        files.put(path, file);
        setLastUpdated();
    }

    public void linkFiles() {
        linkFilesRecursively(logicalRootFolder);
    }

    private void linkFilesRecursively(FileSystemFolder folder) {
        for (FileSystemFolder child : folder.getFolderCollection()) {
            child.setParent(folder);
            linkFilesRecursively(child);
        }

        for (FileSystemFile file : folder.getFileCollection()) {
            file.setParent(folder);
            files.put(file.getPath(), file);
        }
    }

    public void checkRealFilesOnHdd(File physicalRootFolder) {
        int errors = 0;
        for (Entry<String, FileSystemFile> entry : files.entrySet()) {
            String path = entry.getKey();
            FileSystemFile fileMetadata = entry.getValue();
            File file = new File(physicalRootFolder.getAbsoluteFile() + path);

            if (!file.exists()) {
                LOGGER.error("Error due to check physical files, file not found: " + file.getAbsolutePath());
                errors++;
            } else if (!file.isFile()) {
                LOGGER.error("Error due to check physical files, file is not a file: " + file.getAbsolutePath());
                errors++;
            } else if (file.length() != fileMetadata.getSize()) {
                LOGGER.error("Error due to check physical files, file size is different. "
                        + "Expected: " + fileMetadata.getSize() + ", Actual: " + file.length());
                errors++;
            }
        }

        if (errors > 0) {
            Option optionContinue = new ConsoleUtils.Option("Continue");
            Option optionStop = new ConsoleUtils.Option("Stop");
            Option result = ConsoleUtils.chooseOneOption("Some errors occured due to checking files from previous run. "
                    + "What would You like to do?", optionContinue, optionStop);

            if (result == optionContinue) {
                // Nothing to do here, just continuing the program
                LOGGER.info("Used decided to continue on his own risk...");
            } else if (result == optionStop) {
                LOGGER.info("Used decided to stop, exiting...");
                System.exit(0);
            }
        }
    }

    public FileSystemFile createNewFile(long fileSize) {
        FileSystemFolder folder = findSuitableFolderForANewFile();
        String fileName = generateNewFilename(folder);

        FileSystemFile file = new FileSystemFile();
        file.setParent(folder);
        file.setName(fileName);
        file.setSize(fileSize);

        folder.addFile(file);
        addFile(file);

        return file;
    }

    private String generateNewFilename(FileSystemFolder parentFolder) {
        String filename;
        do {
            filename = generateFilename() + ".dat";
        } while (!parentFolder.isNameFree(filename));
        return filename;
    }

    private String generateFilename() {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < 8; i++) {
            int nextCharIndex = RND.nextInt(FILENAME_CHARS.length());
            char nextChar = FILENAME_CHARS.charAt(nextCharIndex);
            sb.append(nextChar);
        }

        return sb.toString();
    }

    private FileSystemFolder findSuitableFolderForANewFile() {
        // Step 1 - Looking for a folder where we have amountOfFiles <= amountOfFolders
        // If found such folder - returning it, otherwise go to Step 2
        FolderIterator iterator = new FolderIterator(logicalRootFolder);
        FileSystemFolder folderWithMinFolderAmount = logicalRootFolder;
        int maxFolderDepth = Integer.parseInt(Configuration.getValue(Keys.MaxFolderDepth));
        while (iterator.hasNext()) {
            // Comparing amount of Files and Folders in the Folder
            FileSystemFolder folder = iterator.next();
            int amountOfFiles = folder.getFileCollection().size();
            int amountOfFolders = folder.getFolderCollection().size();
            if (amountOfFiles <= amountOfFolders) {
                return folder;
            }

            // Looking for a folder with minimal folders amount for Step 2 in case Step 1 fails
            int minAmountOfFolders = folderWithMinFolderAmount.getFolderCollection().size();
            int folderDepth = getFolderDepth(folder);
            if (amountOfFolders < minAmountOfFolders && folderDepth < maxFolderDepth) {
                folderWithMinFolderAmount = folder;
            }
        }

        // Step 2 - Looking for a folder with minimal amountOfFolders,
        // creating a new folder there and returning it
        // Looking for such a folder in a previous step in order to not to loop second time
        String newFolderName = generateNewFoldername(folderWithMinFolderAmount);
        FileSystemFolder newFolder = createNewFolder(folderWithMinFolderAmount, newFolderName);

        return newFolder;
    }

    private int getFolderDepth(FileSystemFolder folder) {
        int depth = 1;
        FileSystemElement parent, current = folder;
        while ((parent = current.getParent()) != null) {
            current = parent;
            depth++;
        }
        return depth;
    }

    private String generateNewFoldername(FileSystemFolder parentFolder) {
        String foldername;
        do {
            foldername = generateFilename();
        } while (!parentFolder.isNameFree(foldername));
        return foldername;
    }

    private FileSystemFolder createNewFolder(FileSystemFolder parentFolder, String newFolderName) {
        FileSystemFolder newFolder = new FileSystemFolder();
        newFolder.setName(newFolderName);
        newFolder.setParent(parentFolder);
        parentFolder.addFolder(newFolder);
        return newFolder;
    }

    public void saveIfNeeded() throws IOException {
        long now = System.currentTimeMillis();
        long sinceLastSaveInSeconds = (now - lastSaveTime) / 1000;
        if (sinceLastSaveInSeconds < dbSaveIntervalInSeconds) {
            return;
        }

        JSONUtils.saveToDisk(this, "database.json");
        lastSaveTime = System.currentTimeMillis();
    }
}
