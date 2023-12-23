package app.core.dto;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;

import utils.JSONUtils;

public class FolderSnapshot {
    private static final Logger LOGGER = LogManager.getLogger(FolderSnapshot.class);

    // TODO Move to Configuration, retrieve by passing Configuration to the class constructor
    private static final int MINIMAL_SAVE_INTERVAL = 5 * 60 * 1000;

    private String name;
    private Map<String, RelativeFileMetadata> files;

    @JsonIgnore
    private File saveFile;
    @JsonIgnore
    private long lastSaveTime;

    public FolderSnapshot() {
        files = new HashMap<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, RelativeFileMetadata> getFilesMap() {
        return files;
    }

    public RelativeFileMetadata getFileMetadata(String relativePath) {
        return files.get(relativePath);
    }

    public void addFileMetadata(RelativeFileMetadata fileMetadata) {
        files.put(fileMetadata.getRelativePath(), fileMetadata);
    }

    public void setSaveFile(File saveFile) {
        this.saveFile = saveFile;
    }

    public void saveIfNeeded() throws IOException {
        long now = System.currentTimeMillis();
        if (now - lastSaveTime > MINIMAL_SAVE_INTERVAL) {
            save();
            lastSaveTime = System.currentTimeMillis();
        }
    }

    public void save() throws IOException {
        try {
            LOGGER.debug("Saving Snapshot " + getName() + " to " + saveFile.toString());
            JSONUtils.saveToDisk(this, saveFile);
            LOGGER.debug("Snapshot " + getName() + " has been saved successfully to " + saveFile.toString());
        } catch (IOException e) {
            LOGGER.error("Error due to save snapshot " + getName() + " to " + saveFile.toString(), e);
            throw e;
        }
    }
}
