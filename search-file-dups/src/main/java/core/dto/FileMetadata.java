package core.dto;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import core.serial.FileTimeJsonDeserializer;
import core.serial.FileTimeJsonSerializer;

public class FileMetadata {
    private static final boolean DEBUG = false;

    @JsonIgnore
    private Database database;

    private String name;
    private String absolutePath;
    private String relativePath;

    private long size;
    private String sha512;

    @JsonSerialize(using = FileTimeJsonSerializer.class)
    @JsonDeserialize(using = FileTimeJsonDeserializer.class)
    private FileTime creationTime;
    @JsonSerialize(using = FileTimeJsonSerializer.class)
    @JsonDeserialize(using = FileTimeJsonDeserializer.class)
    private FileTime lastModifiedTime;
    @JsonSerialize(using = FileTimeJsonSerializer.class)
    @JsonDeserialize(using = FileTimeJsonDeserializer.class)
    private FileTime lastAccessTime;

    private boolean directory;
    private boolean symlink;

    public FileMetadata() {
        // Default constructor for JSON Deserialization
    }

    public FileMetadata(Path path, Path startPath) throws Exception {
        name = path.getFileName().toString();
        absolutePath = path.toAbsolutePath().toString();
        relativePath = startPath.relativize(path).toString();

        BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
        size = attributes.size();

        creationTime = attributes.creationTime();
        lastModifiedTime = attributes.lastModifiedTime();
        lastAccessTime = attributes.lastAccessTime();

        directory = attributes.isDirectory();
        symlink = attributes.isSymbolicLink();
    }

    public Database getDatabase() {
        return database;
    }

    public void setDatabase(Database database) {
        this.database = database;
    }

    public String getName() {
        return name;
    }

    public String getAbsolutePath() {
        return absolutePath;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public long getSize() {
        return size;
    }

    public String getSha512() {
        return sha512;
    }

    public void setSha512(String sha512) {
        this.sha512 = sha512;
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

    public boolean equalsByChecksum(FileMetadata other) {
        return other != null && directory == other.directory && symlink == other.symlink
                && sha512.equals(other.sha512);
    }

    public boolean equalsByMetadata(FileMetadata other) {
        boolean result = other != null && directory == other.directory && symlink == other.symlink
                && size == other.size && lastModifiedTime.equals(other.lastModifiedTime);
        if (DEBUG) {
            if (result) {
            } else {
                System.out.println("Method equalsByMetadata considered following objects are non-equals:");
                System.out.println("This: " + this);
                System.out.println("Other: " + (other == null ? "null" : other));
            }
        }
        return result;
    }

    public String toStringShort() {
        return relativePath + " " + size + " lastModifiedTime=" + lastModifiedTime;
    }

    @Override
    public String toString() {
        return this.getClass().getName() + "[name=" + name + ",absolutePath=" + absolutePath
                + ",relativePath=" + relativePath + ",size=" + size + ",sha512=" + sha512
                + ",creationTime=" + creationTime + ",lastModifiedTime=" + lastModifiedTime
                + ",lastAccessTime=" + lastAccessTime
                + ",directory=" + directory + ",symlink=" + symlink
        + "]";
    }
}
