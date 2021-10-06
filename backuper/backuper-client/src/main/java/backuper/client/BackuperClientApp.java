package backuper.client;

import java.io.File;
import java.io.IOException;

import org.apache.hc.core5.http.HttpException;

import com.fasterxml.jackson.core.type.TypeReference;

import backuper.client.config.BackupTask;
import backuper.client.config.Configuration;
import backuper.client.config.CopySettings;
import backuper.client.config.RemoteResource;
import utils.JSONUtils;

public class BackuperClientApp {
    private static final String CONFIG_FILE = "config.json";

    public static void main(String[] args) throws IOException, HttpException {
        Configuration config = loadConfig();
        Backuper backuper = new Backuper(config);
        backuper.doBackup();
        System.out.println("Done");
    }

    private static Configuration loadConfig() {
        System.out.print("Loading configuration... ");

        Configuration config = null;

        File configFile = new File(CONFIG_FILE);
        if (configFile.exists()) {
            try {
                TypeReference<Configuration> typeReference = new TypeReference<Configuration>() {};
                config = JSONUtils.loadFromDisk(configFile, typeReference);
                System.out.println("done. Found: " + config.getBackupTasks().size() + " task(s)");
                postProcess(config);
            } catch (Exception e) {
                System.out.println("Error due to read tasks from file " + configFile.getAbsolutePath() + ", exiting");
                e.printStackTrace();
                System.exit(-1);
            }
        } else {
            System.out.println("Configuration file " + configFile.getAbsolutePath() + " not found, exiting");
            System.exit(-1);
        }

        return config;
    }

    private static void postProcess(Configuration config) throws IOException {
        bindCopySettingsWithBackupTasks(config);
    }

    private static void bindCopySettingsWithBackupTasks(Configuration config) throws IOException {
        for (BackupTask backupTask : config.getBackupTasks()) {
            RemoteResource remoteResource = RemoteResourceScanner.parseRemoteResource(backupTask.getSource());
            // Checking if the Source is Local or Remote Folder
            if (remoteResource == null) {
                backupTask.setCopySettings(config.getLocalCopySettings());
            } else {
                String serverUrl = remoteResource.getServerUrl();
                CopySettings copySettings = config.getRemoteCopySettings().get(serverUrl);

                // If Settings are not set yet, running the Benchmark and saving the Config
                if (copySettings == null) {
                    System.out.println("Settings for the server " + serverUrl + " not found, doing a benchmark. It will take a while...");
                    copySettings = BenchmarkRemote.doBenchmark(remoteResource);
                    config.getRemoteCopySettings().put(serverUrl, copySettings);
                    JSONUtils.saveToDisk(config, new File(CONFIG_FILE));
                }

                backupTask.setCopySettings(copySettings);
            }
        }
    }
}
