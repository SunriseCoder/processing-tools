package app.utils;

import dto.Point;

public class GeometryUtils {

    public static double distanceBetweenTwoPoints(Point a, Point b) {
        // Pythagora's Theorem
        double distance = Math.sqrt(Math.pow(b.x - a.x, 2) + Math.pow(b.y - a.y, 2));
        return distance;
    }

    public static double distanceBetweenPointAndLine(Point point, Point lineStart, Point lineEnd) {
        double distance;
        // The method is to get Vector Multiplication of 2 Vectors - Line from A to B and Vector from A to Point
        // This Multiplication will be the Area of Parallelogram
        // We can get Parallelogram's Height by Dividing Area by Base (Length of Line)
        double lineLength = distanceBetweenTwoPoints(lineStart, lineEnd);
        if (lineLength == 0) {
            distance = distanceBetweenTwoPoints(point, lineStart);
            return distance;
        }

        double parallelogramArea = point.y * (lineEnd.x - lineStart.x) + point.x * (lineStart.y - lineEnd.y) +
                lineStart.x * lineEnd.y - lineStart.y * lineEnd.x;
        distance = Math.abs(parallelogramArea / lineLength);

        return distance;
    }
}
