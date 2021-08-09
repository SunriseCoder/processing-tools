package backuper.server;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import backuper.common.FolderScanner;
import backuper.common.dto.FileMetadata;
import backuper.common.dto.FileMetadataRemote;
import backuper.server.config.Configuration;
import backuper.server.config.Resource;
import backuper.server.config.User;

public class FileServer {
    private Configuration config;
    private FolderScanner folderScanner;

    public FileServer(Configuration config) {
        this.config = config;
        folderScanner = new FolderScanner();
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
}
