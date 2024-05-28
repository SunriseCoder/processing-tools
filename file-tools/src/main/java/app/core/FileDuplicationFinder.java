package app.core;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import app.collection.RList;
import app.core.dto.Configuration;
import app.core.dto.FileDatabase;
import app.core.dto.FileDuplicationsGroup;
import app.core.dto.FileMetadata;
import app.files.PathIterator;
import app.structures.Pair;
import app.utils.ChecksumUtils;
import app.utils.FormattingUtils;

public class FileDuplicationFinder {
    private static final Logger LOGGER = LogManager.getLogger(FileDuplicationFinder.class);

    private List<String> checksumAlgorithms;
    private FileDatabase fileDatabase;
    private List<Path> baseFolders;
    private List<Path> externalFolders;

    private Map<String, FileDuplicationsGroup> fileDuplicationsGroupsMap;

    // Required for fast approximate duplication size estimation;
    private Map<Long, List<FileMetadata>> baseFilesMapBySize;
    private Map<Long, List<FileMetadata>> externalFilesMapBySize;
    private long totalFilesSize;
    private long uniqueFilesSize;
    private long totalNumberOfFiles;

    // For Base Folders
    private Map<String, List<Pair<FileMetadata, FileDuplicationsGroup>>> baseFilesMapBySizeAndChecksums;
    private List<FileMetadata> baseFilesToComputeChecksums;
    private List<FileMetadata> baseFilesWithoutGroup;

    // For Deleted Files
    private Map<String, List<FileMetadata>> deletedFilesMapBySizeAndChecksums;

    // For External Folders
    private Map<String, List<FileMetadata>> externalFilesMapBySizeAndChecksums;
    private List<FileMetadata> externalFilesToComputeChecksums;


    public FileDuplicationFinder() {
        fileDuplicationsGroupsMap = new HashMap<>();

        baseFilesMapBySize = new HashMap<>();
        externalFilesMapBySize = new HashMap<>();
        totalFilesSize = 0;
        uniqueFilesSize = 0;
        totalNumberOfFiles = 0;

        baseFilesMapBySizeAndChecksums = new HashMap<>();
        baseFilesToComputeChecksums = new ArrayList<>();
        baseFilesWithoutGroup = new ArrayList<>();

        deletedFilesMapBySizeAndChecksums = new HashMap<>();

        externalFilesMapBySizeAndChecksums = new HashMap<>();
        externalFilesToComputeChecksums = new ArrayList<>();
    }

    public void setConfiguration(Configuration configuration) {
        checksumAlgorithms = configuration.getChecksumAlgorithms();
        makeFileDuplicationsGroupsMap(configuration);
    }

    private void makeFileDuplicationsGroupsMap(Configuration configuration) {
        for (FileDuplicationsGroup group : configuration.getFileDuplicationsGroups()) {
            for (String path : group.getPaths()) {
                if (fileDuplicationsGroupsMap.containsKey(path)) {
                    throw new ConfigurationMistakeException("fileDuplicationsGroup - " + group.getName() + " has path, which is already exists: " + path);
                }

                fileDuplicationsGroupsMap.put(path, group);
            }
        }
    }

    public void setFileDatabase(FileDatabase fileDatabase) {
        this.fileDatabase = fileDatabase;
    }

    public void setBaseFolders(List<Path> baseFolders) {
        this.baseFolders = baseFolders;
    }

    public void setExternalFolders(List<Path> externalFolders) {
        this.externalFolders = externalFolders;
    }

    public void findDuplications() throws IOException, NoSuchAlgorithmException {
        // Scanning Folders
        scanBaseFolders();
        loadDeletedFiles();
        if (externalFolders != null) {
            scanExternalFolders();
        }
        fileDatabase.suggestSave();

        // Computing Checksums
        computeFilesChecksums();

        // Searching Duplicates
        findBaseDuplications();
        if (externalFolders != null) {
            findExternalDuplications();
        }
    }

    private void scanBaseFolders() throws IOException {
        LOGGER.info("Scanning Base Folders...");

        for (Path folder : baseFolders) {
            LOGGER.info("Scanning Base Folder: " + folder.toString());

            PathIterator pathIterator = new PathIterator(folder, true);
            while (pathIterator.hasNext()) {
                Path file = pathIterator.next();
                FileMetadata fileMetadata = fileDatabase.updateAndGetFileMetadata(file);

                // Sort FileMetadata with Checksums and without to Compute Checksums Queue
                if (fileMetadata == null) {
                    LOGGER.warn("A Base File has miraculously disappear, skipping this file: " + file.toString());
                    continue;
                } else if (fileMetadata.hasChecksums(checksumAlgorithms)) {
                    addToBaseMapByChecksums(fileMetadata);
                } else {
                    baseFilesToComputeChecksums.add(fileMetadata);
                }

                // Calculations for Fast approximate Duplications Size estimation
                long fileSize = fileMetadata.getSize();
                List<FileMetadata> fileList = baseFilesMapBySize.get(fileSize);
                if (fileList == null) {
                    fileList = new ArrayList<>();
                    baseFilesMapBySize.put(fileSize, fileList);
                    uniqueFilesSize += fileSize;
                }
                fileList.add(fileMetadata);
                totalFilesSize += fileSize;
                totalNumberOfFiles++;

                // Printing Fast Approximate Estimation
                System.out.print("\rApproximately by File Sizes only!!! Total: " + FormattingUtils.humanReadableSizeBi(totalFilesSize)
                        + "b, Unique: " + FormattingUtils.humanReadableSizeBi(uniqueFilesSize)
                        + "b, Duplicates: " + FormattingUtils.humanReadableSizeBi(totalFilesSize - uniqueFilesSize)
                        + "b, Files: " + totalNumberOfFiles + "          ");
            }

            // New Line to close Fast Approximate Estimation
            System.out.println();
        }

        // If there are any Files without Group - throwing an Exception
        if (!baseFilesWithoutGroup.isEmpty()) {
            throw new IllegalStateException("File Duplication Groups not found for the following files:\n"
                    + baseFilesWithoutGroup.stream().map(m -> m.getAbsolutePath()).collect(Collectors.joining("\n")));
        }

        LOGGER.info("Base Folders have been scanned successfully");
    }

    private void addToBaseMapByChecksums(FileMetadata fileMetadata) {
        String checksumsString = ChecksumUtils.generateSizeAndChecksumsString(checksumAlgorithms, fileMetadata.getSize(), fileMetadata.getChecksums());

        FileDuplicationsGroup group = findFileDuplicationsGroup(fileMetadata);
        if (group == null) {
            baseFilesWithoutGroup.add(fileMetadata);
            return;
        }

        List<Pair<FileMetadata, FileDuplicationsGroup>> pairList = baseFilesMapBySizeAndChecksums.get(checksumsString);
        if (pairList == null) {
            pairList = new ArrayList<>();
            baseFilesMapBySizeAndChecksums.put(checksumsString, pairList);
        }
        Pair<FileMetadata, FileDuplicationsGroup> pair = new Pair<>(fileMetadata, group);
        pairList.add(pair);
    }

    private FileDuplicationsGroup findFileDuplicationsGroup(FileMetadata fileMetadata) {
        String path = fileMetadata.getAbsolutePath();

        List<Pair<String, FileDuplicationsGroup>> matchingGroups = new ArrayList<>();
        for (Entry<String, FileDuplicationsGroup> groupEntry : fileDuplicationsGroupsMap.entrySet()) {
            String entryPath = groupEntry.getKey();
            if (path.startsWith(entryPath)) {
                matchingGroups.add(new Pair<>(entryPath, groupEntry.getValue()));
            }
        }

        if (matchingGroups.isEmpty()) {
            return null;
        }

        matchingGroups.sort((a, b) -> b.getKey().length() - a.getKey().length());
        FileDuplicationsGroup result = matchingGroups.get(0).getValue();
        return result;
    }

    private void loadDeletedFiles() {
        LOGGER.info("Loading Deleted Files...");

        RList<FileMetadata> deletedFiles = fileDatabase.getDeletedFiles();
        for (FileMetadata file : deletedFiles) {
            String sizeAndChecksumsString = ChecksumUtils.generateSizeAndChecksumsString(checksumAlgorithms, file.getSize(), file.getChecksums());

            List<FileMetadata> fileList = deletedFilesMapBySizeAndChecksums.get(sizeAndChecksumsString);
            if (fileList == null) {
                fileList = new ArrayList<>();
                deletedFilesMapBySizeAndChecksums.put(sizeAndChecksumsString, fileList);
            }

            fileList.add(file);
        }
    }

    private void scanExternalFolders() throws IOException {
        LOGGER.info("Scanning External Folders...");

        for (Path folder : externalFolders) {
            LOGGER.info("Scanning External Folder: " + folder.toString());

            PathIterator pathIterator = new PathIterator(folder, true);
            while (pathIterator.hasNext()) {
                Path file = pathIterator.next();
                FileMetadata fileMetadata = fileDatabase.updateAndGetFileMetadata(file);

                // Sort FileMetadata with Checksums and without to Compute Checksums Queue
                if (fileMetadata == null) {
                    LOGGER.warn("A Base File has miraculously disappear, skipping this file: " + file.toString());
                    continue;
                } else if (fileMetadata.hasChecksums(checksumAlgorithms)) {
                    addToExternalMapByChecksums(fileMetadata);
                } else {
                    externalFilesToComputeChecksums.add(fileMetadata);
                }

                // Calculations for Fast approximate Duplications Size estimation
                long fileSize = fileMetadata.getSize();
                List<FileMetadata> fileList = externalFilesMapBySize.get(fileSize);
                if (fileList == null) {
                    fileList = new ArrayList<>();
                    externalFilesMapBySize.put(fileSize, fileList);
                    // Considering File as Unique only when it is not present in both maps - base and external
                    if (!baseFilesMapBySize.containsKey(fileSize)) {
                        uniqueFilesSize += fileSize;
                    }
                }
                fileList.add(fileMetadata);
                totalFilesSize += fileSize;
                totalNumberOfFiles++;

                // Printing Fast Approximate Estimation
                System.out.print("\rApproximately by File Size only!!! Total: " + FormattingUtils.humanReadableSizeBi(totalFilesSize)
                        + "b, Unique: " + FormattingUtils.humanReadableSizeBi(uniqueFilesSize)
                        + "b, Duplicates: " + FormattingUtils.humanReadableSizeBi(totalFilesSize - uniqueFilesSize)
                        + "b, Files: " + totalNumberOfFiles + "          ");
            }

            // New Line to close Fast Approximate Estimation
            System.out.println();
        }

        LOGGER.info("External Folders have been scanned successfully");
    }

    private void addToExternalMapByChecksums(FileMetadata fileMetadata) {
        String checksumsString = ChecksumUtils.generateSizeAndChecksumsString(checksumAlgorithms, fileMetadata.getSize(), fileMetadata.getChecksums());

        FileDuplicationsGroup group = findFileDuplicationsGroup(fileMetadata);
        if (group == null) {
            baseFilesWithoutGroup.add(fileMetadata);
        }

        List<FileMetadata> list = externalFilesMapBySizeAndChecksums.get(checksumsString);
        if (list == null) {
            list = new ArrayList<>();
            externalFilesMapBySizeAndChecksums.put(checksumsString, list);
        }
        list.add(fileMetadata);
    }

    private void computeFilesChecksums() throws NoSuchAlgorithmException, IOException {
        long computeChecksumsTotalFilesSize = baseFilesToComputeChecksums.stream().mapToLong(m -> m.getSize()).sum();
        computeChecksumsTotalFilesSize += externalFilesToComputeChecksums.stream().mapToLong(m -> m.getSize()).sum();
        LOGGER.info("Computing File Checksums, file size to Compute: "
                + FormattingUtils.humanReadableSizeBi(computeChecksumsTotalFilesSize) + "b");

        // Compute File Checksums
        List<FileMetadata> fileListToComputeChecksums = new ArrayList<>();
        fileListToComputeChecksums.addAll(baseFilesToComputeChecksums);
        fileListToComputeChecksums.addAll(externalFilesToComputeChecksums);

        MultiThreadChecksumComputer checksumComputer = new MultiThreadChecksumComputer(checksumAlgorithms);
        checksumComputer.computeChecksums(fileListToComputeChecksums);
        // TODO Implement multi-threaded computing, i.e. one thread per physical drive
        /*ChecksumComputer checksumComputer = new ChecksumComputer(checksumAlgorithms);
        checksumComputer.setAllFilesSize(computeChecksumsTotalFilesSize);
        for (FileMetadata fileMetadata : fileListToComputeChecksums) {
            Map<String, String> checksums = checksumComputer.computeChecksums(new File(fileMetadata.getAbsolutePath()));
            fileMetadata.addChecksums(checksums);
            fileDatabase.suggestSave();
        }*/

        // Checking that the Checksums have been computed for All the files
        List<String> filesWithoutChecksums = fileListToComputeChecksums.stream()
                .filter(m -> !m.hasChecksums(checksumAlgorithms))
                .map(m -> m.getAbsolutePath())
                .collect(Collectors.toList());
        if (!filesWithoutChecksums.isEmpty()) {
            throw new IOException("Could not compute Checksums for the following files:\n" + String.join("\n", filesWithoutChecksums));
        }

        // Base Files
        Iterator<FileMetadata> iterator = baseFilesToComputeChecksums.iterator();
        while (iterator.hasNext()) {
            FileMetadata fileMetadata = iterator.next();
            addToBaseMapByChecksums(fileMetadata);
            iterator.remove();
        }

        // External Files
        iterator = externalFilesToComputeChecksums.iterator();
        while (iterator.hasNext()) {
            FileMetadata fileMetadata = iterator.next();
            addToExternalMapByChecksums(fileMetadata);
            iterator.remove();
        }

        LOGGER.info("Computing File Checksums is completed");
    }

    private void findBaseDuplications() throws FileNotFoundException {
        LOGGER.info("Starting to find check Base Files Duplications");

        try (PrintWriter pwBat = new PrintWriter("remove-base-file-duplications.bat");
            PrintWriter pwBash = new PrintWriter("remove-base-file-duplications.sh");) {

            for (Entry<String, List<Pair<FileMetadata, FileDuplicationsGroup>>> entry : baseFilesMapBySizeAndChecksums.entrySet()) {
                String sizeAndChecksumString = entry.getKey();
                List<Pair<FileMetadata, FileDuplicationsGroup>> pairList = entry.getValue();

                pairList.sort((a, b) -> b.getValue().getPriority() - a.getValue().getPriority());

                List<FileMetadata> readOnlyFiles = pairList.stream().filter(e -> e.getValue().isReadOnly()).map(e -> e.getKey()).collect(Collectors.toList());
                List<FileMetadata> deletableFiles = pairList.stream().filter(e -> !e.getValue().isReadOnly()).map(e -> e.getKey()).collect(Collectors.toList());

                List<FileMetadata> survivors = new ArrayList<>();
                List<FileMetadata> duplicates = new ArrayList<>();
                survivors.addAll(readOnlyFiles);
                if (survivors.isEmpty()) {
                    // If there are no read-only files, adding the first one from deletable files list and consider others as duplicates
                    survivors.add(deletableFiles.get(0));
                    for (int i = 1; i < deletableFiles.size(); i++) {
                        duplicates.add(deletableFiles.get(i));
                    }
                } else {
                    // Otherwise (if there are one or more read-only files) - consider all deletable files as duplicates
                    duplicates.addAll(deletableFiles);
                }

                pwBat.println();
                pwBash.println();
                pwBat.println("REM " + sizeAndChecksumString);
                pwBash.println("# " + sizeAndChecksumString);

                for (FileMetadata file : survivors) {
                    pwBat.println("REM Survivor " + file.getAbsolutePath());
                    pwBash.println("# Survivor " + file.getAbsolutePath());
                }

                for (FileMetadata file : duplicates) {
                    pwBat.println("DEL \"" + file.getAbsolutePath() + "\"");
                    pwBash.println("rm \"" + file.getAbsolutePath() + "\"");
                }
            }
        }
    }

    private void findExternalDuplications() throws FileNotFoundException {
        LOGGER.info("Starting to find check Base Files Duplications");

        try (PrintWriter pwRemoveDuplicationsBat = new PrintWriter("remove-external-file-duplications.bat");
            PrintWriter pwRemoveDuplicationsBash = new PrintWriter("remove-external-file-duplications.sh");
            PrintWriter pwMoveUniqueBat = new PrintWriter("move-external-unique-files.bat");
            PrintWriter pwMoveUniqueBash = new PrintWriter("move-external-unique-files.sh");) {

            pwMoveUniqueBat.println("set destination=\"D:\\Income\"");
            pwMoveUniqueBat.println();

            pwMoveUniqueBash.println("#!/bin/bash -x");
            pwMoveUniqueBash.println();
            pwMoveUniqueBash.println("destination=\"/mnt/income\"");
            pwMoveUniqueBash.println();

            for (Entry<String, List<FileMetadata>> entry : externalFilesMapBySizeAndChecksums.entrySet()) {
                String sizeAndChecksumString = entry.getKey();
                List<FileMetadata> fileList = entry.getValue();

                // If such files are existing in Base Files Map - considering whole group as duplications
                List<Pair<FileMetadata, FileDuplicationsGroup>> baseFilesList = baseFilesMapBySizeAndChecksums.get(sizeAndChecksumString);
                if (baseFilesList != null) {
                    String textReason = "Found Base Files:";
                    List<FileMetadata> reasonFileList = baseFilesList.stream().map(p -> p.getKey()).collect(Collectors.toList());
                    processExternalDuplicates(fileList, textReason, reasonFileList, pwRemoveDuplicationsBat, pwRemoveDuplicationsBash);
                    continue;
                }

                // TODO Implement check among deleted files
                // If such files are existing in Deleted Files Map - considering whole group as duplications
                List<FileMetadata> deletedFilesList = deletedFilesMapBySizeAndChecksums.get(sizeAndChecksumString);
                if (deletedFilesList != null) {
                    String textReason = "Found Deleted Files:";
                    processExternalDuplicates(fileList, textReason, deletedFilesList, pwRemoveDuplicationsBat, pwRemoveDuplicationsBash);
                    continue;
                }

                List<FileMetadata> survivors = new ArrayList<>();
                List<FileMetadata> duplicates = new ArrayList<>();
                survivors.add(fileList.get(0));
                for (int i = 1; i < fileList.size(); i++) {
                    duplicates.add(fileList.get(i));
                }
                processExternalSuvivors(survivors, "External Files only:", duplicates, pwMoveUniqueBat, pwMoveUniqueBash);
                processExternalDuplicates(duplicates, "External Files only:", survivors, pwRemoveDuplicationsBat, pwRemoveDuplicationsBash);
            }
        }
    }

    private void processExternalDuplicates(List<FileMetadata> fileList, String textReason, List<FileMetadata> reasonFileList,
            PrintWriter pwRemoveDuplicationsBat, PrintWriter pwRemoveDuplicationsBash) {
        pwRemoveDuplicationsBat.println();
        pwRemoveDuplicationsBash.println();

        pwRemoveDuplicationsBat.println("REM Duplicates because: " + textReason);
        pwRemoveDuplicationsBash.println("# Duplicates because: " + textReason);

        for (FileMetadata file : reasonFileList) {
            pwRemoveDuplicationsBat.println("REM " + file.getAbsolutePath());
            pwRemoveDuplicationsBash.println("# " + file.getAbsolutePath());
        }

        for (FileMetadata file : fileList) {
            pwRemoveDuplicationsBat.println("DEL \"" + file.getAbsolutePath() + "\"");
            pwRemoveDuplicationsBash.println("rm \"" + file.getAbsolutePath() + "\"");
        }
    }

    private void processExternalSuvivors(List<FileMetadata> fileList, String textReason, List<FileMetadata> reasonFileList,
            PrintWriter pwMoveUniqueBat, PrintWriter pwMoveUniqueBash) {
        pwMoveUniqueBat.println();
        pwMoveUniqueBash.println();

        pwMoveUniqueBat.println("REM Survivors because: " + textReason);
        pwMoveUniqueBash.println("# Survivors because: " + textReason);

        for (FileMetadata file : reasonFileList) {
            pwMoveUniqueBat.println("REM Duplicate: " + file.getAbsolutePath());
            pwMoveUniqueBash.println("# Duplicate: " + file.getAbsolutePath());
        }

        for (FileMetadata file : fileList) {
            Path filePath = Paths.get(file.getAbsolutePath());
            Path folder = getExternalFolder(filePath);
            String relativePath = folder.relativize(filePath).toString();

            pwMoveUniqueBat.println("MOVE \"" + file.getAbsolutePath() + "\" \"%destination%" + relativePath  + "\"");
            pwMoveUniqueBash.println("mv \"" + file.getAbsolutePath() + "\" \"${destination}" + relativePath + "\"");
        }
    }

    private Path getExternalFolder(Path targetPath) {
        List<Path> matchingFolders = new ArrayList<>();
        for (Path folder : externalFolders) {
            if (targetPath.startsWith(folder)) {
                matchingFolders.add(folder);
            }
        }

        matchingFolders.sort((a, b) -> b.toString().length() - a.toString().length());
        Path result = matchingFolders.get(0);
        return result;
    }
}
