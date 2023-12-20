package app.core.dto;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import json.serial.FileTimeJsonDeserializer;
import json.serial.FileTimeJsonSerializer;

public class AbsoluteFileMetadata {
    private static final boolean DEBUG = false;

    private String name;
    private String absolutePath;

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

    public AbsoluteFileMetadata() {
        // Default constructor for JSON Deserialization
    }

    public AbsoluteFileMetadata(Path path, Path startPath) throws Exception {
        name = path.getFileName().toString();
        absolutePath = path.toAbsolutePath().toString();

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

    public String getAbsolutePath() {
        return absolutePath;
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

    // TODO Revise
    public boolean equalsByChecksum(AbsoluteFileMetadata other) {
        return other != null && directory == other.directory && symlink == other.symlink
                && sha512.equals(other.sha512);
    }

    // TODO Revise
    public boolean equalsByMetadata(AbsoluteFileMetadata other) {
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
        return absolutePath + " " + size + " lastModifiedTime=" + lastModifiedTime;
    }

    @Override
    public String toString() {
        return this.getClass().getName() + "[name=" + name + ",absolutePath=" + absolutePath
                + ",size=" + size + ",sha512=" + sha512
                + ",creationTime=" + creationTime + ",lastModifiedTime=" + lastModifiedTime
                + ",lastAccessTime=" + lastAccessTime
                + ",directory=" + directory + ",symlink=" + symlink
        + "]";
    }
}
