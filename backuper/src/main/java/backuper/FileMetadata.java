package backuper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

public class FileMetadata {
    private static final String FILE_ATTRIBUTE_LAST_MODIFIED_TIME = "lastModifiedTime";

	private String name;
    private String path;
    private String relativePath;
    private long size;
    private FileTime lastModified;

    private boolean directory;
    private boolean symlink;

    public FileMetadata(Path path, String relativePath) throws IOException {
        this.name = path.getFileName().toString();
        this.path = path.toString();
        this.relativePath = relativePath;

        // TODO Combine getting all the attributes of the file to the single filesystem read operation
        //Map<String, Object> attributes = Files.readAttributes(path, "!!!attributes-here!!!");
        //this.size = Files.size(path);
        //this.lastModified = (FileTime) attributes.get(FILE_ATTRIBUTE_LAST_MODIFIED_TIME);

        this.directory = Files.isDirectory(path);
        this.symlink = Files.isSymbolicLink(path);
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public long getSize() {
        return size;
    }

    public FileTime getLastModified() {
		return lastModified;
	}

    public boolean isDirectory() {
		return directory;
	}

    public boolean isSymlink() {
		return symlink;
	}
}
