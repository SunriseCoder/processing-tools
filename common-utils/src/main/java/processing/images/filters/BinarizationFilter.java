package processing.images.filters;

import java.awt.Color;

import app.utils.ColorUtils;

public class BinarizationFilter extends AbstractImageFilter {
    private double colorThreshold = 85 * 5 * 1000;
    private double weightRed = 3;
    private double weightGreen = 1;
    private double weightBlue = 1;

    @Override
    public int getRGB(double x, double y) {
        int sourceColor = getRGBFromImage((int) x, (int) y);

        double r = ColorUtils.getRed(sourceColor) * weightRed;
        double g = ColorUtils.getGreen(sourceColor) * weightGreen;
        double b = ColorUtils.getBlue(sourceColor) * weightBlue;
        double rgb = r * r + g * g + b * b;

        Color color = rgb >= colorThreshold ? Color.WHITE : Color.BLACK;
        int resultColor = color.getRGB();
        return resultColor;
    }

    public void setColorThreshold(double colorThreshold) {
        this.colorThreshold = colorThreshold;
    }

    public void setWeightRed(double weightRed) {
        this.weightRed = weightRed;
    }

    public void setWeightGreen(double weightGreen) {
        this.weightGreen = weightGreen;
    }

    public void setWeightBlue(double weightBlue) {
        this.weightBlue = weightBlue;
    }
}
