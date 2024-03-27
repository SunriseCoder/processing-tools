package processing.images.rotate;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import app.utils.MathUtils;
import dto.Point;
import processing.images.filters.ImageFilter;

public class MarkupImageRotator extends AbstractImageRotator {
    private ImageFilter smoothFilter;

    @Override
    public void setSmoothFilter(ImageFilter smoothFilter) {
        this.smoothFilter = smoothFilter;
    }

    @Override
    public BufferedImage processImage(BufferedImage image, List<Point> markupBoundaries) {
        smoothFilter.setImage(image);

        // Calculate Rotation Angle
        double rotationAngle = calculateRotationAngle(markupBoundaries);

        // Calculating resolution of new Image
        NewImageBoundaries newImageBoundaries = calculateNewImageBoundaries(image, rotationAngle);

        // Creating empty new Image
        BufferedImage newImage = new BufferedImage(newImageBoundaries.width, newImageBoundaries.height,
                BufferedImage.TYPE_INT_RGB);

        // Copying all pixels to the newImage
        copyRotatedPixels(newImage, rotationAngle, newImageBoundaries);

        return newImage;
    }

    private double calculateRotationAngle(List<Point> markupBoundaries) {
        Point cropPoint1 = markupBoundaries.get(0);
        Point cropPoint2 = markupBoundaries.get(1);
        Point cropPoint3 = markupBoundaries.get(2);
        Point cropPoint4 = markupBoundaries.get(3);

        double angle1 = calculateRotationAngleHorizontalEdge(cropPoint1, cropPoint2);
        double angle2 = calculateRotationAngleVerticalEdge(cropPoint2, cropPoint3);
        double angle3 = calculateRotationAngleHorizontalEdge(cropPoint3, cropPoint4);
        double angle4 = calculateRotationAngleVerticalEdge(cropPoint4, cropPoint1);

        double angle = MathUtils.mathMeaning(angle1, angle2, angle3, angle4);
        return angle;
    }

    // TODO Refactor these 2 methods to get rid of duplication code
    private double calculateRotationAngleHorizontalEdge(Point cropPoint1, Point cropPoint2) {
        double cropPointDeltaX = cropPoint2.x - cropPoint1.x;
        double cropPointDeltaY = cropPoint2.y - cropPoint1.y;

        double tan = cropPointDeltaY / cropPointDeltaX;
        double angle = Math.atan(tan);
        return angle;
    }

    private double calculateRotationAngleVerticalEdge(Point cropPoint1, Point cropPoint2) {
        double cropPointDeltaX = cropPoint2.x - cropPoint1.x;
        double cropPointDeltaY = cropPoint2.y - cropPoint1.y;

        double tan = cropPointDeltaX / cropPointDeltaY;
        double angle = -Math.atan(tan);
        return angle;
    }

    @Override
    public List<Point> calculateCropBoundaries(BufferedImage image, List<Point> markupBoundaries) {
     // Calculate Rotation Angle
        double rotationAngle = calculateRotationAngle(markupBoundaries);

        // Calculating resolution of new Image
        NewImageBoundaries newImageBoundaries = calculateNewImageBoundaries(image, rotationAngle);

        // Image Crop Boundaries
        List<Point> cropBoundaries = rotatePoints(markupBoundaries, -rotationAngle);
        // Applying offset
        cropBoundaries = cropBoundaries.stream()
                .map(point -> new Point(point.x - newImageBoundaries.minX, point.y - newImageBoundaries.minY))
                .collect(Collectors.toList());

        return cropBoundaries;
    }

    private NewImageBoundaries calculateNewImageBoundaries(BufferedImage sourceImage, double rotationAngle) {
        NewImageBoundaries result = new NewImageBoundaries();

        List<Point> sourceImageBoundaries = new ArrayList<>();
        sourceImageBoundaries.add(new Point(0, 0));
        sourceImageBoundaries.add(new Point(sourceImage.getWidth(), 0));
        sourceImageBoundaries.add(new Point(sourceImage.getWidth(), sourceImage.getHeight()));
        sourceImageBoundaries.add(new Point(0, sourceImage.getHeight()));

        List<Point> rotatedImageBoundaries = rotatePoints(sourceImageBoundaries, -rotationAngle);

        result.minX = (int) Math.floor(rotatedImageBoundaries.stream()
                        .mapToDouble(point -> point.x).min().getAsDouble());
        result.maxX = (int) Math.ceil(rotatedImageBoundaries.stream()
                .mapToDouble(point -> point.x).max().getAsDouble());
        result.minY = (int) Math.floor(rotatedImageBoundaries.stream()
                .mapToDouble(point -> point.y).min().getAsDouble());
        result.maxY = (int) Math.ceil(rotatedImageBoundaries.stream()
                .mapToDouble(point -> point.y).max().getAsDouble());

        result.width = result.maxX - result.minX + 1;
        result.height = result.maxY - result.minY + 1;

        return result;
    }

    private void copyRotatedPixels(BufferedImage newImage, double rotationAngle, NewImageBoundaries newImageBoundaries) {
        for (int y = 0; y < newImage.getHeight(); y++) {
            for (int x = 0; x < newImage.getWidth(); x++) {
                // Applying newImage offset before rotation
                Point sourcePoint = new Point(x + newImageBoundaries.minX, y + newImageBoundaries.minY);
                sourcePoint = rotatePoint(sourcePoint, rotationAngle);
                int color = smoothFilter.getRGB(sourcePoint);
                try {
                    newImage.setRGB(x, y, color);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private List<Point> rotatePoints(List<Point> points, double angle) {
        List<Point> result = points.stream().map(point -> rotatePoint(point, angle)).collect(Collectors.toList());
        return result;
    }

    private Point rotatePoint(Point point, double angle) {
        double x = Math.cos(angle) * point.x - Math.sin(angle) * point.y;
        double y = Math.sin(angle) * point.x + Math.cos(angle) * point.y;
        Point rotatedPoint = new Point(x, y);
        return rotatedPoint;
    }

    private static class NewImageBoundaries {
        public int width;
        public int height;

        public int minX;
        public int maxX;
        public int minY;
        public int maxY;
    }
}
