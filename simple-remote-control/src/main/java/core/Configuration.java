package core;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;

import core.dto.Command;
import utils.JSONUtils;

public class Configuration {
    private static Map<String, String> parameters;
    private static Map<String, Command> commands;

    static {
        loadFromFile();
    }

    private static void loadFromFile() {
        File configFile = new File("parameters.json");
        if (configFile.exists()) {
            try {
                TypeReference<Map<String, String>> typeReference = new TypeReference<Map<String, String>>() {};
                parameters = JSONUtils.loadFromDisk(configFile, typeReference);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        configFile = new File("commands.json");
        if (configFile.exists()) {
            try {
                TypeReference<Map<String, Command>> typeReference = new TypeReference<Map<String, Command>>() {};
                commands = JSONUtils.loadFromDisk(configFile, typeReference);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized static void reload() {
        loadFromFile();
    }

    public static String getParameter(String name) {
        return parameters.get(name);
    }

    public static Map<String, Command> getCommands() {
        return commands;
    }

    public static Command getCommand(String name) {
        return commands.get(name);
    }
}
