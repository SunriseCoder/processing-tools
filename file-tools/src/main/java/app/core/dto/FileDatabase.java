package app.core.dto;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import app.checksum.ChecksumComputer;
import app.json.JsonSaveable;

public class FileDatabase extends JsonSaveable {
    @JsonIgnore
    private static final Logger LOGGER = LogManager.getLogger(FileDatabase.class);

    @JsonProperty
    private Map<String, AbsoluteFileMetadata> files;

    @JsonIgnore
    private ChecksumComputer checksumComputer;
    @JsonIgnore
    private List<String> checksumAlgorithms;

    public FileDatabase() {
        files = new HashMap<>();
    }

    @JsonIgnore
    public Map<String, AbsoluteFileMetadata> getFiles() {
        return files;
    }

    public void addFileMetadata(AbsoluteFileMetadata fileMetadata) {
        fileMetadata.setFileDatabase(this);
        AbsoluteFileMetadata oldFileMetadata = files.put(fileMetadata.getAbsolutePath(), fileMetadata);
        if (oldFileMetadata != fileMetadata) {
            setChanged();
        }
    }

    @JsonIgnore
    public AbsoluteFileMetadata getFileMetadataByAbsolutePath(String absolutePath) {
        return files.get(absolutePath);
    }

    public void setChecksumComputer(ChecksumComputer checksumComputer) {
        this.checksumComputer = checksumComputer;
    }

    public void setChecksumAlgorithms(List<String> checksumAlgorithms) {
        this.checksumAlgorithms = checksumAlgorithms;
    }

    public void computeChecksumsIfNotComputedYet(String fileAbsolutePath) throws IOException {
        AbsoluteFileMetadata fileMetadata = files.get(fileAbsolutePath);
        File file = new File(fileAbsolutePath);

        // If Physical File doesn't exist or it is not a file - deleting Entry from the FileDatabase and return
        if (!file.exists() || !file.isFile()) {
            if (fileMetadata != null) {
                files.remove(fileAbsolutePath);
            }
            return;
        }

        // If FileMetadata and Physical File are existing, but have different sizes or lastModified time - replacing the old FileMetadata
        if (fileMetadata != null && file.exists() && file.isFile()) {
            BasicFileAttributes attributes = Files.readAttributes(Paths.get(fileAbsolutePath), BasicFileAttributes.class);
            if (fileMetadata.getSize() != attributes.size() || !fileMetadata.getLastModified().equals(attributes.lastModifiedTime())) {
                fileMetadata = new AbsoluteFileMetadata(Paths.get(fileAbsolutePath));
                files.put(fileAbsolutePath, fileMetadata);
            }
        }

        // If file not in the FileDatabase yet, but exists on the disk and is a File, adding it to the FileDatabase
        if (fileMetadata == null && file.exists() && file.isFile()) {
            fileMetadata = new AbsoluteFileMetadata(Paths.get(fileAbsolutePath));
            files.put(fileAbsolutePath, fileMetadata);
        }

        // If Checksums for the File are not computed yet, computing
        if (!fileMetadata.hasChecksums(checksumAlgorithms)) {
            LOGGER.info("In the FileDatabase there are no Checksums for the File " + fileAbsolutePath + ", computing...");
            checksumComputer.reset();
            checksumComputer.setAllFilesSize(file.length());
            Map<String, String> checksums = checksumComputer.computeChecksums(file);
            fileMetadata.addChecksums(checksums);
        }
    }

    public void remove(AbsoluteFileMetadata absoluteFileMetadata) {
        remove(absoluteFileMetadata.getAbsolutePath());
    }

    public void remove(String absolutePath) {
        AbsoluteFileMetadata oldValue = files.remove(absolutePath);
        if (oldValue != null) {
            setChanged();
        }
    }

    public void updateEntryFromDisk(Path absolutePath, boolean readOnly) throws IOException {
        // If physical file on disk does not exist -> remove from FileDatabase
        if (Files.notExists(absolutePath)) {
            remove(absolutePath.toString());
            return;
        }

        // Otherwise updating the record if needed
        AbsoluteFileMetadata newFileMetadata = new AbsoluteFileMetadata(absolutePath);
        newFileMetadata.setExistsOnDiskNow(true);
        newFileMetadata.setReadOnly(readOnly);
        updateEntryIfOutdated(newFileMetadata);
    }

    public void updateEntryIfOutdated(AbsoluteFileMetadata newFileMetadata) {
        AbsoluteFileMetadata existingFileMetadata = getFileMetadataByAbsolutePath(newFileMetadata.getAbsolutePath());
        if (existingFileMetadata == null) {
            LOGGER.debug("Adding as a new file to the FileDatabase");
            // Adding a new file to the FileDatabase
            addFileMetadata(newFileMetadata);
        } else if(!existingFileMetadata.equalsByMetadata(newFileMetadata)) {
            LOGGER.debug("File in the FileDatabase is outdated, replacing with a new FileMetadata");
            // Overwriting old file metadata with the new (actual) one
            addFileMetadata(newFileMetadata);
        } else {
            LOGGER.debug("File is already in the FileDatabase and looks up-to-date");
            // Marking the existing file that it still exists on the disk
            existingFileMetadata.setExistsOnDiskNow(true);
            existingFileMetadata.setReadOnly(newFileMetadata.isReadOnly());
        }
    }

    // TODO Revise logic - some of the files which really must be deleted from disk
    //      (for example, temporary files, video files with problems, etc - remove from fileDatabaes
    // But at the same time, the files which have been processed (for example, raw recordings of already processed videos)
    //      - such files' metadata must be kept forever to let users delete their copies of these files
    public void removeEntriesForFilesDeletedFomDisk() throws IOException {
        LOGGER.info("Starting to Remove Old Deleted Files from the FileDatabase...");

        Iterator<Entry<String, AbsoluteFileMetadata>> fileIterator = files.entrySet().iterator();
        while (fileIterator.hasNext()) {
            Entry<String, AbsoluteFileMetadata> fileMetadataEntry = fileIterator.next();
            AbsoluteFileMetadata fileMetadata = fileMetadataEntry.getValue();
            if (!fileMetadata.isExistsOnDiskNow()) {
                fileIterator.remove();
                setChanged();
            }
        }

        LOGGER.info("Removing Old Deleted Files from the FileDatabase is done");
    }

    public void linkSubEntities() {
        files.values().stream().forEach(f -> f.setFileDatabase(this));
    }
}
