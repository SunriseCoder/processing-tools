package app.core.dto;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.type.TypeReference;

import app.utils.FileUtils;
import app.utils.JSONUtils;

public class Configuration {
    private static final String CONFIGURATION_FILENAME = "configuration.json";
    private static final Logger LOGGER = LogManager.getLogger(Configuration.class);

    private List<String> checksumAlgorithms;
    private long fileCopyChunkSize;

    private List<FileFolder> fileSources;

    private List<FolderSortRule> folderSortRules;

    private long fileDatabaseMinimalSaveIntervalInMS;
    private long snapshotMinimalSaveIntervalInMS;

    private long progressPrinterDotDataSize;
    private int progressPrinterDotsPerLine;

    public List<String> getChecksumAlgorithms() {
        return checksumAlgorithms;
    }

    public long getFileCopyChunkSize() {
        return fileCopyChunkSize;
    }

    public List<FileFolder> getFileSources() {
        return fileSources;
    }

    public List<FolderSortRule> getFolderSortRules() {
        return folderSortRules;
    }

    public long getFileDatabaseMinimalSaveIntervalInMS() {
        return fileDatabaseMinimalSaveIntervalInMS;
    }

    public long getSnapshotMinimalSaveIntervalInMS() {
        return snapshotMinimalSaveIntervalInMS;
    }

    public long getProgressPrinterDotDataSize() {
        return progressPrinterDotDataSize;
    }

    public int getProgressPrinterDotsPerLine() {
        return progressPrinterDotsPerLine;
    }

    private void postConstruct() {
        // Fixing Drive-Letters for Windows because Path.toAbsolutePath() adds capital Drive-Letter.
        for (FileFolder fileSource : fileSources) {
            String fixedPath = FileUtils.driveLetterToUpperCaseIfNeeded(fileSource.getPath());
            fileSource.setPath(fixedPath);
        }
    }

    public static Configuration load() throws IOException {
        LOGGER.info("Loading Configuration...");
        String applicationHomePath = System.getProperty("app.home");
        File configFile = new File(applicationHomePath, CONFIGURATION_FILENAME);
        if (configFile.exists()) {
            TypeReference<Configuration> typeReference = new TypeReference<Configuration>() {};
            Configuration configuration = JSONUtils.loadFromDisk(configFile, typeReference);
            configuration.postConstruct();
            return configuration;
        } else {
            throw new FileNotFoundException("Configuration file not found: " + configFile.getAbsolutePath());
        }
    }
}
