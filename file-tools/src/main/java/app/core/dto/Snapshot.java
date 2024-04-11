package app.core.dto;

import java.util.Map;
import java.util.TreeMap;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import app.json.JsonSaveable;

// TODO Implement Pattern Observer
public class Snapshot extends JsonSaveable {
    @JsonProperty
    private String name;
    private Map<String, SnapshotFile> files;

    public Snapshot() {
        files = new TreeMap<>();
    }

    public String getName() {
        return name;
    }

    @JsonIgnore
    public void setName(String name) {
        this.name = name;
        setChanged();
    }

    @JsonIgnore
    public Map<String, SnapshotFile> getFilesMap() {
        return files;
    }

    @JsonIgnore
    public SnapshotFile getFileMetadata(String relativePath) {
        return files.get(relativePath);
    }

    public void addFileMetadata(SnapshotFile fileMetadata) {
        fileMetadata.addChangedListener(this);
        files.put(fileMetadata.getRelativePath(), fileMetadata);
        setChanged();
    }

    //TODO Rewrite - let FolderSnapshot have all necessary files - ChecksumComputer with ProgressPrinter (if needed)
    //      and compute all checksums and set them inside this class in order to prevent violation of Demetra's Law
    public void addChecksums(SnapshotFile fileMetadata, Map<String, String> checksums) {
        fileMetadata.addAllChecksums(checksums);
        setChanged();
    }

    public void setFileContentReadError(SnapshotFile fileMetadata, String errorMessage) {
        fileMetadata.setReadContentError(errorMessage);
        setChanged();
    }
}
