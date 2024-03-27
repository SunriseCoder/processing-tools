package processing.images.merge;

import java.awt.Color;
import java.awt.image.BufferedImage;

import app.utils.ImageUtils;

public class ImageMerge {
    private Color defaultColor = Color.WHITE;

    public BufferedImage mergeImages(BufferedImage image1, BufferedImage image2) {
        if (image1 == null) {
            image1 = ImageUtils.createFilledBufferedImage(image2.getWidth(), image2.getHeight(), defaultColor);
        }
        if (image2 == null) {
            image2 = ImageUtils.createFilledBufferedImage(image1.getWidth(), image1.getHeight(), defaultColor);
        }

        int width = image1.getWidth() + image2.getWidth();
        int height = Math.max(image1.getHeight(), image2.getHeight());
        BufferedImage newImage = ImageUtils.createFilledBufferedImage(width, height, defaultColor);

        int offsetX = 0;
        int offsetY = (newImage.getHeight() - image1.getHeight()) / 2;
        int[] data = image1.getRGB(0, 0, image1.getWidth(), image1.getHeight(), null, 0, image1.getWidth());
        newImage.setRGB(offsetX, offsetY, image1.getWidth(), image1.getHeight(), data, 0, image1.getWidth());

        offsetX = image1.getWidth();
        offsetY = (newImage.getHeight() - image2.getHeight()) / 2;
        data = image2.getRGB(0, 0, image2.getWidth(), image2.getHeight(), null, 0, image2.getWidth());
        newImage.setRGB(offsetX, offsetY, image2.getWidth(), image2.getHeight(), data, 0, image2.getWidth());

        return newImage;
    }
}
