package backuper.client.dto;

public class BackupTask {
    private String name;
    private String source;
    private String destination;

    public BackupTask() {
        // Default constructor for deserializating
    }

    public BackupTask(String source, String destination) {
        this.source = source;
        this.destination = destination;
    }

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

    @Override
    public String toString() {
        return name + ": \"" + source + "\" -> \"" + destination + "\"";
    }
}
