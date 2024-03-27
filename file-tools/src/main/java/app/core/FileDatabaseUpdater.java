package app.core;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import app.core.dto.AbsoluteFileMetadata;
import app.core.dto.FileDatabase;
import app.core.dto.FileFolder;
import app.files.PathIterator;

// TODO Merge this class into FileDatabase class
//      (look at the methods, possibly this mechanism is already implemented there)
public class FileDatabaseUpdater {
    private static final Logger LOGGER = LogManager.getLogger(FileDatabaseUpdater.class);

    private FileDatabase fileDatabase;

    private List<FileFolder> fileSources;

    public void setFileDatabase(FileDatabase fileDatabase) {
        this.fileDatabase = fileDatabase;
    }

    public void setFileSources(List<FileFolder> fileSources) {
        this.fileSources = fileSources;
    }

    public void updateFileDatabase() throws IOException {
        scanFileFolders();
        fileDatabase.removeEntriesForFilesDeletedFomDisk();
        fileDatabase.suggestSave();
    }

    private void scanFileFolders() throws IOException {
        LOGGER.info("Starting to Scan File Folders...");

        // Marking existing files in the FileDatabase as they potentially does not exist on disk anymore
        // Later all existing files will be whitelisted during Scan phase
        for (AbsoluteFileMetadata fileMetadata : fileDatabase.getFiles().values()) {
            fileMetadata.setExistsOnDiskNow(false);
        }

        for (FileFolder fileSource : fileSources) {
            String message = "Scanning FileFolder " + fileSource.getPath() + (fileSource.isReadOnly() ? " (ReadOnly)" : "") + "...";
            LOGGER.info(message);

            Path folderPath = Paths.get(fileSource.getPath());
            PathIterator iterator = new PathIterator(folderPath, true);
            while (iterator.hasNext()) {
                Path currentFile = iterator.next();
                LOGGER.debug("Found file: " + currentFile.toString());

                fileDatabase.updateEntryFromDisk(currentFile, fileSource.isReadOnly());
            }
        }

        LOGGER.info("File scanning is done");
    }
}
