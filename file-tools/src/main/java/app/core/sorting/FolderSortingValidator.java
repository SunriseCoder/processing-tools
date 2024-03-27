package app.core.sorting;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import app.core.dto.FolderSortRule;
import app.structures.Pair;

public class FolderSortingValidator {
    private static final Logger LOGGER = LogManager.getLogger(FolderSortingValidator.class);

    private Path targetFolder;
    private List<FolderSortRule> folderSorterRules;

    private List<Path> invalidFolders;
    private List<Pair<Path, FolderSortRule>> validFolders;

    public FolderSortingValidator(Path targetFolder, List<FolderSortRule> folderSorterRules) {
        this.targetFolder = targetFolder;
        this.folderSorterRules = folderSorterRules;

        invalidFolders = new ArrayList<>();
        validFolders = new ArrayList<>();
    }

    public void validate() throws IOException {
        LOGGER.info("Validating folder: " + targetFolder);

        Map<String, FolderSortRule> sorterRulesMap = folderSorterRules.stream()
                .collect(Collectors.toMap(r -> r.getSource(), r -> r));

        invalidFolders = new ArrayList<>();

        DirectoryStream<Path> directoryStream = Files.newDirectoryStream(targetFolder);
        for (Path path : directoryStream) {
            if (!Files.isDirectory(path)) {
                continue;
            }

            String subFolderName = path.getFileName().toString();
            if (!sorterRulesMap.containsKey(subFolderName)) {
                invalidFolders.add(path);
            } else {
                FolderSortRule rule = sorterRulesMap.get(subFolderName);
                validFolders.add(new Pair<>(path, rule));
            }
        }
    }

    public List<Path> getInvalidFolders() {
        return invalidFolders;
    }

    public List<Pair<Path, FolderSortRule>> getValidFolders() {
        return validFolders;
    }
}
