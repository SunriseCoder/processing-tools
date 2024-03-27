package app.core.dto;

import java.util.Map;
import java.util.TreeMap;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import app.json.JsonSaveable;

public class FolderSnapshot extends JsonSaveable {
    @JsonProperty
    private String name;
    private Map<String, RelativeFileMetadata> files;

    public FolderSnapshot() {
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
    public Map<String, RelativeFileMetadata> getFilesMap() {
        return files;
    }

    @JsonIgnore
    public RelativeFileMetadata getFileMetadata(String relativePath) {
        return files.get(relativePath);
    }

    public void addFileMetadata(RelativeFileMetadata fileMetadata) {
        files.put(fileMetadata.getRelativePath(), fileMetadata);
        setChanged();
    }

    //TODO Rewrite - let FolderSnapshot have all necessary files - ChecksumComputer with ProgressPrinter (if needed)
    //      and compute all checksums and set them inside this class in order to prevent violation of Demetra's Law
    public void addChecksums(RelativeFileMetadata fileMetadata, Map<String, String> checksums) {
        fileMetadata.addAllChecksums(checksums);
        setChanged();
    }

    public void setFileContentReadError(RelativeFileMetadata fileMetadata, String errorMessage) {
        fileMetadata.setReadContentError(errorMessage);
        setChanged();
    }
}
