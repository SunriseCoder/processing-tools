package backuper.client.config;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class BackupTask {
    private String name;
    private String source;
    private String destination;
    private String tmp;

    @JsonIgnore
    private CopySettings copySettings;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getTmp() {
        return tmp;
    }

    public void setTmp(String tmp) {
        this.tmp = tmp;
    }

    public CopySettings getCopySettings() {
        return copySettings;
    }

    public void setCopySettings(CopySettings copySettings) {
        this.copySettings = copySettings;
    }

    @Override
    public String toString() {
        return name + ": \"" + source + "\" -> \"" + destination + "\"";
    }
}
