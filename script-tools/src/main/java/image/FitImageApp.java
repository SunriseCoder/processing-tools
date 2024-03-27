package image;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import app.utils.ColorUtils;
import app.utils.ImageUtils;
import processing.images.filters.BilinearFilter;
import processing.images.resize.ImageResize;

public class FitImageApp {

    public static void main(String[] args) throws IOException {
        if (args.length < 5 || args[0].equals("--help")) {
            printUsage();
            System.exit(-1);
        }

        // Parsing command-line arguments
        File inputFile = new File(args[0]);
        int outputWidth = Integer.parseInt(args[1]);
        int outputHeight = Integer.parseInt(args[2]);
        Color color = new Color(ColorUtils.getRGB(args[3]));
        File outputFile = new File(args[4]);

        // Calculating factors and scale parameters
        BufferedImage inputImage = ImageUtils.loadImage(inputFile);
        double factorX = (double) outputWidth / inputImage.getWidth();
        double factorY = (double) outputHeight / inputImage.getHeight();
        int sourceDPI = factorX > factorY ? inputImage.getHeight() : inputImage.getWidth();
        int targetDPI = factorX > factorY ? outputHeight : outputWidth;

        // Resize Image
        ImageResize resizer = new ImageResize();
        resizer.setSmoothFilter(new BilinearFilter());
        resizer.setSourceDPI(sourceDPI);
        resizer.setTargetDPI(targetDPI);
        BufferedImage resizedImage = resizer.processImage(inputImage);

        // Adjusting borders
        int offsetX = (outputWidth - resizedImage.getWidth()) / 2;
        int offsetY = (outputHeight - resizedImage.getHeight()) / 2;
        BufferedImage outputImage = ImageUtils.createFilledBufferedImage(outputWidth, outputHeight, color);
        outputImage.getGraphics().drawImage(resizedImage, offsetX, offsetY, null);

        // Saving Image
        ImageUtils.saveImage(outputImage, outputFile);
    }

    private static void printUsage() {
        System.out.println("Usage:");
        System.out.println("\t" + FitImageApp.class.getName()
                + " <input_file> <output_width> <output_height> <color (FF8800)> <output_file>");
    }
}
