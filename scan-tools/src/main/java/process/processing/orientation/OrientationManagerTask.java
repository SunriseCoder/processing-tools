package process.processing.orientation;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Future;

import filters.FilenameFilterImages;
import multithreading.AbstractManagerTask;
import processing.images.rotation.AbstractOrientationRotate;

public class OrientationManagerTask extends AbstractManagerTask {
    private Class<? extends AbstractOrientationRotate> rotationMethodClass;

    public OrientationManagerTask(String name) {
        super(name);
    }

    @Override
    protected void runWithExceptions() throws Exception {
        File sourceFolder = applicationContext.getWorkFolder();
        File outputFolder = new File(sourceFolder, "oriented");
        outputFolder.mkdirs();

        File[] files = sourceFolder.listFiles(new FilenameFilterImages(false));
        int amountOfImages = files.length;

        List<Future<?>> taskFutures = new ArrayList<>();

        for (int i = 0; i < amountOfImages; i++) {
            File sourceFile = files[i];

            String taskName = "Image Orientation: " + sourceFile.getAbsolutePath();
            OrientationTask subTask = new OrientationTask(taskName);
            subTask.setApplicationContext(applicationContext);
            subTask.setRotationMethodClass(rotationMethodClass);
            subTask.setImageIndex(i);
            subTask.setSourceFile(sourceFile);
            subTask.setOutputFolder(outputFolder);

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

    public void setRotationMethodClass(Class<? extends AbstractOrientationRotate> rotationMethodClass) {
        this.rotationMethodClass = rotationMethodClass;
    }
}
