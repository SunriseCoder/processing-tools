package utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import app.utils.MathUtils;

public class MathUtilsTest {

    @Test
    public void testCalculateHypotenuse() {
        assertEquals(5, MathUtils.calculateHypotenuse(3, 4), 0.0001);
    }

    @Test
    public void testCeilToInt() {
        assertEquals(5, MathUtils.ceilToInt(5));
        assertEquals(5, MathUtils.ceilToInt(4.0001));
    }

    @Test
    public void testFloorToInt() {
        assertEquals(5, MathUtils.floorToInt(5));
        assertEquals(5, MathUtils.floorToInt(5.999));
    }

    @Test
    public void testRoundToInt() {
        assertEquals(5, MathUtils.roundToInt((double) 5));
        assertEquals(5, MathUtils.roundToInt((double) 5.3));
        assertEquals(5, MathUtils.roundToInt((double) 4.5));
    }

    @Test
    public void testRoundToLong() {
        assertEquals(5L, MathUtils.roundToLong((double) 5));
        assertEquals(5L, MathUtils.roundToLong((double) 5.3));
        assertEquals(5L, MathUtils.roundToLong((double) 4.5));
    }

    @Test
    public void testAdjustValue() {
        assertEquals(10, MathUtils.adjustValue(10, 1, 100));
        assertEquals(10, MathUtils.adjustValue(1, 10, 100));
        assertEquals(100, MathUtils.adjustValue(150, 1, 100));
    }

    @Test
    public void testMathMeaning() {
        assertEquals(1, MathUtils.mathMeaning(1, 1), 0.001);
        assertEquals(1, MathUtils.mathMeaning(1, 1, 1), 0.001);
        assertEquals(1, MathUtils.mathMeaning(0, 2), 0.001);
        assertEquals(2, MathUtils.mathMeaning(-1, 2, 5), 0.001);
    }
}
