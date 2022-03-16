package process.processing.render;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Future;

import dto.TaskParameters;
import filters.FilenameFilterImages;
import multithreading.AbstractManagerTask;
import utils.FileUtils;

public class RenderManagerTask extends AbstractManagerTask {
    private TaskParameters taskParameters;

    public RenderManagerTask(String name) {
        super(name);
    }

    @Override
    protected void runWithExceptions() throws Exception {
        File sourceFolder = applicationContext.getWorkFolder();
        File outputFolder = new File(sourceFolder, "rendered");
        outputFolder.mkdirs();

        File[] files = sourceFolder.listFiles(new FilenameFilterImages(false));
        int amountOfImages = files.length;
        if (amountOfImages == 0) {
            applicationContext.showWarning("There is no Images to Render", null);
            return;
        }

        List<Future<?>> taskFutures = new ArrayList<>();

        for (int i = 0; i < amountOfImages; i++) {
            File sourceFile = files[i];

            // Output File and Task Name
            boolean needBinarization = taskParameters.getBoolean(RenderTask.NEED_BINARIZATION);
            String formatName = needBinarization ? "png" : "bmp";
            String outputFileName = FileUtils.getFileName(sourceFile.getName()) + "." + formatName;

            String taskName = "Render Image: " + outputFileName;
            RenderTask subTask = new RenderTask(taskName);

            subTask.setApplicationContext(applicationContext);

            subTask.setSourceFile(sourceFile);

            File outputFile = new File(outputFolder, outputFileName);
            subTask.setOutputFile(outputFile);

            subTask.setParameters(taskParameters);

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

    public void setTaskParameters(TaskParameters taskParameters) {
        this.taskParameters = taskParameters;
    }
}
