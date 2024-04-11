package app.image;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import app.utils.ImageUtils;

public class CropImagesExperimentApp {
    private static final Logger LOGGER = LogManager.getLogger(CropImagesExperimentApp.class);

    public static void main(String[] args) {
        LOGGER.info("Application has started");

        try {
            File inputFolder = new File("tmp\\in");
            File outputFolder = new File("tmp\\out");

            File[] inputFiles = inputFolder.listFiles();
            for (File inputFile : inputFiles) {
                processInputFile(inputFile, outputFolder);
            }

            LOGGER.info("Application has finished successfully");
        } catch (Exception e) {
            LOGGER.error("Error: " + e.getMessage(), e);
        }
    }

    private static void processInputFile(File inputFile, File outputFolder) throws IOException {
        LOGGER.info("Cropping Image: " + inputFile.getName());

        BufferedImage image = ImageUtils.loadImage(inputFile);
        BufferedImage croppedImage = image.getSubimage(0, 0, 1920, 1040);
        File outputFile = new File(outputFolder, inputFile.getName());
        ImageUtils.saveImage(croppedImage, outputFile);
    }
}
