package app.core.dto.fs;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class FileSystemFolder extends FileSystemElement {
    private Map<String, FileSystemFolder> folders;
    private Map<String, FileSystemFile> files;

    public FileSystemFolder() {
        this(null);
    }

    public FileSystemFolder(String name) {
        folders = new HashMap<>();
        files = new HashMap<>();
        this.setName(name);
    }

    @JsonIgnore
    public Collection<FileSystemFolder> getFolderCollection() {
        return folders.values();
    }

    @JsonIgnore
    public Collection<FileSystemFile> getFileCollection() {
        return files.values();
    }

    public void addFolder(FileSystemFolder folder) {
        folders.put(folder.getName(), folder);
    }

    public void addFile(FileSystemFile file) {
        files.put(file.getName(), file);
    }

    public boolean isNameFree(String name) {
        boolean isNameFree = !folders.containsKey(name);
        isNameFree &= !files.containsKey(name);
        return isNameFree;
    }
}
