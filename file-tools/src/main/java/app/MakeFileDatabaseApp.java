package app;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import app.core.FileDatabaseUpdater;
import app.core.dto.FileDatabase;
import app.core.dto.FileFolder;

public class MakeFileDatabaseApp {
    private static final String FILE_DATABASE_FILENAME_COMPLETE = "file-database.json";
    private static final String FILE_DATABASE_FILENAME_INCOMPLETE = "file-database-INCOMPLETE.json";

    private static final Logger LOGGER = LogManager.getLogger(MakeFileDatabaseApp.class);

    private static FileDatabase fileDatabase;

    public static void main(String[] args) {
        LOGGER.info("Make FileDatabase App started");

        try {
            LOGGER.info("Updating File Database...");
            fileDatabase = new FileDatabase();
            fileDatabase.setSaveFileComplete(new File(FILE_DATABASE_FILENAME_COMPLETE));
            fileDatabase.setSaveFileIncomplete(new File(FILE_DATABASE_FILENAME_INCOMPLETE));
            FileDatabaseUpdater fileDatabaseUpdater = new FileDatabaseUpdater();
            fileDatabaseUpdater.setFileDatabase(fileDatabase);

            List<FileFolder> fileFolders = new ArrayList<>();
            for (String arg : args) {
                fileFolders.add(new FileFolder(arg, true));
            }
            fileDatabaseUpdater.setFileSources(fileFolders);

            fileDatabaseUpdater.updateFileDatabase();

            fileDatabase.saveIfNeededComplete();

            LOGGER.info("Application finished successfully");
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }
}
