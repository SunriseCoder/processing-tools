package backuper.client;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.hc.core5.http.HttpException;

import com.fasterxml.jackson.core.type.TypeReference;

import backuper.client.dto.BackupTask;
import utils.JSONUtils;

public class BackuperClientApp {

    public static void main(String[] args) throws IOException, HttpException {
        System.out.println("Loading backup tasks...");

        List<BackupTask> tasks = null;
        File tasksFile = new File("tasks.json");
        if (tasksFile.exists()) {
            try {
                TypeReference<List<BackupTask>> typeReference = new TypeReference<List<BackupTask>>() {};
                tasks = JSONUtils.loadFromDisk(tasksFile, typeReference);
                System.out.println("Found: " + tasks.size() + " task(s)");
            } catch (Exception e) {
                System.out.println("Error due to read tasks from file " + tasksFile.getAbsolutePath() + ", exiting");
                e.printStackTrace();
                System.exit(-1);
            }
        } else {
            System.out.println("Task file " + tasksFile.getAbsolutePath() + " not found, exiting");
            System.exit(-1);
        }

        Backuper backuper = new Backuper();
        backuper.doBackupTasks(tasks);
    }
}
