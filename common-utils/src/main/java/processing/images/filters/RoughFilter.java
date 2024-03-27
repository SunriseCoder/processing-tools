package processing.images.filters;

import app.utils.MathUtils;

public class RoughFilter extends AbstractImageFilter {

    @Override
    public int getRGB(double x, double y) {
        int intX = MathUtils.roundToInt(x);
        int intY = MathUtils.roundToInt(y);

        int result = getRGBFromImage(intX, intY);
        return result;
    }
}
