package app.image.core;

import java.awt.image.BufferedImage;
import java.awt.image.RasterFormatException;
import java.io.File;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import app.utils.ImageUtils;
import app.utils.MathUtils;

public class CustomImageSplitter {
    private static final Logger LOGGER = LogManager.getLogger(CustomImageSplitter.class);

    private int offsetX;
    private int offsetY;

    private int rows;
    private int cols;

    private int subImageWidth;
    private int subImageHeight;

    private double spaceBetweenRows;
    private double spaceBetweenCols;

    public void setOffset(int x, int y) {
        this.offsetX = x;
        this.offsetY = y;
    }

    public void setRows(int rows) {
        this.rows = rows;
    }

    public void setCols(int cols) {
        this.cols = cols;
    }

    public void setSubImageWidth(int subImageWidth) {
        this.subImageWidth = subImageWidth;
    }

    public void setSubImageHeight(int subImageHeight) {
        this.subImageHeight = subImageHeight;
    }

    public void setSpaceBetweenRows(double spaceBetweenRows) {
        this.spaceBetweenRows = spaceBetweenRows;
    }

    public void setSpaceBetweenCols(double spaceBetweenCols) {
        this.spaceBetweenCols = spaceBetweenCols;
    }

    public void split(BufferedImage image, File outputFolder, String filePrefix) throws IOException {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int x = offsetX + MathUtils.roundToInt(col * (subImageWidth + spaceBetweenCols));
                int y = offsetY + MathUtils.roundToInt(row * (subImageHeight + spaceBetweenRows));

                try {
                    BufferedImage subImage = image.getSubimage(x, y, subImageWidth, subImageHeight);

                    File outputFile = new File(outputFolder, filePrefix + "-" + row + "-" + col + ".png");
                    LOGGER.info("Saving image to: " + outputFile.toString());
                    ImageUtils.saveImage(subImage, outputFile);
                } catch (RasterFormatException e) {
                    LOGGER.error("row = " + row + ", col = " + col + ", x = " + x + ", y = " + y, e);
                }
            }
        }
    }
}
