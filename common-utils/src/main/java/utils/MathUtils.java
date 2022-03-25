package utils;

import java.util.Arrays;

public class MathUtils {

	public static double calculateDistance(double a, double b) {
		return Math.sqrt(a * a + b * b);
	}

	public static int ceilToInt(double value) {
	    long result = Math.round(Math.ceil(value));
        return (int) result;
	}

	public static int floorToInt(double value) {
	    long result = Math.round(Math.floor(value));
        return (int) result;
	}

    public static int roundToInt(double value) {
        long result = roundToLong(value);
        return (int) result;
    }

    public static long roundToLong(double value) {
        long result = Math.round(value);
        return result;
    }

    public static int adjustValue(int value, int min, int max) {
        int result = value;
        result = result < min ? min : result;
        result = result > max ? max : result;
        return result;
    }

    public static double mathMeaning(double... values) {
        double sum = Arrays.stream(values).sum();
        double result = sum / values.length;
        return result;
    }

    public static int sign(long value) {
        int result = value == 0 ? 0 : value > 0 ? 1 : -1;
        return result;
    }
}
