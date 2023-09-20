package app.config;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;

import utils.JSONUtils;

public class Configuration {
    private static Map<String, String> configuration;

    public enum Keys {
        HddPath, HddTmpPath,
        MinFreeSpace,
        DbSaveIntervalInSeconds,
        MinFileSize, MaxFileSize,
        MaxFolderDepth
    };

    static {
        loadFromFile();
    }

    private static void loadFromFile() {
        File configFile = new File("configuration.json");
        if (configFile.exists()) {
            try {
                TypeReference<Map<String, String>> typeReference = new TypeReference<Map<String, String>>() {};
                configuration = JSONUtils.loadFromDisk(configFile, typeReference);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static String getValue(Keys key) {
        return configuration.get(key.name());
    }

    public synchronized static void reload() {
        loadFromFile();
    }
}
