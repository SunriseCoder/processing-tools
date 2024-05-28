package app.core.dto;

import java.util.List;

public class FileDuplicationsGroup {
    private String name;
    private boolean readOnly;
    private int priority;
    private List<String> paths;

    public FileDuplicationsGroup() {
        // Default constructor
    }

    public String getName() {
        return name;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public int getPriority() {
        return priority;
    }

    public List<String> getPaths() {
        return paths;
    }
}
