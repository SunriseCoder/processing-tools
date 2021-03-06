package utils;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class ImageUtils {

    public static BufferedImage createFilledBufferedImage(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setPaint(color);
        graphics.fillRect(0, 0, width, height);
        return image;
    }
}
