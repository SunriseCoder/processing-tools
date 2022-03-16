package process.processing.merge;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Future;

import filters.FilenameFilterImages;
import multithreading.AbstractManagerTask;
import utils.FileUtils;
import utils.NumberUtils;

public class MergeManagerTask extends AbstractManagerTask {
    private int remainder;

    public MergeManagerTask(String name) {
        super(name);
    }

    @Override
    protected void runWithExceptions() throws Exception {
        File sourceFolder = applicationContext.getWorkFolder();
        File outputFolder = new File(sourceFolder, "merged");
        outputFolder.mkdirs();

        File[] files = sourceFolder.listFiles(new FilenameFilterImages(false));
        int amountOfImages = files.length;
        if (amountOfImages == 0) {
            applicationContext.showWarning("There is no Images to Merge", null);
            return;
        }

        String fileNameBase = createFileNameBase(files[0].getName());
        List<Future<?>> taskFutures = new ArrayList<>();

        int taskCounter = 0;
        File previousImageFile = null;
        for (int i = 0; i < amountOfImages; i++) {
            File sourceFile = files[i];
            if (i % 2 == remainder) {
                String outputFileName = fileNameBase + NumberUtils.generateNumberByLength(taskCounter + 1, 4) + ".png";
                String taskName = "Merge Image: " + outputFileName;
                MergeTask subTask = new MergeTask(taskName);

                subTask.setApplicationContext(applicationContext);
                subTask.setSourceFile1(previousImageFile);
                subTask.setSourceFile2(sourceFile);
                File outputFile = new File(outputFolder, outputFileName);
                subTask.setOutputFile(outputFile);

                Future<?> future = applicationContext.submitTask(subTask);
                taskFutures.add(future);
                taskCounter++;

                previousImageFile = null;
            } else {
                previousImageFile = sourceFile;
            }

        }

        if (previousImageFile != null) {
            String outputFileName = fileNameBase + NumberUtils.generateNumberByLength(taskCounter + 1, 4) + ".png";
            String taskName = "Merge Image: " + outputFileName;
            MergeTask subTask = new MergeTask(taskName);

            subTask.setApplicationContext(applicationContext);
            subTask.setSourceFile1(previousImageFile);
            subTask.setSourceFile2(null);
            File outputFile = new File(outputFolder, outputFileName);
            subTask.setOutputFile(outputFile);

            Future<?> future = applicationContext.submitTask(subTask);
            taskFutures.add(future);
            taskCounter++;

            previousImageFile = null;
        }

        int processedImagesCounter = 0;
        applicationContext.updateProgress(progressBar, 0);
        while (processedImagesCounter < taskCounter) {
            Iterator<Future<?>> iterator = taskFutures.iterator();
            while (iterator.hasNext()) {
                Future<?> taskFuture = iterator.next();
                if (taskFuture.isDone()) {
                    iterator.remove();
                    processedImagesCounter++;
                    double progress = (double) processedImagesCounter / taskCounter;
                    applicationContext.updateProgress(progressBar, progress);
                }
            }

            Thread.sleep(100);
        }
    }

    private String createFileNameBase(String fileName) {
        String base = FileUtils.getFileName(fileName);
        base = base.replaceAll("[0-9]+$", "");
        return base;
    }

    public void setRemainder(int remainder) {
        this.remainder = remainder;
    }
}
