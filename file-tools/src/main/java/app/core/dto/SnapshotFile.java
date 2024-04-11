package app.core.dto;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import app.collection.RList;
import app.collection.RMap;
import app.collection.RMapEntry;
import app.collection.wrap.RListWrap;
import app.collection.wrap.RMapWrap;
import app.json.serial.FileTimeJsonDeserializer;
import app.json.serial.FileTimeJsonSerializer;
import app.pattern.listener.ChangedListenable;
import app.pattern.listener.ChangedListener;

public class SnapshotFile implements ChangedListenable {
    private static final Logger LOGGER = LogManager.getLogger(SnapshotFile.class);

    private String name;
    private String path;
    private long size;
    private Map<String, String> checksums;

    @JsonSerialize(using = FileTimeJsonSerializer.class)
    @JsonDeserialize(using = FileTimeJsonDeserializer.class)
    private FileTime lastModifiedTime;

    // TODO Get rid of this field, move it where it is used, because it has nothing to do with this class
    @JsonIgnore
    private boolean existsOnDiskNow;

    private Status status;
    private List<String> statusMessages;

    @JsonIgnore
    private Set<ChangedListener> changedListeners;

    public SnapshotFile() {
        checksums = new HashMap<>();
        statusMessages = new ArrayList<>();
        changedListeners = new HashSet<>();
    }

    // TODO Move readAttributes to factory method or util method
    public SnapshotFile(Path absoluteFilePath, Path startPath) {
        this();

        try {
            name = absoluteFilePath.getFileName().toString();
            path = startPath.relativize(absoluteFilePath).toString();

            // TODO Use NO_FOLLOW_LINKS, Extract to static Factory Method
            BasicFileAttributes attributes = Files.readAttributes(absoluteFilePath, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
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
        return path;
    }

    public long getSize() {
        return size;
    }

    // TODO Wrap collection with ReadOnlyWrapper, which will not let any modification of collection from outside (Demetra's Law)
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

    // TODO Revise all the equalsXXX methods
    public boolean equalsByMetadata(SnapshotFile other) {
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

    public boolean equalsByMetadata(FileMetadata other) {
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

    public boolean equalsByChecksum(SnapshotFile other) {
        if (other == null) {
            LOGGER.trace("equalsByChecksum of " + this + " against " + other + " is false due to other == null");
            return false;
        }

        boolean result = equalsByChecksum(other.getChecksums());
        return result;
    }

    public boolean equalsByChecksum(FileMetadata other) {
        if (other == null) {
            LOGGER.trace("equalsByChecksum of " + this + " against " + other + " is false due to other == null");
            return false;
        }

        boolean result = equalsByChecksum(other.getChecksums());
        return result;
    }

    private boolean equalsByChecksum(RMap<String, String> otherChecksums) {
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
        for (RMapEntry<String, String> otherChecksumEntry : otherChecksums.entryRSet()) {
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
        return "[relativePath=" + path + ",size=" + size + ",lastModifiedTime=" + lastModifiedTime + "]";
    }

    @Override
    public String toString() {
        return this.getClass().getName() + "[name=" + name
                + ",relativePath=" + path
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
        fireChanged();
    }

    public void addAllChecksums(Map<String, String> checksums) {
        this.checksums.putAll(checksums);
        fireChanged();
    }

    public Status getStatus() {
        return status;
    }

    // TODO Wrap collection with ReadOnlyWrapper, which will not let any modification of collection from outside (Demetra's Law)
    public RList<String> getStatusMessages() {
        return new RListWrap<>(statusMessages);
    }

    public void setReadContentError(String errorMessage) {
        status = Status.ReadContentError;
        statusMessages.add(errorMessage);
        fireChanged();
    }

    @Override
    public void addChangedListener(ChangedListener listener) {
        changedListeners.add(listener);
    }

    @Override
    public void fireChanged() {
        changedListeners.forEach(l -> l.fireChanged());
    }

    @Override
    public void removeChangedListener(ChangedListener listener) {
        changedListeners.remove(listener);
    }

    public static enum Status {
        Ok, ReadMetadataError, ReadContentError
    }
}
