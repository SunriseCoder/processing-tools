package process.processing.rotateAndCrop;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Future;

import filters.FilenameFilterImages;
import multithreading.AbstractManagerTask;
import processing.images.filters.AbstractImageFilter;

public class RotateAndCropManagerTask extends AbstractManagerTask {
    private boolean needRotate;
    private boolean needCrop;
    private Class<? extends AbstractImageFilter> smoothFilterClass;

    public RotateAndCropManagerTask(String name) {
        super(name);
    }

    @Override
    protected void runWithExceptions() throws Exception {
        File sourceFolder = applicationContext.getWorkFolder();
        File outputFolder = new File(sourceFolder, "rotated-and-cropped");
        outputFolder.mkdirs();

        File[] files = sourceFolder.listFiles(new FilenameFilterImages(false));
        int amountOfImages = files.length;

        List<Future<?>> taskFutures = new ArrayList<>();

        for (int i = 0; i < amountOfImages; i++) {
            File sourceFile = files[i];

            String taskName = "Rotate and Crop Image: " + sourceFile.getAbsolutePath();
            RotateAndCropTask subTask = new RotateAndCropTask(taskName);
            subTask.setApplicationContext(applicationContext);
            subTask.setSmoothFilterClass(smoothFilterClass);
            subTask.setSourceFolder(sourceFolder);
            subTask.setSourceFile(sourceFile);
            subTask.setOutputFolder(outputFolder);
            subTask.setNeedRotate(needRotate);
            subTask.setNeedCrop(needCrop);

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

    public void setNeedRotate(boolean needRotate) {
        this.needRotate = needRotate;
    }

    public void setNeedCrop(boolean needCrop) {
        this.needCrop = needCrop;
    }

    public void setSmoothFilterClass(Class<? extends AbstractImageFilter> smoothFilterClass) {
        this.smoothFilterClass = smoothFilterClass;
    }
}
