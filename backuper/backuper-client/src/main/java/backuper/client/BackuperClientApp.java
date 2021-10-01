package backuper.client;

import java.io.File;
import java.io.IOException;

import org.apache.hc.core5.http.HttpException;

import com.fasterxml.jackson.core.type.TypeReference;

import backuper.client.dto.Configuration;
import utils.JSONUtils;

public class BackuperClientApp {

    public static void main(String[] args) throws IOException, HttpException {
        Configuration configuration = loadConfiguration();
        Backuper backuper = new Backuper(configuration);
        backuper.doBackup();
    }

    private static Configuration loadConfiguration() {
        System.out.print("Loading configuration... ");

        Configuration configuration = null;

        File configurationFile = new File("configuration.json");
        if (configurationFile.exists()) {
            try {
                TypeReference<Configuration> typeReference = new TypeReference<Configuration>() {};
                configuration = JSONUtils.loadFromDisk(configurationFile, typeReference);
                System.out.println("Found: " + configuration.getBackupTasks().size() + " task(s)");
            } catch (Exception e) {
                System.out.println("Error due to read tasks from file " + configurationFile.getAbsolutePath() + ", exiting");
                e.printStackTrace();
                System.exit(-1);
            }
        } else {
            System.out.println("Configuration file " + configurationFile.getAbsolutePath() + " not found, exiting");
            System.exit(-1);
        }

        System.out.println("done");

        return configuration;
    }
}
