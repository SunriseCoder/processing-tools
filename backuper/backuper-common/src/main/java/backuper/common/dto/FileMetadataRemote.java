package backuper.common.dto;

public class FileMetadataRemote {
    private String name;
    private String path;
    private long size;

    private long creationTime;
    private long lastModifiedTime;
    private long lastAccessTime;

    private boolean directory;
    private boolean symlink;

    public FileMetadataRemote() {
        // Default constructor for deserialization
    }

    public FileMetadataRemote(FileMetadata fileMetadata) {
        name = fileMetadata.getName();
        path = fileMetadata.getRelativePath().toString();
        size = fileMetadata.getSize();

        creationTime = fileMetadata.getCreationTime().toMillis();
        lastModifiedTime = fileMetadata.getLastModified().toMillis();
        lastAccessTime = fileMetadata.getLastAccessTime().toMillis();

        directory = fileMetadata.isDirectory();
        symlink = fileMetadata.isSymlink();
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

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(long creationTime) {
        this.creationTime = creationTime;
    }

    public long getLastModifiedTime() {
        return lastModifiedTime;
    }

    public void setLastModifiedTime(long lastModifiedTime) {
        this.lastModifiedTime = lastModifiedTime;
    }

    public long getLastAccessTime() {
        return lastAccessTime;
    }

    public void setLastAccessTime(long lastAccessTime) {
        this.lastAccessTime = lastAccessTime;
    }

    public boolean isDirectory() {
        return directory;
    }

    public void setDirectory(boolean directory) {
        this.directory = directory;
    }

    public boolean isSymlink() {
        return symlink;
    }

    public void setSymlink(boolean symlink) {
        this.symlink = symlink;
    }
}
