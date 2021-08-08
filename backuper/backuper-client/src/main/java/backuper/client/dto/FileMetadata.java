package backuper.client.dto;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

public class FileMetadata {
    private String name;
    private Path absolutePath;
    private Path relativePath;

    private long size;

    private FileTime creationTime;
    private FileTime lastModifiedTime;
    private FileTime lastAccessTime;

    private boolean directory;
    private boolean symlink;

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

    public boolean equalsRelatively(FileMetadata other) {
        return other != null && directory == other.directory && symlink == other.directory && name.equals(other.name) && relativePath.equals(other.relativePath)
                && size == other.size && lastModifiedTime.equals(other.lastModifiedTime);
    }
}
