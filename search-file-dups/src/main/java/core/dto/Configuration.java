package core.dto;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;

import utils.JSONUtils;

public class Configuration {
    private static Map<String, String> configuration;

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

    public static String getValue(String key) {
        return configuration.get(key);
    }

    public static String getYoutubeCookies() {
        return configuration.get("youtubeCookie");
    }

    public synchronized static void reload() {
        loadFromFile();
    }
}
