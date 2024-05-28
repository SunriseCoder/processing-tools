package app.core.dto;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;

import app.checksum.ChecksumComputer;
import app.collection.RList;
import app.collection.wrap.RListWrap;
import app.json.JsonSaveable;
import app.utils.JSONUtils;
import app.utils.PathUtils;

// TODO Implement pattern Listener
// TODO Replace all Getters of Collections with ReadOnly Collections
public class FileDatabase extends JsonSaveable {
    @JsonIgnore
    private static final Logger LOGGER = LogManager.getLogger(FileDatabase.class);
    @JsonIgnore
    private static final String FILE_DATABASE_FILENAME = "file-database.json";
    @JsonIgnore
    private static final String FILE_DATABASE_INCOMPLETE_FILENAME = "file-database-INCOMPLETE.json";

    @JsonProperty
    private Map<String, FileMetadata> activeFiles;
    @JsonProperty
    private List<FileMetadata> deletedFiles;

    // TODO Try to remove this field
    @JsonIgnore
    private ChecksumComputer checksumComputer;
    // TODO Try to remove this field
    @JsonIgnore
    private List<String> checksumAlgorithms;

    private FileDatabase() {
        activeFiles = new HashMap<>();
        deletedFiles = new ArrayList<>();
    }

    public static FileDatabase load() throws IOException, NoSuchAlgorithmException {
        Configuration configuration = Configuration.load();
        FileDatabase result = load(configuration);
        return result;
    }

    public static FileDatabase load(Configuration configuration) throws IOException, NoSuchAlgorithmException {
        LOGGER.info("Loading File Database...");

        String applicationHomePath = System.getProperty("app.home");
        File fileDatabaseFile = new File(applicationHomePath, FILE_DATABASE_FILENAME);
        FileDatabase fileDatabase;
        if (fileDatabaseFile.exists()) {
            TypeReference<FileDatabase> typeReference = new TypeReference<FileDatabase>() {};
            fileDatabase = JSONUtils.loadFromDisk(fileDatabaseFile, typeReference);
        } else {
            fileDatabase = new FileDatabase();
        }

        fileDatabase.setSaveFileComplete(fileDatabaseFile);
        fileDatabase.setSaveFileIncomplete(new File(applicationHomePath, FILE_DATABASE_INCOMPLETE_FILENAME));
        fileDatabase.setMinimalSaveIntervalInMS(configuration.getFileDatabaseMinimalSaveIntervalInMS());
        // TODO Remove this line
        fileDatabase.setChecksumComputer(new ChecksumComputer(configuration.getChecksumAlgorithms()));
        // TODO Remove this line
        fileDatabase.setChecksumAlgorithms(configuration.getChecksumAlgorithms());

        fileDatabase.postConstruct();

        return fileDatabase;
    }

    // TODO Refactor
    //  1. Return RMap instead of Map
    //  2. Try to remove this method and replace its calls with retrieving one FileMetadata at a time
    //      It it is not so easy - try to revise algorithms and rewrite them this way
    @JsonIgnore
    public Map<String, FileMetadata> getActiveFiles() {
        return activeFiles;
    }

    // TODO Refactor
    //      1. Remove this method
    //      2. Implement all possible searches among deleted files and use everywhere their invocations only
    @JsonIgnore
    public RList<FileMetadata> getDeletedFiles() {
        return new RListWrap<>(deletedFiles);
    }

    // TODO Check calls of this method and try to use other methods instead.
    //      Make this method private if it is used in this class
    public void addFileMetadata(FileMetadata fileMetadata) {
        // TODO Remove the following line after implementing Listener pattern
        fileMetadata.setFileDatabase(this);

        FileMetadata oldFileMetadata = activeFiles.put(fileMetadata.getAbsolutePath(), fileMetadata);
        // TODO !!!Investigate, looks like here must be good equals method instead of "!="
        if (oldFileMetadata != fileMetadata) {
            setChanged();
        }
    }

    @JsonIgnore
    public FileMetadata getFileMetadataByAbsolutePath(String absolutePath) {
        return activeFiles.get(absolutePath);
    }

    // TODO Try to remove this method
    public void setChecksumComputer(ChecksumComputer checksumComputer) {
        this.checksumComputer = checksumComputer;
    }

    // TODO Try to remove this method
    public void setChecksumAlgorithms(List<String> checksumAlgorithms) {
        this.checksumAlgorithms = checksumAlgorithms;
    }

    // TODO Try to remove this method because it cannot give any checksum calculation estimations to the user
    public void computeChecksumsIfNotComputedYet(String fileAbsolutePath) throws IOException {
        FileMetadata fileMetadata = activeFiles.get(fileAbsolutePath);
        File file = new File(fileAbsolutePath);

        // If Physical File doesn't exist or it is not a file - deleting Entry from the FileDatabase and return
        if (!file.exists() || !file.isFile()) {
            if (fileMetadata != null) {
                activeFiles.remove(fileAbsolutePath);
            }
            return;
        }

        // If FileMetadata and Physical File are existing, but have different sizes or lastModified time - replacing the old FileMetadata
        if (fileMetadata != null && file.exists() && file.isFile()) {
            BasicFileAttributes attributes = Files.readAttributes(Paths.get(fileAbsolutePath), BasicFileAttributes.class);
            if (fileMetadata.getSize() != attributes.size() || !fileMetadata.getLastModified().equals(attributes.lastModifiedTime())) {
                fileMetadata = new FileMetadata(Paths.get(fileAbsolutePath));
                activeFiles.put(fileAbsolutePath, fileMetadata);
            }
        }

        // If file not in the FileDatabase yet, but exists on the disk and is a File, adding it to the FileDatabase
        if (fileMetadata == null && file.exists() && file.isFile()) {
            fileMetadata = new FileMetadata(Paths.get(fileAbsolutePath));
            activeFiles.put(fileAbsolutePath, fileMetadata);
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

    // TODO Check calls of this method and try to use other methods instead.
    //      Make this method private if it is used in this class
    public void remove(FileMetadata fileMetadata) {
        String absolutePath = fileMetadata.getAbsolutePath();
        remove(absolutePath);
    }

    // TODO Check calls of this method and try to use other methods instead.
    //      Make this method private if it is used in this class
    public void remove(String absolutePath) {
        FileMetadata fileMetadata = activeFiles.remove(absolutePath);
        if (fileMetadata != null) {
            // Adding to deletedFiles has sense when we have fileMetadata with checksums only
            if (fileMetadata.hasChecksums(checksumAlgorithms)) {
                deletedFiles.add(fileMetadata);
            }
            setChanged();
        }
    }

    public FileMetadata updateAndGetFileMetadata(Path filePath) throws IOException {
        if (!filePath.isAbsolute()) {
            filePath = filePath.toAbsolutePath();
        }

        if (!PathUtils.exists(filePath)) {
            remove(filePath.toString());
            return null;
        }

        if (PathUtils.isDirectory(filePath)) {
            throw new IllegalArgumentException("Given filePath must be a file, but actually is a directory: " + filePath.toString());
        }

        FileMetadata fileMetadataOnDisk = new FileMetadata(filePath);
        FileMetadata fileMetadataInDatabase = getFileMetadataByAbsolutePath(filePath.toString());

        if (fileMetadataInDatabase == null) {
            addFileMetadata(fileMetadataOnDisk);
            return fileMetadataOnDisk;
        }

        if (fileMetadataOnDisk.getLastModified().equals(fileMetadataInDatabase.getLastModified())) {
            return fileMetadataInDatabase;
        }

        // Replacing old FileMetadata in FileDatabase with new fresh FileMetadata
        // TODO Refactor - first refactor addFileMetadata method and replace the following block of code with addFileMetadata method
        activeFiles.put(filePath.toString(), fileMetadataOnDisk);
        if (fileMetadataInDatabase.hasChecksums(checksumAlgorithms)) {
            deletedFiles.add(fileMetadataInDatabase);
        }
        setChanged();
        //addFileMetadata(fileMetadataOnDisk);

        return fileMetadataOnDisk;
    }

    public void updateEntryFromDisk(Path absolutePath, boolean readOnly) throws IOException {
        // If physical file on disk does not exist -> remove from FileDatabase
        if (Files.notExists(absolutePath)) {
            remove(absolutePath.toString());
            return;
        }

        // Otherwise updating the record if needed
        FileMetadata newFileMetadata = new FileMetadata(absolutePath);
        newFileMetadata.setExistsOnDiskNow(true);
        newFileMetadata.setReadOnly(readOnly);
        updateEntryIfOutdated(newFileMetadata);
    }

    public void updateEntryIfOutdated(FileMetadata newFileMetadata) {
        FileMetadata existingFileMetadata = getFileMetadataByAbsolutePath(newFileMetadata.getAbsolutePath());
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

        Iterator<Entry<String, FileMetadata>> fileIterator = activeFiles.entrySet().iterator();
        while (fileIterator.hasNext()) {
            Entry<String, FileMetadata> fileMetadataEntry = fileIterator.next();
            FileMetadata fileMetadata = fileMetadataEntry.getValue();
            if (!fileMetadata.isExistsOnDiskNow()) {
                fileIterator.remove();
                setChanged();
            }
        }

        LOGGER.info("Removing Old Deleted Files from the FileDatabase is done");
    }

    // TODO Revise logic, use pattern Listener instead of adding the database link to the file metadata
    public void postConstruct() {
        activeFiles.values().stream().forEach(f -> f.setFileDatabase(this));
    }
}
