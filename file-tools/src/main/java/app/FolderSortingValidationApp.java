package app;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import app.core.dto.Configuration;
import app.core.dto.FolderSortRule;
import app.core.sorting.FolderSortingValidator;
import app.utils.PathUtils;

public class FolderSortingValidationApp {
    private static final Logger LOGGER = LogManager.getLogger(FolderSortingValidationApp.class);

    private static Configuration configuration;
    private static Path targetFolder;

    public static void main(String[] args) throws IOException {
        processInputArguments(args);

        configuration = Configuration.load();
        printFolderSorterRules();

        validateTargetFolder();

        LOGGER.info("Application finished successfully");
    }

    private static void processInputArguments(String[] args) throws IOException {
        LOGGER.info("Checking Input Arguments...");
        targetFolder = PathUtils.getAbsolutePathWithDriveLetterUpperCase(args.length < 1 ? "." : args[0]);
        LOGGER.info("Target folder is: " + targetFolder.toString());
    }

    private static void printFolderSorterRules() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n\tSorter Rules:\n");
        for (FolderSortRule rule : configuration.getFolderSortRules()) {
            sb.append(rule.getSource() + "\t- " + rule.getComment() + "\n");
        }
        LOGGER.info(sb.toString());
    }

    private static void validateTargetFolder() throws IOException {
        FolderSortingValidator validator = new FolderSortingValidator(targetFolder, configuration.getFolderSortRules());
        validator.validate();
        List<Path> invalidFolders = validator.getInvalidFolders();
        if (!invalidFolders.isEmpty()) {
            String errorsMessage = invalidFolders.stream()
                    .map(p -> p.getFileName().toString())
                    .collect(Collectors.joining("\n"));
            LOGGER.error("No Sorting Rule found for the following folder(s):\n" + errorsMessage);
        } else {
            LOGGER.info("For All the Folder(s) Sorting Rules found Successfully");
        }
    }
}
