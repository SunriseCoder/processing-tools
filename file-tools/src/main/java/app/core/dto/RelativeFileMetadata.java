package app.core.dto;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import app.json.serial.FileTimeJsonDeserializer;
import app.json.serial.FileTimeJsonSerializer;

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

    private Status status;
    private List<String> statusMessages;

    public RelativeFileMetadata() {
        checksums = new HashMap<>();
        statusMessages = new ArrayList<>();
    }

    public RelativeFileMetadata(Path path, Path startPath) {
        this();

        try {
            name = path.getFileName().toString();
            relativePath = startPath.relativize(path).toString();

            BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
            size = attributes.size();

            lastModifiedTime = attributes.lastModifiedTime();
            status = Status.Ok;
        } catch (IOException e) {
            statusMessages.add(e.getMessage() + ": " + path.toString());
            status = Status.ReadMetadataError;
            LOGGER.error("Error due to create " + getClass().getName() + ": " + path.toString(), e);
        }
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

    // TODO Revise all the equalsXXX methods
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

    public boolean equalsByMetadata(AbsoluteFileMetadata other) {
        boolean result = other != null && size == other.getSize() && lastModifiedTime.equals(other.getLastModifiedTime());
        if (!result && LOGGER.getLevel().isMoreSpecificThan(Level.TRACE)) {
            StringBuilder sb = new StringBuilder();
            sb.append("Method equalsByMetadata considered following objects are non-equals: ")
                    .append("This: " + this)
                    .append("Other: " + (other == null ? "null" : other));
            LOGGER.trace(sb.toString());
        }
        return result;
    }

    public boolean equalsByChecksum(RelativeFileMetadata other) {
        if (other == null) {
            LOGGER.trace("equalsByChecksum of " + this + " against " + other + " is false due to other == null");
            return false;
        }

        boolean result = equalsByChecksum(other.getChecksums());
        return result;
    }

    public boolean equalsByChecksum(AbsoluteFileMetadata other) {
        if (other == null) {
            LOGGER.trace("equalsByChecksum of " + this + " against " + other + " is false due to other == null");
            return false;
        }

        boolean result = equalsByChecksum(other.getChecksums());
        return result;
    }

    private boolean equalsByChecksum(Map<String, String> otherChecksums) {
        // Checking that Both Checksums are not Empty
        if ((checksums == null || checksums.isEmpty()) && (otherChecksums == null || otherChecksums.isEmpty())) {
            LOGGER.trace("equalsByChecksum of " + checksums + " against " + otherChecksums
                    + " is false due to (this or other).checksums == (null or isEmpty)");
            return false;
        }

        // Loop over this.checksums against other.checksums
        for (Entry<String, String> thisChecksumEntry : checksums.entrySet()) {
            if (thisChecksumEntry.getValue() == null || thisChecksumEntry.getValue().isEmpty()
                    || !thisChecksumEntry.getValue().equals(otherChecksums.get(thisChecksumEntry.getKey()))) {
                LOGGER.trace("equalsByChecksum of " + checksums + " against " + otherChecksums
                        + " is false due to this.checksums[i].value is null or empty or not equals to others.checksums[i].value");
                return false;
            }
        }

        // Loop over other.checksums against this.checksums
        for (Entry<String, String> otherChecksumEntry : otherChecksums.entrySet()) {
            if (otherChecksumEntry.getValue() == null || otherChecksumEntry.getValue().isEmpty()
                    || !otherChecksumEntry.getValue().equals(checksums.get(otherChecksumEntry.getKey()))) {
                LOGGER.trace("equalsByChecksum of " + checksums + " against " + otherChecksums
                        + " is false due to other.checksums[i].value is null or empty or not equals to this.checksums[i].value");
                return false;
            }
        }

        LOGGER.trace("equalsByChecksum of " + checksums + " against " + otherChecksums + " is true");
        return true;
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

    public boolean hasChecksums(List<String> checksumAlgorithms) {
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

    public void addAllChecksums(Map<String, String> checksums) {
        this.checksums.putAll(checksums);
    }

    public Status getStatus() {
        return status;
    }

    public List<String> getStatusMessages() {
        return statusMessages;
    }

    public void setReadContentError(String errorMessage) {
        statusMessages.add(errorMessage);
        status = Status.ReadContentError;
    }

    public static enum Status {
        Ok, ReadMetadataError, ReadContentError
    }
}
