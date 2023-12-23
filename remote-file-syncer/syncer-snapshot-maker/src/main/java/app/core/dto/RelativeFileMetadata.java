package app.core.dto;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import json.serial.FileTimeJsonDeserializer;
import json.serial.FileTimeJsonSerializer;

public class RelativeFileMetadata {
    private static final Logger LOGGER = LogManager.getLogger(RelativeFileMetadata.class);

    private String name;
    private String relativePath;
    private long size;
    private Map<String, String> checksums;

    @JsonSerialize(using = FileTimeJsonSerializer.class)
    @JsonDeserialize(using = FileTimeJsonDeserializer.class)
    private FileTime lastModifiedTime;

    @JsonIgnore
    private boolean existsOnDiskNow;

    public RelativeFileMetadata() {
        checksums = new HashMap<>();
    }

    public RelativeFileMetadata(Path path, Path startPath) throws IOException {
        this();
        name = path.getFileName().toString();
        relativePath = startPath.relativize(path).toString();

        BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
        size = attributes.size();

        lastModifiedTime = attributes.lastModifiedTime();
    }

    public String getName() {
        return name;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public long getSize() {
        return size;
    }

    public Map<String, String> getChecksums() {
        return checksums;
    }

    @JsonIgnore
    public FileTime getLastModified() {
        return lastModifiedTime;
    }

    public boolean isExistsOnDiskNow() {
        return existsOnDiskNow;
    }

    public void setExistsOnDiskNow(boolean existsOnDiskNow) {
        this.existsOnDiskNow = existsOnDiskNow;
    }

    // TODO Revise
    public boolean equalsByChecksum(RelativeFileMetadata other) {
        return other != null;
        // TODO Implement Checksum comparison (all checksums, not sha512 only)
        // && sha512.equals(other.sha512);
    }

    public boolean equalsByMetadata(RelativeFileMetadata other) {
        boolean result = other != null && size == other.size && lastModifiedTime.equals(other.lastModifiedTime);
        if (!result && LOGGER.getLevel().isMoreSpecificThan(Level.TRACE)) {
            StringBuilder sb = new StringBuilder();
            sb.append("Method equalsByMetadata considered following objects are non-equals: ")
                    .append("This: " + this)
                    .append("Other: " + (other == null ? "null" : other));
            LOGGER.trace(sb.toString());
        }
        return result;
    }

    public String toStringShort() {
        return "[relativePath=" + relativePath + ",size=" + size + ",lastModifiedTime=" + lastModifiedTime + "]";
    }

    @Override
    public String toString() {
        return this.getClass().getName() + "[name=" + name
                + ",relativePath=" + relativePath
                + ",size=" + size
                + ",lastModifiedTime=" + lastModifiedTime
                + ",checksums=" + checksums
        + "]";
    }

    public boolean hasChecksums(String[] checksumAlgorithms) {
        for (String algorithm : checksumAlgorithms) {
            if (!checksums.containsKey(algorithm)) {
                return false;
            }
        }

        return true;
    }

    public void addChecksum(String algorithm, String checksum) {
        checksums.put(algorithm, checksum);
    }
}
