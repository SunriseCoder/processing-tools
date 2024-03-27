package backuper.common.dto;

import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FileChecksums {
    private String filename;
    private long size;
    private FileTime lastModifiedTime;
    private String fileChecksum;
    private Map<Integer, List<String>> chunkChecksums;

    public FileChecksums() {
        chunkChecksums = new HashMap<>();
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public FileTime getLastModifiedTime() {
        return lastModifiedTime;
    }

    public void setLastModifiedTime(FileTime lastModifiedTime) {
        this.lastModifiedTime = lastModifiedTime;
    }

    public String getFileChecksum() {
        return fileChecksum;
    }

    public void setFileChecksum(String fileChecksum) {
        this.fileChecksum = fileChecksum;
    }

    public Set<Integer> getChunkSizes() {
        return chunkChecksums.keySet();
    }

    public List<String> getChunkChecksum(int chunkSize) {
        return chunkChecksums.get(chunkSize);
    }

    public void addChunkChecksums(int chunkSize, List<String> chunkChecksums) {
        this.chunkChecksums.put(chunkSize, chunkChecksums);
    }

    public void addChunkChecksum(int chunkSize, String checksum) {
        List<String> chunkList = chunkChecksums.get(chunkSize);
        if (chunkList == null) {
            chunkList = new ArrayList<>();
            chunkChecksums.put(chunkSize, chunkList);
        }
        chunkList.add(checksum);
    }
}
