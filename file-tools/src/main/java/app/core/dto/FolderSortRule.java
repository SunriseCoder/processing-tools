package app.core.dto;

public class FolderSortRule {
    private String source;
    private String destination;
    private String comment;
    private boolean includeParentFolder;
    private boolean addCurrentDateToDestinationFolderName;

    public String getSource() {
        return source;
    }

    public String getDestination() {
        return destination;
    }

    public String getComment() {
        return comment;
    }

    public boolean isIncludeParentFolder() {
        return includeParentFolder;
    }

    public boolean isAddCurrentDateToDestinationFolderName() {
        return addCurrentDateToDestinationFolderName;
    }
}
