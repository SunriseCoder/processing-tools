package process.processing.automarkup;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Future;

import filters.FilenameFilterImages;
import multithreading.AbstractManagerTask;

public class AutoMarkupManagerTask extends AbstractManagerTask {
    private int threshold;
    private int areaSize;

    public AutoMarkupManagerTask(String name) {
        super(name);
    }

    @Override
    protected void runWithExceptions() throws Exception {
        File sourceFolder = applicationContext.getWorkFolder();

        File[] files = sourceFolder.listFiles(new FilenameFilterImages(false));
        int amountOfImages = files.length;

        List<Future<?>> taskFutures = new ArrayList<>();

        for (int i = 0; i < amountOfImages; i++) {
            File sourceFile = files[i];

            String taskName = "Auto Markup: " + sourceFile.getAbsolutePath();
            AutoMarkupTask subTask = new AutoMarkupTask(taskName);
            subTask.setApplicationContext(applicationContext);
            subTask.setSourceFile(sourceFile);
            subTask.setThreshold(threshold);
            subTask.setAreaSize(areaSize);

            Future<?> future = applicationContext.submitTask(subTask);
            taskFutures.add(future);
        }

        int processedImagesCounter = 0;
        applicationContext.updateProgress(progressBar, 0);
        while (processedImagesCounter < amountOfImages) {
            Iterator<Future<?>> iterator = taskFutures.iterator();
            while (iterator.hasNext()) {
                Future<?> taskFuture = iterator.next();
                if (taskFuture.isDone()) {
                    iterator.remove();
                    processedImagesCounter++;
                    double progress = (double) processedImagesCounter / amountOfImages;
                    applicationContext.updateProgress(progressBar, progress);
                }
            }

            Thread.sleep(100);
        }
    }

    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }

    public void setAreaSize(int areaSize) {
        this.areaSize = areaSize;
    }
}
