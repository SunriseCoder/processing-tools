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

    public String getPath() {
        return path;
    }

    public long getSize() {
        return size;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public long getLastModifiedTime() {
        return lastModifiedTime;
    }

    public long getLastAccessTime() {
        return lastAccessTime;
    }

    public boolean isDirectory() {
        return directory;
    }

    public boolean isSymlink() {
        return symlink;
    }
}
