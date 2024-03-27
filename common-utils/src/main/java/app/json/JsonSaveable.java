package app.json;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import app.utils.FileUtils;
import app.utils.JSONUtils;
import app.utils.PathUtils;

public abstract class JsonSaveable {
    private static final Logger LOGGER = LogManager.getLogger(JsonSaveable.class);

    @JsonIgnore
    private File saveFileIncomplete;
    @JsonIgnore
    private File saveFileComplete;
    @JsonIgnore
    private long minimalSaveIntervalInMS;

    @JsonIgnore
    private boolean isChanged;

    @JsonIgnore
    private DateFormat lastUpdatedDateFormatter;
    @JsonProperty
    private String lastUpdated;

    @JsonIgnore
    private long lastSaveTime;
    @JsonIgnore
    private SaveType lastSaveType;

    public JsonSaveable() {
        lastUpdatedDateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    }

    @JsonIgnore
    public String getLastUpdated() {
        return lastUpdated;
    }

    public void setSaveFileIncomplete(File saveFileIncomplete) {
        this.saveFileIncomplete = saveFileIncomplete;
    }

    public void setSaveFileComplete(File saveFileComplete) {
        this.saveFileComplete = saveFileComplete;
    }

    public void setMinimalSaveIntervalInMS(long minimalSaveIntervalInMS) {
        this.minimalSaveIntervalInMS = minimalSaveIntervalInMS;
    }

    public void setChanged() {
        this.lastUpdated = lastUpdatedDateFormatter.format(new Date());
        this.isChanged = true;
    }

    public void suggestSave() throws IOException {
        LOGGER.debug("SuggestSave: Checking if we need to save " + getClass().getName() + "...");
        if (!isChanged) {
            LOGGER.debug("There are no changes to save " + getClass().getName());
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastSaveTime > minimalSaveIntervalInMS) {
            LOGGER.debug("It is time to save " + getClass().getName());
            if (saveFileIncomplete == null) {
                saveComplete();
            } else {
                saveIncomplete();
            }
        } else {
            LOGGER.debug("It is NOT the time to save " + getClass().getName());
        }
    }

    public void saveIfNeededIncomplete() throws IOException {
        LOGGER.debug("SaveIfNeededIncomplete: Checking if we need to save " + getClass().getName() + "...");
        if (isChanged) {
            saveIncomplete();
        }
    }

    private void saveIncomplete() throws IOException {
        LOGGER.debug("SaveIncomplete: Saving " + getClass().getName() + " to file: " + saveFileIncomplete.getAbsolutePath() + "...");
        save(saveFileIncomplete);
        if (saveFileComplete != null && saveFileComplete.exists()) {
            LOGGER.debug("SaveIncomplete: Deleting Complete File: " + saveFileComplete.getAbsolutePath());
            PathUtils.deleteFile(saveFileComplete.toPath());
        }
        lastSaveType = SaveType.Incomplete;
    }

    public void saveIfNeededComplete() throws IOException {
        LOGGER.debug("SaveIfNeededComplete: Checking if we need to save " + getClass().getName() + "...");
        if (isChanged || !SaveType.Complete.equals(lastSaveType)) {
            saveComplete();
        }
    }

    private void saveComplete() throws IOException {
        LOGGER.debug("SaveComplete: Saving " + getClass().getName() + " to file: " + saveFileComplete.getAbsolutePath() + "...");
        save(saveFileComplete);
        if (saveFileIncomplete != null && saveFileIncomplete.exists()) {
            LOGGER.debug("SaveIncomplete: Deleting Incomplete File: " + saveFileIncomplete.getAbsolutePath());
            PathUtils.deleteFile(saveFileIncomplete.toPath());
            Path saveFileIncompleteBackup = generateBackupFilePath(saveFileIncomplete.toPath());
            if (PathUtils.exists(saveFileIncompleteBackup)) {
                LOGGER.debug("SaveIncomplete: Deleting Incomplete File: " + saveFileIncompleteBackup);
                PathUtils.deleteFile(saveFileIncompleteBackup);
            }
        }
        lastSaveType = SaveType.Complete;
    }

    private void save(File saveFile) throws IOException {
        try {
            LOGGER.info("Saving " + getClass().getName() + " to " + saveFile.toString());

            // Saving to a temporary file
            Path saveFilePath = saveFile.toPath().toAbsolutePath().normalize();
            Path saveFolderPath = saveFilePath.getParent();
            Path tempFilePath = Files.createTempFile(saveFolderPath, "saving-", ".tmp");
            LOGGER.debug("Saving " + getClass().getName() + " to temporary file " + tempFilePath.toString());
            JSONUtils.saveToDisk(this, tempFilePath.toFile());

            // If SaveFile already exists, Moving old file to bak-file
            if (Files.exists(saveFilePath)) {
                Path backupFilePath = generateBackupFilePath(saveFilePath);
                LOGGER.debug("Moving old version of " + getClass().getName()
                        + " from file " + saveFilePath.toString() + " to file " + backupFilePath.toString());
                PathUtils.moveFileWithReplacement(saveFilePath, backupFilePath);
            }

            // Moving temporary file to save file
            LOGGER.debug("Moving " + getClass().getName()
                    + " from temporary file " + tempFilePath.toString() + " to file " + saveFilePath.toString());
            PathUtils.moveFileWithReplacement(tempFilePath, saveFilePath);

            // Reset counters
            lastSaveTime = System.currentTimeMillis();
            isChanged = false;
            LOGGER.info(getClass().getName() + " has been saved successfully to " + saveFile.toString());
        } catch (IOException e) {
            LOGGER.error("Error due to save " + getClass().getName() + " to " + saveFile.toString(), e);
            throw e;
        }
    }

    private Path generateBackupFilePath(Path saveFilePath) {
        String saveFileString = saveFilePath.toString();
        String backupFileString = FileUtils.getFileName(saveFileString) + ".backup." + FileUtils.getFileExtension(saveFileString);
        Path backupFilePath = Paths.get(backupFileString);
        return backupFilePath;
    }

    private static enum SaveType {
        Complete, Incomplete
    }
}
