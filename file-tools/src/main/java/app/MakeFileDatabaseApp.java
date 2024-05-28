package app;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import app.core.FileDatabaseUpdater;
import app.core.dto.FileDatabase;
import app.core.dto.AssemblerFileSource;

public class MakeFileDatabaseApp {
    private static final Logger LOGGER = LogManager.getLogger(MakeFileDatabaseApp.class);

    private static FileDatabase fileDatabase;

    public static void main(String[] args) {
        LOGGER.info("Make FileDatabase App started");

        try {
            LOGGER.info("Updating File Database...");

            fileDatabase = FileDatabase.load();

            FileDatabaseUpdater fileDatabaseUpdater = new FileDatabaseUpdater();
            fileDatabaseUpdater.setFileDatabase(fileDatabase);

            List<AssemblerFileSource> fileFolders = new ArrayList<>();
            for (String arg : args) {
                fileFolders.add(new AssemblerFileSource(arg, true));
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
