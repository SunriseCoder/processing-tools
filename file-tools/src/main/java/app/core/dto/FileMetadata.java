package app.core.dto;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
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

import app.collection.RMap;
import app.collection.RMapEntry;
import app.collection.wrap.RMapWrap;
import app.json.serial.FileTimeJsonDeserializer;
import app.json.serial.FileTimeJsonSerializer;

// TODO Implement Listener Pattern
public class FileMetadata {
    private static final Logger LOGGER = LogManager.getLogger(FileMetadata.class);

    private String name;
    private String absolutePath;
    private long size;

    @JsonSerialize(using = FileTimeJsonSerializer.class)
    @JsonDeserialize(using = FileTimeJsonDeserializer.class)
    private FileTime lastModifiedTime;

    private Map<String, String> checksums;

    // TODO Try to remove this field
    @JsonIgnore
    private boolean existsOnDiskNow;
    // TODO Try to remove this field
    @JsonIgnore
    private boolean readOnly;

    // TODO Remove this field, replace with Listener pattern
    @JsonIgnore
    private FileDatabase fileDatabase;

    public FileMetadata() {
        checksums = new HashMap<>();
    }

    public FileMetadata(Path path) throws IOException {
        this();
        name = path.getFileName().toString();
        absolutePath = path.toAbsolutePath().toString();

        // TODO Use NoFollowLinks in readAttributes or better to move to PathUtils
        BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
        size = attributes.size();

        lastModifiedTime = attributes.lastModifiedTime();
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

    public FileTime getLastModifiedTime() {
        return lastModifiedTime;
    }

    public RMap<String, String> getChecksums() {
        return new RMapWrap<>(checksums);
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

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public void setFileDatabase(FileDatabase fileDatabase) {
        this.fileDatabase = fileDatabase;
    }

    // TODO Move equalsByXXX Methods to Util class
    public boolean equalsByChecksum(FileMetadata other) {
        if (other == null) {
            LOGGER.trace("equalsByChecksum of " + this + " against " + other + " is false due to other == null");
            return false;
        }

        boolean result = equalsByChecksum(other.getChecksums());
        return result;
    }

    public boolean equalsByChecksum(SnapshotFile other) {
        if (other == null) {
            LOGGER.trace("equalsByChecksum of " + this + " against " + other + " is false due to other == null");
            return false;
        }

        boolean result = equalsByChecksum(other.getChecksums());
        return result;
    }

    public boolean equalsByChecksum(RMap<String, String> otherChecksums) {
        if ((checksums == null || checksums.isEmpty()) && (otherChecksums == null || otherChecksums.isEmpty())) {
            LOGGER.trace("equalsByChecksum of " + checksums + " against " + otherChecksums
                    + " is false due to (this or other).checksums == (null or isEmpty)");
            return false;
        }

        for (Entry<String, String> thisChecksumEntry : checksums.entrySet()) {
            if (thisChecksumEntry.getValue() == null || thisChecksumEntry.getValue().isEmpty()
                    || !thisChecksumEntry.getValue().equals(otherChecksums.get(thisChecksumEntry.getKey()))) {
                LOGGER.trace("equalsByChecksum of " + checksums + " against " + otherChecksums
                        + " is false due to this.checksums[" + thisChecksumEntry.getKey() + "].value is null or empty"
                        + " or not equals to others.checksums[" + thisChecksumEntry.getKey() + "].value");
                return false;
            }
        }

        for (RMapEntry<String, String> otherChecksumEntry : otherChecksums.entryRSet()) {
            if (otherChecksumEntry.getValue() == null || otherChecksumEntry.getValue().isEmpty()
                    || !otherChecksumEntry.getValue().equals(checksums.get(otherChecksumEntry.getKey()))) {
                LOGGER.trace("equalsByChecksum of " + checksums + " against " + otherChecksums
                        + " is false due to other.checksums[" + otherChecksumEntry.getKey() + "].value is null or empty"
                        + " or not equals to this.checksums[" + otherChecksumEntry.getKey() + "].value");
                return false;
            }
        }

        LOGGER.trace("equalsByChecksum of " + checksums + " against " + otherChecksums + " is true");
        return true;
    }

    // TODO Review where this method is used and revise, maybe some of those methods should NOT use it
    public boolean equalsByMetadata(FileMetadata other) {
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
        return "[absolutePath=" + absolutePath + ",size=" + size + ",lastModifiedTime=" + lastModifiedTime + "]";
    }

    @Override
    public String toString() {
        return this.getClass().getName() + "[name=" + name
                + ",absolutePath=" + absolutePath
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
        this.checksums.put(algorithm, checksum);
        setChanged();
    }

    public void addChecksums(Map<String, String> checksums) {
        this.checksums.putAll(checksums);
        setChanged();
    }

    private void setChanged() {
        fileDatabase.setChanged();
    }
}
