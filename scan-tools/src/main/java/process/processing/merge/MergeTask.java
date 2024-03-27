package process.processing.merge;

import java.awt.image.BufferedImage;
import java.io.File;

import app.utils.FileUtils;
import multithreading.AbstractTask;
import processing.images.merge.ImageMerge;

public class MergeTask extends AbstractTask {
    private File sourceFile1;
    private File sourceFile2;
    private File outputFile;

    public MergeTask(String name) {
        super(name);
    }

    @Override
    protected void runWithExceptions() throws Exception {
        BufferedImage image1 = sourceFile1 == null ? null : applicationContext.readImage(sourceFile1);
        BufferedImage image2 = sourceFile2 == null ? null : applicationContext.readImage(sourceFile2);

        ImageMerge merge = new ImageMerge();
        BufferedImage resultImage = merge.mergeImages(image1, image2);

        String formatName = FileUtils.getFileExtension(outputFile.getName());
        applicationContext.writeImage(resultImage, formatName, outputFile);
    }

    public void setSourceFile1(File sourceFile1) {
        this.sourceFile1 = sourceFile1;
    }

    public void setSourceFile2(File sourceFile2) {
        this.sourceFile2 = sourceFile2;
    }

    public void setOutputFile(File outputFile) {
        this.outputFile = outputFile;
    }
}
