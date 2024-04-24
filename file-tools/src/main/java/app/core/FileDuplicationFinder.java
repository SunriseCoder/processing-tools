package app.core;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import app.core.dto.FileMetadata;
import app.files.PathIterator;
import app.utils.FormattingUtils;

public class FileDuplicationFinder {
    private static final Logger LOGGER = LogManager.getLogger(FileDuplicationFinder.class);

    private Map<Long, List<FileMetadata>> files;

    private long totalFileSize;
    private long uniqueFileSize;

    public FileDuplicationFinder() {
        files = new HashMap<>();

        totalFileSize = 0;
        uniqueFileSize = 0;
    }

    public void scanFolder(String folder) throws IOException {
        LOGGER.info("Start scanning folder: " + folder);

        PathIterator pathIterator = new PathIterator(Paths.get(folder), true);

        while (pathIterator.hasNext()) {
            Path file = pathIterator.next();
            processFoundFile(file);
            reportResults();
        }
    }

    private void processFoundFile(Path file) throws IOException {
        FileMetadata fileMeatadata = new FileMetadata(file);

        long fileSize = fileMeatadata.getSize();
        if (!files.containsKey(fileSize)) {
            // Creating new List
            List<FileMetadata> fileList = new ArrayList<>();
            files.put(fileSize, fileList);

            uniqueFileSize += fileSize;
        }

        // Adding File to existing list
        List<FileMetadata> fileList = files.get(fileSize);
        fileList.add(fileMeatadata);

        totalFileSize += fileSize;
    }

    public void reportResults() {
        System.out.println("\tTotal: " + FormattingUtils.humanReadableSizeBi(totalFileSize)
                + "b, Unique: " + FormattingUtils.humanReadableSizeBi(uniqueFileSize)
                + "b, Duplicates: " + FormattingUtils.humanReadableSizeBi(totalFileSize - uniqueFileSize)
                + "b");
    }
}
