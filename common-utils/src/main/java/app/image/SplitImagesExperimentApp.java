package app.image;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import app.image.core.CustomImageSplitter;
import app.utils.ColorUtils;
import app.utils.FileUtils;
import app.utils.ImageUtils;

public class SplitImagesExperimentApp {
    private static final Logger LOGGER = LogManager.getLogger(SplitImagesExperimentApp.class);

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
        BufferedImage image = ImageUtils.loadImage(inputFile);

        int offsetX = 258;
        int offsetY = findOffsetY(image);
        String filePrefix = FileUtils.getFileName(inputFile.getName());
        splitImage(image, offsetX, offsetY, outputFolder, filePrefix);
    }

    private static int findOffsetY(BufferedImage image) {
        int targetColor = ColorUtils.getRGB("7c5341");

        int x = 500;
        int foundColorTimes = 0;
        SearchOffsetPhases phase = SearchOffsetPhases.Roaming;
        for (int y = 220; y < 1080; y++) {
            int rgb = image.getRGB(x, y);
            int colorDelta = ColorUtils.getColorDelta(rgb, targetColor);
            int colorDeltaMax = 10;

            switch (phase) {
            case Roaming:
                if (colorDelta <= colorDeltaMax) {
                    phase = SearchOffsetPhases.FoundColor;
                    foundColorTimes++;
                }
                break;
            case FoundColor:
                if (colorDelta <= colorDeltaMax) {
                    foundColorTimes++;
                } else {
                    if (foundColorTimes >= 10) {
                        return y;
                    } else {
                        phase = SearchOffsetPhases.Roaming;
                        foundColorTimes = 0;
                    }
                }

            default:
                break;
            }
        }

        throw new IllegalStateException("Offset Y was not found");
    }

    private enum SearchOffsetPhases {
        Roaming, FoundColor
    }

    private static void splitImage(BufferedImage image, int offsetX, int offsetY, File outputFolder, String filePrefix) throws IOException {
        CustomImageSplitter splitter = new CustomImageSplitter();

        int subImageWidth = 149;
        int subImageHeight = 149;
        double spaceBetweenRows = (double) (726 - 239) / 3 - subImageHeight;
        double spaceBetweenCols = (double) (1359 - 223) / 7 - subImageWidth;

        splitter.setOffset(offsetX, offsetY);
        splitter.setRows(4);
        splitter.setCols(8);
        splitter.setSubImageWidth(subImageWidth);
        splitter.setSubImageHeight(subImageHeight);
        splitter.setSpaceBetweenRows(spaceBetweenRows);
        splitter.setSpaceBetweenCols(spaceBetweenCols);

        splitter.split(image, outputFolder, filePrefix);
    }
}
