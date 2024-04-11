package app.core.sorting;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import app.core.dto.Configuration;
import app.core.dto.FolderSortRule;
import app.core.file.operations.FileOperation;
import app.core.file.operations.MoveFolderOperation;
import app.progress.DottedProgressPrinter;
import app.structures.Pair;
import app.utils.ConsoleUtils;
import app.utils.DateTimeUtils;
import app.utils.FormattingUtils;

public class FolderSorter {
    private static final Logger LOGGER = LogManager.getLogger(FolderSorter.class);

    private Configuration configuration;
    private Path targetFolder;

    private List<Pair<Path, FolderSortRule>> validFolders;
    private List<Path> invalidFolders;
    private List<FileOperation> folderOperationsList;

    private long totalCopySize;

    private List<String> folderOperationsErrors;

    public FolderSorter() {
        folderOperationsList = new ArrayList<>();
        folderOperationsErrors = new ArrayList<>();
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    public void setTargetFolder(Path targetFolder) {
        this.targetFolder = targetFolder;
    }

    public void sort() throws IOException {
        validateSortFolder();

        generateFolderOperationsList();

        if (askUserConfirmation()) {
            performOperations();
            reportResults();
        }
    }

    private void validateSortFolder() throws IOException {
        FolderSortingValidator validator = new FolderSortingValidator(targetFolder, configuration.getFolderSortRules());
        validator.validate();
        validFolders = validator.getValidFolders();
        invalidFolders = validator.getInvalidFolders();

        if (!invalidFolders.isEmpty()) {
            String errorsMessage = invalidFolders.stream()
                    .map(p -> p.toString())
                    .collect(Collectors.joining("\n"));
            LOGGER.error("No Sorting Rule found for the following folder(s):\n" + errorsMessage);
        }
    }

    private void generateFolderOperationsList() throws IOException {
        folderOperationsList.clear();
        for (Pair<Path, FolderSortRule> pair : validFolders) {
            Path sourcePath = pair.getKey();

            FolderSortRule rule = pair.getValue();
            Path destinationPath = Paths.get(rule.getDestination());
            if (rule.isIncludeParentFolder()) {
                String targetFolderName = targetFolder.getFileName().toString();
                if (rule.isAddCurrentDateToDestinationFolderName()) {
                    targetFolderName += " [md-" + DateTimeUtils.getCurrentDateFormatted() + "]";
                }
                destinationPath = destinationPath.resolve(targetFolderName);
            }

            folderOperationsList.add(new MoveFolderOperation(sourcePath, destinationPath));
        }
    }

    private boolean askUserConfirmation() {
        StringBuilder sb = new StringBuilder();

        sb.append("\n\n\tThe following Operations will be performed:\n");
        folderOperationsList.forEach(o -> sb.append(o).append("\n"));

        if (!invalidFolders.isEmpty()) {
            sb.append("\n\t!!! ATTENTION !!! No Sorting Rule found For the following Folder(s)...\n");
            sb.append("\tThis mean that the following Folders will NOT be Sorted:");
            invalidFolders.forEach(f -> sb.append(f).append("\n"));
        }

        totalCopySize = folderOperationsList.stream()
                .mapToLong(o -> o.getCopyDataSize())
                .sum();
        sb.append("Total Copy Size: ").append(FormattingUtils.humanReadableSizeBi(totalCopySize)).append("b\n");

        sb.append("Do you confirm the operations (yes/no)? ");
        LOGGER.info("User Confirmation text:\n" + sb.toString());

        boolean userConfirmation = ConsoleUtils.userConfirm(null);
        if (userConfirmation) {
            LOGGER.info("User Confirmed the Real File Operations...");
        } else {
            LOGGER.info("User did NOT Confirm the Real File Operations...");
        }

        return userConfirmation;
    }

    private void performOperations() throws IOException {
        LOGGER.info("Performing Real File Operations - Copying and Moving the Files...");

        DottedProgressPrinter progressPrinter = new DottedProgressPrinter();
        progressPrinter.setDotDataSize(configuration.getProgressPrinterDotDataSize());
        progressPrinter.setDotsPerLine(configuration.getProgressPrinterDotsPerLine());
        progressPrinter.reset(totalCopySize);

        long fileCopyChunkSize = configuration.getFileCopyChunkSize();

        for (FileOperation folderOperation : folderOperationsList) {
            folderOperation.setCopyChunkSize(fileCopyChunkSize);
            folderOperation.setProgressPrinter(progressPrinter);

            LOGGER.info("Performing Real Folder Operation:\n" + folderOperation);
            try {
                folderOperation.perform();
            } catch (IOException e) {
                folderOperationsErrors.add("Error:\n" + e.getMessage() + " on:\n" + folderOperation);
                LOGGER.error("Error due to Folder Operation: " + e.getMessage(), e);
                throw e;
            }
        }

        LOGGER.info("Real File Operations has finished");
    }

    private void reportResults() {
        if (folderOperationsErrors.isEmpty()) {
            LOGGER.info("There was NO Errors during File Sorting process");
        } else {
            String errors = String.join("\n", folderOperationsErrors);
            LOGGER.error("There were following Errors during File Sorting process:\n" + errors);
        }
    }
}
