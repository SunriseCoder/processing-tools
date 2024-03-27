package process.processing.rotateAndCrop;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

import app.utils.FileUtils;
import dto.Point;
import multithreading.AbstractTask;
import processing.images.crop.AbstractImageCropper;
import processing.images.crop.SimpleImageCropper;
import processing.images.filters.AbstractImageFilter;
import processing.images.rotate.AbstractImageRotator;
import processing.images.rotate.MarkupImageRotator;

public class RotateAndCropTask extends AbstractTask {
    private Class<? extends AbstractImageFilter> smoothFilterClass;
    private AbstractImageFilter smoothFilter;
    private File sourceFolder;
    private File sourceFile;
    private File outputFolder;
    private boolean needRotate;
    private boolean needCrop;

    public RotateAndCropTask(String name) {
        super(name);
    }

    @Override
    protected void runWithExceptions() throws Exception {
        smoothFilter = smoothFilterClass.newInstance();

        String fileName = sourceFile.getName();
        BufferedImage image = applicationContext.readImage(sourceFile);

        // Getting Image Boundaries from Manual Markup by User
        List<Point> markupBoundaries = applicationContext.getSelectionBoundaries(sourceFolder, fileName);
        List<Point> cropBoundaries = markupBoundaries;
        // TODO think about how to integrate this code in the current thread pool architecture
        /*if (markupBoundaries == null) {
            if (!sentNoBoundariesWarning) {
                applicationContext.showWarning("Not all Images has been Marked Up", null);
                sentNoBoundariesWarning = true;
            }
        }*/

        // Rotate
        if (needRotate && markupBoundaries != null) {
            AbstractImageRotator imageRotator = new MarkupImageRotator();
            imageRotator.setSmoothFilter(smoothFilter);
            image = imageRotator.processImage(image, markupBoundaries);
            cropBoundaries = imageRotator.calculateCropBoundaries(image, markupBoundaries);
        }

        // Crop
        if (needCrop && markupBoundaries != null) {
            AbstractImageCropper imageCropper = new SimpleImageCropper();
            image = imageCropper.processImage(image, cropBoundaries);
        }

        String outputFileName = FileUtils.getFileName(sourceFile.getName()) + ".bmp";
        File outputFile = new File(outputFolder, outputFileName);
        String formatName = FileUtils.getFileExtension(outputFileName);
        applicationContext.writeImage(image, formatName, outputFile);
    }

    public void setSmoothFilterClass(Class<? extends AbstractImageFilter> smoothFilterClass) {
        this.smoothFilterClass = smoothFilterClass;
    }

    public void setSourceFolder(File sourceFolder) {
        this.sourceFolder = sourceFolder;
    }

    public void setSourceFile(File sourceFile) {
        this.sourceFile = sourceFile;
    }

    public void setOutputFolder(File outputFolder) {
        this.outputFolder = outputFolder;
    }

    public void setNeedRotate(boolean needRotate) {
        this.needRotate = needRotate;
    }

    public void setNeedCrop(boolean needCrop) {
        this.needCrop = needCrop;
    }
}
