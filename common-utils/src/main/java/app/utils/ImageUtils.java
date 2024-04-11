package app.utils;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public class ImageUtils {

    public static BufferedImage createFilledBufferedImage(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setPaint(color);
        graphics.fillRect(0, 0, width, height);
        return image;
    }

    public static BufferedImage loadImage(File file) throws IOException {
        BufferedImage image = ImageIO.read(file);
        return image;
    }

    public static boolean saveImage(BufferedImage image, File outputFile) throws IOException {
        String formatName = FileUtils.getFileExtension(outputFile.getName());
        boolean result = ImageIO.write(image, formatName , outputFile);
        return result;
    }

    public static BufferedImage createEmptyImageSameSizeAndType(BufferedImage sourceImage) {
        int width = sourceImage.getWidth();
        int height = sourceImage.getHeight();
        int type = sourceImage.getType();
        BufferedImage result = new BufferedImage(width, height, type);
        return result;
    }
}
