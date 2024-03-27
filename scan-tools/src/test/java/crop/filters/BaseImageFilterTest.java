package crop.filters;

import static app.utils.ColorUtils.getRGB;
import static org.junit.Assert.assertEquals;

import java.awt.image.BufferedImage;

import org.junit.BeforeClass;
import org.junit.Test;

import processing.images.filters.ImageFilter;

public class BaseImageFilterTest {
    protected static BufferedImage image;
    protected ImageFilter filter;

    protected static int[] pixels;
    protected static int resolutionX;

    @BeforeClass
    public static void beforeClass() {
        pixels = new int[] {
                getRGB(255, 0, 0), getRGB(0, 255, 0), getRGB(0, 0, 255),
                getRGB(255, 255, 0), getRGB(255, 0, 255), getRGB(0, 255, 255),
                getRGB(127, 127, 127), getRGB(255, 255, 255), getRGB(0, 0, 0),
                getRGB(127, 127, 127), getRGB(255, 255, 255), getRGB(0, 0, 0)
        };

        resolutionX = 3;
        int resolutionY = pixels.length / resolutionX;
        image = new BufferedImage(resolutionX, resolutionY, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, resolutionX, resolutionY, pixels, 0, resolutionX);
    }

    protected int getExpectedPixel(double x, double y) {
        return getExpectedPixel((int) x, (int) y);
    }

    protected int getExpectedPixel(int x, int y) {
        return pixels[y * resolutionX + x];
    }

    @Test
    public void testCheckTestData() {
        for (double y = 0; y < image.getHeight(); y++) {
            for (double x = 0; x < image.getWidth(); x++) {
                assertEquals(getExpectedPixel(x, y), image.getRGB((int) x, (int) y));
            }
        }
    }
}
