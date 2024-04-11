package app.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import dto.Point;

public class GeometryUtilsTest {

    @Test
    public void testDistanceBetweenTwoPoints() {
        // Same Points
        assertEquals(0, GeometryUtils.distanceBetweenTwoPoints(new Point(0, 0), new Point(0, 0)), 0.0001);
        // Horizontal
        assertEquals(1, GeometryUtils.distanceBetweenTwoPoints(new Point(0, 0), new Point(1, 0)), 0.0001);
        // Vertical
        assertEquals(1, GeometryUtils.distanceBetweenTwoPoints(new Point(0, 0), new Point(0, 1)), 0.0001);
        // Custom
        assertEquals(5, GeometryUtils.distanceBetweenTwoPoints(new Point(0, 0), new Point(3, 4)), 0.0001);
    }

    @Test
    public void testDistanceBetweenPointAndLine() {
        // Same coordinates of all points
        assertEquals(0, GeometryUtils.distanceBetweenPointAndLine(new Point(0, 0), new Point(0, 0), new Point(0, 0)), 0.0001);
        // Point is the one of the ends of the Line
        assertEquals(0, GeometryUtils.distanceBetweenPointAndLine(new Point(0, 0), new Point(0, 0), new Point(4, 0)), 0.0001);
        // Point in the middle of the Line
        assertEquals(0, GeometryUtils.distanceBetweenPointAndLine(new Point(2, 0), new Point(0, 0), new Point(4, 0)), 0.0001);
        // Point is near the middle of the Line
        assertEquals(1, GeometryUtils.distanceBetweenPointAndLine(new Point(2, 1), new Point(0, 0), new Point(4, 0)), 0.0001);
        // Same Line, but backwards
        assertEquals(1, GeometryUtils.distanceBetweenPointAndLine(new Point(2, 1), new Point(4, 0), new Point(0, 0)), 0.0001);
    }
}
