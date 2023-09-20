package app.core.dto.fs;

import java.util.HashMap;
import java.util.Map;

public class FileSystemFile extends FileSystemElement {
    private long size;
    private Map<String, String> checksums;
    private boolean checked;

    public FileSystemFile() {
        checksums = new HashMap<>();
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public Map<String, String> getChecksums() {
        return checksums;
    }

    public void setChecksum(String algorithm, String checksum) {
        checksums.put(algorithm, checksum);
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }
}
