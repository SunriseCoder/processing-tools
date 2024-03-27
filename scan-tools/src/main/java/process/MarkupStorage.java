package process;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;

import app.utils.JSONUtils;
import dto.Point;
import process.context.ApplicationContext;

public class MarkupStorage {
    private static final String MARKUP_STORAGE_FILENAME = "markups.json";

    private ApplicationContext applicationContext;

    private Map<String, FileEntry> storage;
    private File storageFile;

    public MarkupStorage(ApplicationContext applicationContext, File folder) {
        this.applicationContext = applicationContext;
        storageFile = new File(folder, MARKUP_STORAGE_FILENAME);
        storage = new TreeMap<>();
        loadData();
    }

    private void loadData() {
        if (!storageFile.exists()) {
            return;
        }

        try {
            TypeReference<List<FileEntry>> typeReference = new TypeReference<List<FileEntry>>() {};
            List<FileEntry> data = JSONUtils.loadFromDisk(storageFile, typeReference);
            storage.putAll(data.stream().collect(Collectors.toMap(e -> e.filename, e -> e)));
        } catch (Exception e) {
            if (applicationContext != null) {
                applicationContext.showError("Error due to load data from storage file " + storageFile.getAbsolutePath(), e);
            }
        }
    }

    public List<FileEntry> getAllBoundaries() {
        return new ArrayList<>(storage.values());
    }

    public List<Point> getSelectionBoundaries(String filename) {
        FileEntry entry = storage.get(filename);
        List<Point> result = entry == null ? null : entry.points;
        return result;
    }

    public void saveSelectionBoundaries(String filename, List<Point> points) {
        storage.put(filename, new FileEntry(filename, points, new Date()));
        saveStorageToDisk();
    }

    private void saveStorageToDisk() {
        try {
            JSONUtils.saveToDisk(storage.values(), storageFile);
        } catch (Exception e) {
            applicationContext.showError("Error due to save Markups to the disk", e);
        }
    }

    public static class FileEntry {
        public String filename;
        public List<Point> points;
        public Date date;

        public FileEntry() {
            // Default construction for deserialization
        }

        public FileEntry(String filename, List<Point> points, Date date) {
            this.filename = filename;
            this.points = points;
            this.date = date;
        }

        @Override
        public String toString() {
            return this.getClass().getSimpleName() + "[filename=" + filename +
                    ", points=" + points + ", date=" + date + "]";
        }
    }
}
