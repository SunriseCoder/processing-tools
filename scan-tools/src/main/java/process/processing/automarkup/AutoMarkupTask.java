package process.processing.automarkup;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import app.structures.RGB;
import app.utils.ColorUtils;
import app.utils.MathUtils;
import dto.IntPoint;
import dto.Point;
import multithreading.AbstractTask;

public class AutoMarkupTask extends AbstractTask {
    private File sourceFile;
    private int threshold;
    private int areaSize;

    public AutoMarkupTask(String name) {
        super(name);
    }

    @Override
    protected void runWithExceptions() throws Exception {
        BufferedImage image = applicationContext.readImage(sourceFile);
        ImageMap imageMap = new ImageMap(image, threshold, areaSize);
        ImageAnalyzer analyzer = new ImageAnalyzer(imageMap, areaSize);

        List<Point> selectionBoundaries = new ArrayList<>();
        int height = image.getHeight();
        int width = image.getWidth();
        int minImageSide = Math.min(height, width);

        // Top Left
        outer:
        for (int i = 0; i < minImageSide; i++) {
            System.out.println(i);
            for (int x = 0; x <= i; x++) {
                int y = i - x;
                if (x == 124 && y == 171) {
                    System.out.println();
                }
                boolean hasConnections = analyzer.findUsefulNeighbors(x, y);
                if (hasConnections) {
                    selectionBoundaries.add(new Point(x, y));
                    break outer;
                }
            }
        }

        // Top Right
        outer:
        for (int i = 0; i < minImageSide; i++) {
            for (int x = width - i - 1; x < width; x++) {
                int y = x + i - width + 1;
                boolean hasConnections = analyzer.findUsefulNeighbors(x, y);
                if (hasConnections) {
                    selectionBoundaries.add(new Point(x, y));
                    break outer;
                }
            }
        }

        // Bottom Right
        outer:
        for (int i = 0; i < minImageSide; i++) {
            for (int x = width - i - 1; x < width; x++) {
                int y = height + width - x - i - 2;
                boolean hasConnections = analyzer.findUsefulNeighbors(x, y);
                if (hasConnections) {
                    selectionBoundaries.add(new Point(x, y));
                    break outer;
                }
            }
        }

        // Bottom Left
        outer:
        for (int i = 0; i < minImageSide; i++) {
            for (int x = 0; x <= i; x++) {
                int y = x + height - i - 1;
                boolean hasConnections = analyzer.findUsefulNeighbors(x, y);
                if (hasConnections) {
                    selectionBoundaries.add(new Point(x, y));
                    break outer;
                }
            }
        }

        if (!selectionBoundaries.isEmpty()) {
            applicationContext.saveSelectionBoundaries(sourceFile.getParentFile(), sourceFile.getName(), selectionBoundaries);
        }
    }

    public void setSourceFile(File sourceFile) {
        this.sourceFile = sourceFile;
    }

    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }

    public void setAreaSize(int areaSize) {
        this.areaSize = areaSize;
    }

    private static class ImageMap {
        private BufferedImage image;
        private int threshold;
        private int areaSide;

        private Status[][] imageMap;
        private Boundaries boundaries;

        public ImageMap(BufferedImage image, int threshold, int areaSize) {
            this.image = image;
            this.threshold = threshold;
            this.areaSide = MathUtils.ceilToInt(Math.sqrt(areaSize)) * 2; // For non-square areas
            imageMap = new Status[image.getWidth()][image.getHeight()];
        }

        public int getHeight() {
            return image.getHeight();
        }

        public int getWidth() {
            return image.getWidth();
        }

        public boolean isUseful(int x, int y) {
            Status mapValue = imageMap[x][y];
            if (mapValue == null) {
                mapValue = calculatePixelStatus(x, y);
                imageMap[x][y] = mapValue;
            }

            boolean isUseful = mapValue == Status.Useful;
            if (isUseful) {
                isUseful &= isMatchesAreaBoundaries(x, y);
            }
            return isUseful;
        }

        private Status calculatePixelStatus(int x, int y) {
            if (x < 0 || x >= image.getWidth() || y < 0 || y >= image.getHeight()) {
                return null;
            }

            int color = image.getRGB(x, y);
            RGB rgb = ColorUtils.getRGB(color);
            double sum = Math.pow(rgb.r, 2) + Math.pow(rgb.g, 2) + Math.pow(rgb.b, 2);
            Status result = sum >= threshold ? Status.Useful : Status.NotUseful;
            return result;
        }

        private boolean isMatchesAreaBoundaries(int x, int y) {
            if (boundaries.reachedLimitX && (x < boundaries.minX || x > boundaries.maxX)) {
                return false;
            }

            if (boundaries.reachedLimitY && (y < boundaries.minY || y > boundaries.maxY)) {
                return false;
            }

            if (x < boundaries.minX) {
                boundaries.setMinX(x);
            }

            if (x > boundaries.maxX) {
                boundaries.setMaxX(x);
            }

            if (y < boundaries.minY) {
                boundaries.setMinY(y);
            }

            if (y > boundaries.maxY) {
                boundaries.setMaxY(y);
            }

            return true;
        }

        private enum Status {
            Useful, NotUseful;
        }

        public void clearAreaMeasurements() {
            boundaries = new Boundaries();
            boundaries.areaSide = areaSide;
        }

        private class Boundaries {
            private int minX = Integer.MAX_VALUE - 10;
            private int maxX = -1;
            private int minY = Integer.MAX_VALUE - 10;
            private int maxY = -1;

            private boolean reachedLimitX = false;
            private boolean reachedLimitY = false;

            private int areaSide;

            public void setMinX(int minX) {
                this.minX = minX;
                checkLimitX();
            }

            public void setMaxX(int maxX) {
                this.maxX = maxX;
                checkLimitX();
            }

            public void setMinY(int minY) {
                this.minY = minY;
                checkLimitY();
            }

            public void setMaxY(int maxY) {
                this.maxY = maxY;
                checkLimitY();
            }

            private void checkLimitX() {
                if (maxX - minX >= areaSide) {
                    reachedLimitX = true;
                }
            }

            private void checkLimitY() {
                if (maxY - minY >= areaSide) {
                    reachedLimitY = true;
                }
            }
        }
    }

    private static class ImageAnalyzer {
        private ImageMap imageMap;
        private int areaSize;
        private List<IntPoint> queue;
        private boolean[][] checkedMap;

        public ImageAnalyzer(ImageMap imageMap, int areaSize) {
            this.imageMap = imageMap;
            this.areaSize = areaSize;

        }

        public boolean findUsefulNeighbors(int x, int y) {
            imageMap.clearAreaMeasurements();
            boolean isUseful = imageMap.isUseful(x, y);
            if (!isUseful) {
                return false;
            }

            int width = imageMap.getWidth();
            int height = imageMap.getHeight();
            queue = new ArrayList<>();
            checkedMap = new boolean[width][height];

            IntPoint point = new IntPoint(x, y);
            queue.add(point);
            checkedMap[x][y] = true;

            int usefulPointsCounter = 1;
            while (!queue.isEmpty()) {
                point = queue.remove(0);

                IntPoint[] pointsToCheck = new IntPoint[] {
                        new IntPoint(point.x + 1, point.y), new IntPoint(point.x - 1, point.y),
                        new IntPoint(point.x, point.y + 1), new IntPoint(point.x, point.y - 1)
                };

                for (int i = 0; i < pointsToCheck.length; i++) {
                    point = pointsToCheck[i];

                    boolean isOutOfBounds = point.x < 0 || point.x >= width || point.y < 0 || point.y >= height;
                    if (isOutOfBounds) {
                        continue;
                    }

                    boolean isCheckedAlready = checkedMap[point.x][point.y];
                    if (isCheckedAlready) {
                        continue;
                    }
                    checkedMap[point.x][point.y] = true;

                    isUseful = imageMap.isUseful(point.x, point.y);
                    if (isUseful) {
                        queue.add(point);
                        usefulPointsCounter++;
                    }

                    if (usefulPointsCounter >= areaSize) {
                        return true;
                    }
                }
            }

            return false;
        }
    }
}
