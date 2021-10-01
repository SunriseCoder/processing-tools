package backuper.client;

import java.io.File;
import java.io.IOException;

import org.apache.hc.core5.http.HttpException;

import com.fasterxml.jackson.core.type.TypeReference;

import backuper.client.config.Configuration;
import utils.JSONUtils;

public class BackuperClientApp {

    public static void main(String[] args) throws IOException, HttpException {
        Configuration config = loadConfig();
        Backuper backuper = new Backuper(config);
        backuper.doBackup();
    }

    private static Configuration loadConfig() {
        System.out.print("Loading configuration... ");

        Configuration config = null;

        File configFile = new File("config.json");
        if (configFile.exists()) {
            try {
                TypeReference<Configuration> typeReference = new TypeReference<Configuration>() {};
                config = JSONUtils.loadFromDisk(configFile, typeReference);
                System.out.println("Found: " + config.getBackupTasks().size() + " task(s)");
            } catch (Exception e) {
                System.out.println("Error due to read tasks from file " + configFile.getAbsolutePath() + ", exiting");
                e.printStackTrace();
                System.exit(-1);
            }
        } else {
            System.out.println("Configuration file " + configFile.getAbsolutePath() + " not found, exiting");
            System.exit(-1);
        }

        System.out.println("done");

        return config;
    }
}
