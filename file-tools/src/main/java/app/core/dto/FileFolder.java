package app.core.dto;

public class FileFolder {
    private String path;
    private boolean readOnly;

    public FileFolder() {
        // Default constructor
    }

    public FileFolder(String path, boolean readOnly) {
        super();
        this.path = path;
        this.readOnly = readOnly;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean isReadOnly() {
        return readOnly;
    }
}
