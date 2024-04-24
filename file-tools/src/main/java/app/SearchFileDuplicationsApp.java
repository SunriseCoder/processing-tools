package app;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import app.core.FileDuplicationFinder;

public class SearchFileDuplicationsApp {
    private static final Logger LOGGER = LogManager.getLogger(SearchFileDuplicationsApp.class);

    public static void main(String[] args) throws IOException {
        LOGGER.info("Search File Duplication started");

        try {
            FileDuplicationFinder finder = new FileDuplicationFinder();

            for (String arg : args) {
                finder.scanFolder(arg);
            }

            finder.reportResults();

            LOGGER.info("Application finished successfully");
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            System.exit(-1);
        }
    }
}
