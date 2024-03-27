package app;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import app.core.dto.Configuration;
import app.core.sorting.FolderSorter;
import app.utils.PathUtils;

public class FolderSortingApp {
    private static final Logger LOGGER = LogManager.getLogger(FolderSortingApp.class);

    private static Configuration configuration;
    private static Path targetFolder;

    public static void main(String[] args) throws IOException {
        processInputArguments(args);

        configuration = Configuration.load();

        sortFolder();

        LOGGER.info("Application finished successfully");
    }

    private static void sortFolder() throws IOException {
        FolderSorter folderSorter = new FolderSorter();
        folderSorter.setConfiguration(configuration);
        folderSorter.setTargetFolder(targetFolder);
        folderSorter.sort();
    }

    private static void processInputArguments(String[] args) throws IOException {
        LOGGER.info("Checking Input Arguments...");
        targetFolder = PathUtils.getAbsolutePathWithDriveLetterUpperCase(args.length < 1 ? "." : args[0]);
    }
}
