package app;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import app.core.FileDuplicationFinder;
import app.core.dto.Configuration;
import app.core.dto.FileDatabase;
import app.utils.PathUtils;

public class SearchFileDuplicationsApp {
    private static final Logger LOGGER = LogManager.getLogger(SearchFileDuplicationsApp.class);
    private static List<Path> baseFolders;
    private static List<Path> externalFolders;

    public static void main(String[] args) throws IOException {
        LOGGER.info("Search File Duplication started");

        try {
            processInputArguments(args);

            Configuration configuration = Configuration.load();
            FileDatabase fileDatabase = FileDatabase.load(configuration);

            FileDuplicationFinder finder = new FileDuplicationFinder();
            finder.setConfiguration(configuration);
            finder.setFileDatabase(fileDatabase);
            finder.setBaseFolders(baseFolders);
            finder.setExternalFolders(externalFolders);

            finder.findDuplications();

            fileDatabase.saveIfNeededComplete();

            LOGGER.info("Application finished successfully");
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            System.exit(-1);
        }
    }

    private static void processInputArguments(String[] args) throws IOException {
        LOGGER.info("Checking Input Arguments...");
        if (args.length < 1) {
            LOGGER.error("Invalid input parameters: " + Arrays.toString(args));
            printUsage();
            System.exit(-1);
        }

        baseFolders = PathUtils.splitPathsFromString(args[0]);
        if (args.length >= 2) {
            externalFolders = PathUtils.splitPathsFromString(args[1]);
        }
    }

    private static void printUsage() {
        System.out.println("\nUsage:");
        System.out.println(SearchFileDuplicationsApp.class.getName() + " <base-folders> [external-folders]");
        System.out.println("  <base-folders>     - (semi)colon-separated-list of the folders, which are the body of the archive/cloud/etc");
        System.out.println("  [external-folders] - (semi)colon-separated-list of the folders, which need to be checked against the base for the sake of cleanup duplicates");
        System.out.println("(semi)colon-separated-list of the folders is for example: \"D:\\Data;E:\\Data\" or \"/mnt/data1:/mnt/data2\"");
        System.out.println("Separator depends on the Operating System, i.e. Semicolon (;) for Windows, Colon (:) for Linux/Unix/Mac");
    }
}
