package app.core.dto;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class FileDatabase {
    private String lastUpdated;

    private Map<String, AbsoluteFileMetadata> files;

    @JsonIgnore
    private DateFormat lastUpdatedDateFormatter;

    public FileDatabase() {
        files = new HashMap<>();
        lastUpdatedDateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    }

    public String getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated() {
        this.lastUpdated = lastUpdatedDateFormatter.format(new Date());
    }

    public Map<String, AbsoluteFileMetadata> getFiles() {
        return files;
    }

    public void addFileMetadata(AbsoluteFileMetadata fileMetadata) {
        files.put(fileMetadata.getAbsolutePath(), fileMetadata);
        setLastUpdated();
    }
}
