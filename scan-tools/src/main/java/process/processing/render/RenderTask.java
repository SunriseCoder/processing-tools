package process.processing.render;

import java.awt.image.BufferedImage;
import java.io.File;

import app.utils.FileUtils;
import dto.TaskParameters;
import multithreading.AbstractTask;
import processing.images.binarization.ImageBinarization;
import processing.images.filters.BinarizationFilter;
import processing.images.filters.ImageFilter;
import processing.images.resize.ImageResize;

public class RenderTask extends AbstractTask {
    public static final String SMOOTH_FILTER_CLASS = "smoothFilterClass";

    public static final String NEED_RESIZE = "needResize";
    public static final String SOURCE_DPI = "sourceDPI";
    public static final String TARGET_DPI = "targetDPI";

    public static final String NEED_BINARIZATION = "needBinarization";
    public static final String WEIGHT_RED = "weightRed";
    public static final String WEIGHT_GREEN = "weightGreen";
    public static final String WEIGHT_BLUE = "weightBlue";
    public static final String COLOR_THRESHOLD = "colorThreshold";

    private File sourceFile;
    private File outputFile;
    private TaskParameters parameters;

    public RenderTask(String name) {
        super(name);
    }

    @Override
    protected void runWithExceptions() throws Exception {
        BufferedImage image = applicationContext.readImage(sourceFile);

        boolean needResize = parameters.getBoolean(NEED_RESIZE);
        if (needResize) {
            image = resizeImage(image);
        }

        boolean needBinarization = parameters.getBoolean(NEED_BINARIZATION);
        if (needBinarization) {
            image = binarizeImage(image);
        }

        // TODO Ask User on the UI
        String formatName = FileUtils.getFileExtension(outputFile.getName());
        applicationContext.writeImage(image, formatName, outputFile);
    }

    public BufferedImage resizeImage(BufferedImage image) throws InstantiationException, IllegalAccessException {
        ImageResize resize = new ImageResize();

        Class<?> smoothFilterClass = parameters.getClass(SMOOTH_FILTER_CLASS);
        ImageFilter smoothFilter = (ImageFilter) smoothFilterClass.newInstance();
        resize.setSmoothFilter(smoothFilter);


        int sourceDPI = parameters.getInt(SOURCE_DPI);
        resize.setSourceDPI(sourceDPI);

        int targetDPI = parameters.getInt(TARGET_DPI);
        resize.setTargetDPI(targetDPI );

        image = resize.processImage(image);

        return image;
    }

    public BufferedImage binarizeImage(BufferedImage image) {
        BinarizationFilter binarizationFilter = new BinarizationFilter();

        double weightRed = parameters.getDouble(WEIGHT_RED);
        binarizationFilter.setWeightRed(weightRed);

        double weightGreen = parameters.getDouble(WEIGHT_GREEN);
        binarizationFilter.setWeightGreen(weightGreen);

        double weightBlue = parameters.getDouble(WEIGHT_BLUE);
        binarizationFilter.setWeightBlue(weightBlue);

        double colorThreshold = parameters.getDouble(COLOR_THRESHOLD);
        colorThreshold *= weightRed + weightGreen + weightBlue;
        binarizationFilter.setColorThreshold(colorThreshold);

        ImageBinarization binarization = new ImageBinarization();
        binarization.setColorFilter(binarizationFilter);
        image = binarization.processImage(image);
        return image;
    }

    public void setSourceFile(File sourceFile) {
        this.sourceFile = sourceFile;
    }

    public void setOutputFile(File outputFile) {
        this.outputFile = outputFile;
    }

    public void setParameters(TaskParameters parameters) {
        this.parameters = parameters;
    }
}
