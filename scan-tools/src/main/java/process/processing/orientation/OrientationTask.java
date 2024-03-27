package process.processing.orientation;

import java.awt.image.BufferedImage;
import java.io.File;

import app.utils.FileUtils;
import multithreading.AbstractTask;
import processing.images.rotation.AbstractOrientationRotate;

public class OrientationTask extends AbstractTask {
    private Class<? extends AbstractOrientationRotate> rotationMethodClass;
    private File sourceFile;
    private File outputFolder;
    private int imageIndex;

    public OrientationTask(String name) {
        super(name);
    }

    @Override
    protected void runWithExceptions() throws Exception {
        BufferedImage image = applicationContext.readImage(sourceFile);

        AbstractOrientationRotate rotator = rotationMethodClass.newInstance();
        image = rotator.rotateImage(image, imageIndex);

        String fileName = sourceFile.getName();
        String outputFormat = "bmp";
        String outputFileName = FileUtils.getFileName(fileName) + "." + outputFormat;
        File outputFile = new File(outputFolder, outputFileName);
        applicationContext.writeImage(image, outputFormat, outputFile);
    }

    public void setRotationMethodClass(Class<? extends AbstractOrientationRotate> rotationMethodClass) {
        this.rotationMethodClass = rotationMethodClass;
    }

    public void setSourceFile(File sourceFile) {
        this.sourceFile = sourceFile;
    }

    public void setOutputFolder(File outputFolder) {
        this.outputFolder = outputFolder;
    }

    public void setImageIndex(int imageIndex) {
        this.imageIndex = imageIndex;
    }
}
