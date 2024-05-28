package app.core.dto;

public class AssemblerFileSource {
    private String path;
    private boolean readOnly;

    public AssemblerFileSource() {
        // Default constructor
    }

    public AssemblerFileSource(String path, boolean readOnly) {
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
