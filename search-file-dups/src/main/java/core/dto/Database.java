package core.dto;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Database {
    private String name;
    private String path;
    private String lastUpdated;

    private Map<String, FileMetadata> files;

    @JsonIgnore
    private DateFormat lastUpdatedDateFormatter;

    public Database() {
        files = new HashMap<>();
        lastUpdatedDateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated() {
        this.lastUpdated = lastUpdatedDateFormatter.format(new Date());
    }

    public Map<String, FileMetadata> getFiles() {
        return files;
    }

    public void addFileMetadata(FileMetadata fileMetadata) {
        fileMetadata.setDatabase(this);
        files.put(fileMetadata.getRelativePath(), fileMetadata);
    }

    public void linkFiles() {
        Iterator<FileMetadata> iterator = files.values().iterator();
        while (iterator.hasNext()) {
            FileMetadata fileMetadata = iterator.next();
            fileMetadata.setDatabase(this);
        }
    }
}
