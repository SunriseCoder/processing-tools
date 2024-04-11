package app.image;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import app.utils.ColorUtils;
import app.utils.ImageUtils;

public class MakeColorMapApp {
    private static final Logger LOGGER = LogManager.getLogger(MakeColorMapApp.class);

    public static void main(String[] args) throws IOException {
        LOGGER.info("Application has started");

        File file1 = new File("tmp\\dark.png");
        File file2 = new File("tmp\\bright.png");
        File mapFile = new File("tmp\\map.png");

        BufferedImage image1 = ImageUtils.loadImage(file1);
        BufferedImage image2 = ImageUtils.loadImage(file2);

        if (image1.getWidth() != image2.getWidth() || image1.getHeight() != image2.getHeight()) {
            throw new IllegalStateException("Image Resolution mismatch");
        }

        BufferedImage mapImage = ImageUtils.createEmptyImageSameSizeAndType(image1);

        int colorWhite = ColorUtils.getRGB(255, 255, 255);
        int colorBlack = ColorUtils.getRGB(0, 0, 0);
        int targetColor = ColorUtils.getRGB("7c5341");
        for (int y = 0; y < image1.getHeight(); y++) {
            for (int x = 0; x < image2.getWidth(); x++) {
                int color1 = image1.getRGB(x, y);
                int deltaColor1AndTargetColor = ColorUtils.getColorDelta(color1, targetColor);
                int color2 = image2.getRGB(x, y);
                int deltaColor1AndColor2 = ColorUtils.getColorDelta(color1, color2);
                int mapColor = deltaColor1AndTargetColor <= 10 ? colorBlack :
                        deltaColor1AndColor2 <= 40 ? colorBlack : colorWhite;
                mapImage.setRGB(x, y, mapColor);
            }
        }

        ImageUtils.saveImage(mapImage, mapFile);

        LOGGER.info("Application has finished successfully");
    }

}
