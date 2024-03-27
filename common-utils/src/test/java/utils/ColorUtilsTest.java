package utils;

import static org.junit.Assert.assertEquals;

import java.awt.Color;

import org.junit.Test;

import app.structures.RGB;
import app.utils.ColorUtils;

public class ColorUtilsTest {

    @Test
    public void testGetIntRGBFromChannels() {
        assertEquals(Color.WHITE.getRGB(), ColorUtils.getRGB(255, 255, 255));
        assertEquals(Color.GRAY.getRGB(), ColorUtils.getRGB(128, 128, 128));
        assertEquals(Color.BLACK.getRGB(), ColorUtils.getRGB(0, 0, 0));
    }

    @Test
    public void testGetIntRGBFromRGBStructure() {
        RGB rgb = new RGB(1, 2.3, 3);

        assertEquals(ColorUtils.getRGB(1, 2, 3), ColorUtils.getRGB(rgb));
    }

    @Test
    public void testGetIntRGBFromRGBStructureWithOverload() {
        RGB rgb = new RGB(1, 2.3, 315);

        assertEquals(ColorUtils.getRGB(1, 2, 255), ColorUtils.getRGB(rgb));
    }

    @Test
    public void testGetIntRGBFromRGBStructureRound() {
        int rgb = ColorUtils.getRGB(1, 2, 3);
        RGB multipliedRGB = ColorUtils.multiplyRGB(rgb, 5.4);

        assertEquals(ColorUtils.getRGB(5, 11, 16), ColorUtils.getRGB(multipliedRGB));
    }

    @Test
    public void testMultiplyRGB() {
        int rgb = ColorUtils.getRGB(1, 2, 3);
        RGB multipliedRGB = ColorUtils.multiplyRGB(rgb, 5.4);

        assertEquals(5.4, multipliedRGB.r, 0.001);
        assertEquals(10.8, multipliedRGB.g, 0.001);
        assertEquals(16.2, multipliedRGB.b, 0.001);
    }
}
