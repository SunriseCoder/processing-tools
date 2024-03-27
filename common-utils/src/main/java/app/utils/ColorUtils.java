package app.utils;

import app.structures.RGB;

public class ColorUtils {

    public static int getRGB(int r, int g, int b) {
        int a = 255;
        int rgb = ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | ((b & 0xFF) << 0);
        return rgb;
    }

    public static int getRGB(RGB rgb) {
        int r = Math.min((int) Math.round(rgb.r), 255);
        int g = Math.min((int) Math.round(rgb.g), 255);
        int b = Math.min((int) Math.round(rgb.b), 255);

        int result = getRGB(r, g, b);

        return result;
    }

    public static RGB getRGB(int color) {
        RGB rgb = new RGB();

        rgb.r = getRed(color);
        rgb.g = getGreen(color);
        rgb.b = getBlue(color);

        return rgb;
    }

    public static int getRGB(String hexColor) {
        int rgb = NumeralSystemsUtils.hexToDecInt(hexColor);
        return rgb;
    }

    public static int getRGBWitoutAlpha(int color) {
        int colorWithoutAlpha = color & 0xFFFFFF;
        return colorWithoutAlpha;
    }

    public static RGB multiplyRGB(int rgb, double factor) {
        RGB resultRGB = new RGB();

        resultRGB.r = (rgb >> 16 & 0xFF) * factor;
        resultRGB.g = (rgb >> 8 & 0xFF) * factor;
        resultRGB.b = (rgb & 0xFF) * factor;

        return resultRGB;
    }

    public static int getRed(int color) {
        int red = color >> 16 & 0xFF;
        return red;
    }

    public static int getGreen(int color) {
        int green = color >> 8 & 0xFF;
        return green;
    }

    public static int getBlue(int color) {
        int blue = color & 0xFF;
        return blue;
    }
}
