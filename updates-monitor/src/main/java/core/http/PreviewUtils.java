package core.http;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;

import core.dto.VideoPreview;
import utils.JSONUtils;

public class PreviewUtils {

    public static void setProcessed(String id) throws IOException {
        TypeReference<List<VideoPreview>> typeReference = new TypeReference<List<VideoPreview>>() {};
        File databaseFile = new File("database/previews.json");
        List<VideoPreview> previews = databaseFile.exists()
                ? JSONUtils.loadFromDisk(databaseFile, typeReference) : new ArrayList<>();

        for (VideoPreview preview : previews) {
            if (preview.getVideoId().equals(id)) {
                preview.setProcessed(true);
                break;
            }
        }

        JSONUtils.saveToDisk(previews, databaseFile);
    }

    public static void addToDelete(String id) throws IOException {
        TypeReference<List<String>> typeReference = new TypeReference<List<String>>() {};
        File databaseFile = new File("database/to-delete.json");
        List<String> toDeleteIds = databaseFile.exists()
                ? JSONUtils.loadFromDisk(databaseFile, typeReference) : new ArrayList<>();

        for (String videoId : toDeleteIds) {
            if (videoId.equals(id)) {
                return;
            }
        }
        toDeleteIds.add(id);

        JSONUtils.saveToDisk(toDeleteIds, databaseFile);
    }
}
