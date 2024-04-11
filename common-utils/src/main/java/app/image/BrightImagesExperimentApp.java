package app.image;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import app.utils.ColorUtils;
import app.utils.ImageUtils;
import app.utils.MathUtils;

public class BrightImagesExperimentApp {
    private static final Logger LOGGER = LogManager.getLogger(BrightImagesExperimentApp.class);

    public static void main(String[] args) {
        LOGGER.info("Application has started");

        try {
            File inputFolder = new File("tmp\\in");
            File outputFolder = new File("tmp\\out");

            BufferedImage mapImage = ImageUtils.loadImage(new File("tmp\\map.png"));
            File[] inputFiles = inputFolder.listFiles();
            for (File inputFile : inputFiles) {
                processInputFile(inputFile, mapImage, outputFolder);
            }

            LOGGER.info("Application has finished successfully");
        } catch (Exception e) {
            LOGGER.error("Error: " + e.getMessage(), e);
        }
    }

    private static void processInputFile(File inputFile, BufferedImage mapImage, File outputFolder) throws IOException {
        LOGGER.info("Brighting Image: " + inputFile.getName());

        BufferedImage inputImage = ImageUtils.loadImage(inputFile);
        File outputFile = new File(outputFolder, inputFile.getName());

        if (isImageMatches(inputImage)) {
            LOGGER.info("Image matches, no need to process it");
            ImageUtils.saveImage(inputImage, outputFile);
            return;
        }

        BufferedImage outputImage = ImageUtils.createEmptyImageSameSizeAndType(inputImage);

        double redMultiplier = (double) 248 / 189;
        double greenMultiplier = (double) 211 / 161;
        double blueMultiplier = (double) 175 / 134;
        int colorBlack = ColorUtils.getRGB(0, 0, 0);
        for (int y = 0; y < inputImage.getHeight(); y++) {
            for (int x = 0; x < inputImage.getWidth(); x++) {
                int mapColor = mapImage.getRGB(x, y);
                if (mapColor == colorBlack) {
                    outputImage.setRGB(x, y, inputImage.getRGB(x, y));
                    continue;
                }

                int inputColor = inputImage.getRGB(x, y);
                int outputRed = MathUtils.adjustValue(MathUtils.roundToInt(ColorUtils.getRed(inputColor) * redMultiplier), 0, 255);
                int outputGreen = MathUtils.adjustValue(MathUtils.roundToInt(ColorUtils.getGreen(inputColor) * greenMultiplier), 0, 255);
                int outputBlue = MathUtils.adjustValue(MathUtils.roundToInt(ColorUtils.getBlue(inputColor) * blueMultiplier), 0, 255);
                int outputColor = ColorUtils.getRGB(outputRed, outputGreen, outputBlue);
                outputImage.setRGB(x, y, outputColor);
            }
        }

        ImageUtils.saveImage(outputImage, outputFile);
    }

    private static boolean isImageMatches(BufferedImage image) {
        int probeX = 10, probeY = 70;
        int targetColor = ColorUtils.getRGB("f8d3af");

        int probeColor = image.getRGB(probeX, probeY);
        int deltaColor = ColorUtils.getColorDelta(probeColor, targetColor);
        boolean result = deltaColor < 10;
        return result;
    }
}
