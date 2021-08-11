package backuper.server;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import backuper.common.LocalFolderScanner;
import backuper.common.dto.FileMetadata;
import backuper.common.dto.FileMetadataRemote;
import backuper.server.config.Configuration;
import backuper.server.config.Resource;
import backuper.server.config.User;

public class FileServer {
    private Configuration config;
    private LocalFolderScanner folderScanner;

    public FileServer(Configuration config) {
        this.config = config;
        folderScanner = new LocalFolderScanner();
    }

    public boolean hasResource(String resource) {
        return config.getResourceByName(resource) != null;
    }

    public boolean hasToken(String token) {
        return config.getUserByToken(token) != null;
    }

    public User getUserByToken(String token) {
        return config.getUserByToken(token);
    }

    public List<FileMetadataRemote> getFileList(String resourceName, String token) throws IOException {
        User user = config.getUserByToken(token);
        if (user == null || !user.getPermissions().contains(resourceName)) {
            return null;
        }

        Resource resource = config.getResourceByName(resourceName);
        Map<String, FileMetadata> foldersMap = folderScanner.scan(Paths.get(resource.getPath()));
        List<FileMetadataRemote> foldersList = foldersMap.values().stream()
                .map(fileMetadata -> new FileMetadataRemote(fileMetadata)).collect(Collectors.toList());
        return foldersList;
    }

    public boolean hasAccess(String resource, String token) {
        User user = config.getUserByToken(token);
        return user.getPermissions().contains(resource);
    }

    public byte[] getFileData(String resourceName, String relativePath, long start, int length) throws IOException {
        Resource resource = config.getResourceByName(resourceName);
        Path path = Paths.get(resource.getPath(), relativePath);

        long fileSize = Files.size(path);
        if (length > fileSize - start) {
            length = (int) (fileSize - start);
        }

        ByteBuffer byteBuffer = ByteBuffer.allocate(length);
        try (RandomAccessFile inputFile = new RandomAccessFile(path.toString(), "r")) {
            FileChannel in = inputFile.getChannel();
            in.position(start);
            in.read(byteBuffer);
        }

        return byteBuffer.array();
    }
}
