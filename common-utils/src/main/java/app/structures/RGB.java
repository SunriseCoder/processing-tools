package app.structures;

public class RGB {

    public RGB() {

    }

    public RGB(double r, double g, double b) {
        this.r = r;
        this.g = g;
        this.b = b;
    }

    public double r;
    public double g;
    public double b;

    public void add(RGB rgb) {
        r += rgb.r;
        g += rgb.g;
        b += rgb.b;
    }

    @Override
    public String toString() {
        return RGB.class.getSimpleName() + "[r=" + r + ", g=" + g + ", b=" + b + "]";
    }
}
