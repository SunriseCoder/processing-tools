package crop.filters;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import app.structures.RGB;
import app.utils.ColorUtils;
import processing.images.filters.BilinearFilter;

public class BilinearFilterTest extends BaseImageFilterTest {
    @Before
    public void beforeTest() {
        filter = new BilinearFilter();
        filter.setImage(image);
    }

    @Test
    public void testGetRGBFromIntegerPoint() {
        double x = 1;
        double y = 2;

        assertEquals(getExpectedPixel((int) x, (int) y), filter.getRGB(x, y));
    }

    @Test
    public void testGetRGBFomMiddleBetween2Points() {
        double x = 1.5;
        double y = 2;

        int rgb1 = getExpectedPixel(Math.floor(x), y);
        int rgb2 = getExpectedPixel(Math.ceil(x), y);

        RGB expectedRGB = ColorUtils.multiplyRGB(rgb1, 0.5);
        expectedRGB.add(ColorUtils.multiplyRGB(rgb2, 0.5));
        int expected = ColorUtils.getRGB(expectedRGB);

        assertEquals(expected, filter.getRGB(x, y));
    }

    @Test
    public void testGetRGBFomMiddleBetween4Points() {
        double x = 1.5;
        double y = 1.5;

        int rgb1 = getExpectedPixel(Math.floor(x), Math.floor(y));
        int rgb2 = getExpectedPixel(Math.ceil(x), Math.floor(y));
        int rgb3 = getExpectedPixel(Math.floor(x), Math.ceil(y));
        int rgb4 = getExpectedPixel(Math.ceil(x), Math.ceil(y));

        RGB expectedRGB = ColorUtils.multiplyRGB(rgb1, 0.25);
        expectedRGB.add(ColorUtils.multiplyRGB(rgb2, 0.25));
        expectedRGB.add(ColorUtils.multiplyRGB(rgb3, 0.25));
        expectedRGB.add(ColorUtils.multiplyRGB(rgb4, 0.25));
        int expected = ColorUtils.getRGB(expectedRGB);

        assertEquals(expected, filter.getRGB(x, y));
    }

    @Test
    public void testGetRGBFomNonMiddleBetween4Points() {
        double x = 1.3;
        double y = 1.4;

        int rgb1 = getExpectedPixel(Math.floor(x), Math.floor(y));
        int rgb2 = getExpectedPixel(Math.ceil(x), Math.floor(y));
        int rgb3 = getExpectedPixel(Math.floor(x), Math.ceil(y));
        int rgb4 = getExpectedPixel(Math.ceil(x), Math.ceil(y));

        RGB expectedRGB = ColorUtils.multiplyRGB(rgb1, 0.42);
        expectedRGB.add(ColorUtils.multiplyRGB(rgb2, 0.18));
        expectedRGB.add(ColorUtils.multiplyRGB(rgb3, 0.28));
        expectedRGB.add(ColorUtils.multiplyRGB(rgb4, 0.12));
        int expected = ColorUtils.getRGB(expectedRGB);

        assertEquals(expected, filter.getRGB(x, y));
    }
}
