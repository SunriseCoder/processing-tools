package backuper.common.dto;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

public class FileMetadata {
    private String remoteResource;
    private String token;

    private String name;
    private Path absolutePath;
    private Path relativePath;

    private long size;

    private FileTime creationTime;
    private FileTime lastModifiedTime;
    private FileTime lastAccessTime;

    private boolean directory;
    private boolean symlink;
    private boolean remote;

    public FileMetadata(Path path, Path startPath) throws IOException {
        name = path.getFileName().toString();
        absolutePath = path;
        relativePath = startPath.relativize(path);

        BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
        size = attributes.size();

        creationTime = attributes.creationTime();
        lastModifiedTime = attributes.lastModifiedTime();
        lastAccessTime = attributes.lastAccessTime();

        directory = attributes.isDirectory();
        symlink = attributes.isSymbolicLink();
    }

    public FileMetadata(FileMetadataRemote remoteFileMetadata, String remoteResource, String token) {
        this.remoteResource = remoteResource;
        this.token = token;

        name = remoteFileMetadata.getName();
        relativePath = Paths.get(remoteFileMetadata.getPath());

        size = remoteFileMetadata.getSize();

        creationTime = FileTime.fromMillis(remoteFileMetadata.getCreationTime());
        lastModifiedTime = FileTime.fromMillis(remoteFileMetadata.getLastModifiedTime());
        lastAccessTime = FileTime.fromMillis(remoteFileMetadata.getLastAccessTime());

        directory = remoteFileMetadata.isDirectory();
        symlink = remoteFileMetadata.isSymlink();
        remote = true;
    }

    public String getRemoteResource() {
        return remoteResource;
    }

    public String getToken() {
        return token;
    }

    public String getName() {
        return name;
    }

    public Path getAbsolutePath() {
        return absolutePath;
    }

    public Path getRelativePath() {
        return relativePath;
    }

    public long getSize() {
        return size;
    }

    public FileTime getCreationTime() {
        return creationTime;
    }

    public FileTime getLastModified() {
        return lastModifiedTime;
    }

    public FileTime getLastAccessTime() {
        return lastAccessTime;
    }

    public boolean isDirectory() {
        return directory;
    }

    public boolean isSymlink() {
        return symlink;
    }

    public boolean isRemote() {
        return remote;
    }

    public boolean equalsRelatively(FileMetadata other) {
        return other != null && directory == other.directory && symlink == other.directory && name.equals(other.name) && relativePath.equals(other.relativePath)
                && size == other.size && lastModifiedTime.equals(other.lastModifiedTime);
    }
}
