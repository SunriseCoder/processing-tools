package processing.images.filters;

import app.structures.RGB;
import app.utils.ColorUtils;
import app.utils.MathUtils;

public class BilinearFilter extends AbstractImageFilter {

    @Override
    public int getRGB(double x, double y) {
        int floorX = MathUtils.floorToInt(x);
        int floorY = MathUtils.floorToInt(y);
        int ceilX = MathUtils.ceilToInt(x);
        int ceilY = MathUtils.ceilToInt(y);
        ceilX = floorX == ceilX ? ceilX + 1 : ceilX;
        ceilY = floorY == ceilY ? ceilY + 1 : ceilY;

        int colorTopLeft = getRGBFromImage(floorX, floorY);
        int colorTopRight = getRGBFromImage(ceilX, floorY);
        int colorBottomLeft = getRGBFromImage(floorX, ceilY);
        int colorBottomRight = getRGBFromImage(ceilX, ceilY);

        RGB rgb = ColorUtils.multiplyRGB(colorTopLeft, (ceilX - x) * (ceilY - y));
        rgb.add(ColorUtils.multiplyRGB(colorTopRight, (x - floorX) * (ceilY - y)));
        rgb.add(ColorUtils.multiplyRGB(colorBottomLeft, (ceilX - x) * (y - floorY)));
        rgb.add(ColorUtils.multiplyRGB(colorBottomRight, (x - floorX) * (y - floorY)));

        int result = ColorUtils.getRGB(rgb);
        return result;
    }
}
