package process.markup;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import app.utils.FileUtils;
import app.utils.MathUtils;
import components.ExtCircle;
import dto.FileListEntry;
import dto.Point;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.Parent;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.TouchEvent;
import javafx.scene.input.TouchPoint;
import javafx.scene.input.ZoomEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import process.context.ApplicationContext;
import process.context.ApplicationEvents;
import process.context.ApplicationParameters;

public class ImageViewer {
    private static final Color CIRCLE_COLOR_ACTIVE = Color.RED;
    private static final Color CIRCLE_COLOR_PASSIVE = Color.BLUE;
    private static final double CIRCLE_RADIUS = 40;
    private static final int CIRCLE_STROKE_WIDTH = 3;

    // Circle names
    private static final String CIRCLE_NAME_BOTTOM_LEFT = "BottomLeft";
    private static final String CIRCLE_NAME_BOTTOM_RIGHT = "BottomRight";
    private static final String CIRCLE_NAME_TOP_RIGHT = "TopRight";
    private static final String CIRCLE_NAME_TOP_LEFT = "TopLeft";

    private ApplicationContext applicationContext;

    private int MarkupRoughFactor = 10;
    private int MarkupRoughModes = 2;
    private boolean sensorControl;
    private Map<String, ExtCircle> circles;
    private File currentFolder;
    private String currentImageFilename;

    private Image image;

    // ImageViewer components
    @FXML
    private Pane imagePane;
    @FXML
    private ImageView imageView;
    @FXML
    private Polygon polygon;
    @FXML
    private Rectangle rectangle;

    // File operations components
    @FXML
    private TextField openFolderTextField;

    private ExtCircle currentCircle;

    private double lastPosX;
    private double lastPosY;
    private double scale = 1;
    private int roughMarkupMode;

    private boolean mouseTrace = false;
    private boolean touchTrace = false;
    private boolean zoomInProgress = false;

    public Parent init(ApplicationContext applicationContext) throws Exception {
        this.applicationContext = applicationContext;

        Parent root = FileUtils.loadFXML(this);

        // 4 circles to define points of image crop
        circles = createCircles();
        imagePane.getChildren().addAll(circles.values());

        // Saving mouse position when the button was pressed
        imagePane.setOnMousePressed(e -> handleMousePressedEvent(e));

        // Saving mouse position when Sensor was touched
        imagePane.setOnTouchPressed(e -> handleTouchPressedEvent(e));

        // Handling dragging event when mouse moved after the button is pressed
        imagePane.setOnMouseDragged(e -> {
            if (sensorControl) {
                return;
            }

            if (mouseTrace) {
                System.out.println(e);
            }

            handleImageOrCircleDrag(e);
            savePointerPosition(e.getSceneX(), e.getSceneY());
        });

        // Handling dragging event by Sensor Touch
        imagePane.setOnTouchMoved(e -> handleImageOrCircleDrag(e));

        // Handle scaling event by Mouse Scroll
        imagePane.setOnScroll(e -> handleImageScale(e));

        //imagePane.addEventHandler(ZoomEvent.ZOOM, e -> System.out.println(e));
        imagePane.setOnZoomStarted(e -> zoomInProgress = true);
        imagePane.setOnZoom(e -> handleImageScale(e));
        imagePane.setOnZoomFinished(e -> zoomInProgress = false);

        // Moving image (or circles) with the keyboard
        imagePane.setOnKeyPressed(e -> handleMoveViaKeyboard(e));

        applicationContext.addEventListener(ApplicationEvents.CenterImage, e -> centerImage());
        applicationContext.addEventListener(ApplicationEvents.SaveImage, e -> saveImage());

        applicationContext.addEventListener(ApplicationEvents.SensorControl, value -> this.sensorControl = (boolean) value);

        applicationContext.addEventListener(ApplicationEvents.WorkFolderChanged, value -> handleWorkFolderChanged(value));
        applicationContext.addEventListener(ApplicationEvents.WorkFileSelected, value -> handleSelectWorkFile(value));

        return root;
    }

    public void initialize() {
        String markupRoughModesString = applicationContext.getParameterValue(ApplicationParameters.MarkupRoughModes);
        if (markupRoughModesString == null) {
            applicationContext.setParameterValue(ApplicationParameters.MarkupRoughModes, String.valueOf(MarkupRoughModes));
        } else {
            MarkupRoughModes = Integer.parseInt(markupRoughModesString);
        }
    }

    private void handleTouchPressedEvent(TouchEvent e) {
        if (touchTrace) {
            System.out.println(e);
        }

        TouchPoint point = e.getTouchPoint();
        savePointerPosition(point.getSceneX(), point.getSceneY());
    }

    private void handleMousePressedEvent(MouseEvent e) {
        if (sensorControl) {
            return;
        }

        if (mouseTrace) {
            System.out.println(e);
        }

        savePointerPosition(e.getSceneX(), e.getSceneY());
    }

    private void savePointerPosition(double x, double y) {
        lastPosX = x;
        lastPosY = y;
        imagePane.requestFocus();
        setCurrentCircle();
    }

    private void handleWorkFolderChanged(Object value) {
        File newFolder = (File) value;
        currentFolder = newFolder;
    }

    private void handleSelectWorkFile(Object value) {
        if (value == null) {
            return;
        }

        FileListEntry fileListEntry = (FileListEntry) value;
        currentImageFilename = fileListEntry.getFilename();
        String uri = new File(currentFolder, currentImageFilename).toURI().toString();
        image = new Image(uri);
        imageView.setImage(image);

        // Scale image to fit into the window
        double parentWidth = imagePane.getLayoutBounds().getWidth() - CIRCLE_RADIUS * 2 - CIRCLE_STROKE_WIDTH * 2;
        double parentHeight = imagePane.getLayoutBounds().getHeight() - CIRCLE_RADIUS * 2 - CIRCLE_STROKE_WIDTH * 2;
        double horizontalRatio = parentWidth / image.getWidth();
        double verticalRatio = parentHeight / image.getHeight();
        scale = Math.min(horizontalRatio, verticalRatio);
        imagePane.setScaleX(scale);
        imagePane.setScaleY(scale);

        adjustRectangleScale();
        adjustCirclesScale();
        adjustCirclePositions();

        centerImage();

        roughMarkupMode = MarkupRoughModes - 1;
        setCurrentCircle(circles.get(CIRCLE_NAME_TOP_LEFT));
    }

    private Map<String, ExtCircle> createCircles() {
        Map<String, ExtCircle> circles = new LinkedHashMap<>();
        circles.put(CIRCLE_NAME_TOP_LEFT, createCircle(CIRCLE_NAME_TOP_LEFT));
        circles.put(CIRCLE_NAME_TOP_RIGHT, createCircle(CIRCLE_NAME_TOP_RIGHT));
        circles.put(CIRCLE_NAME_BOTTOM_RIGHT, createCircle(CIRCLE_NAME_BOTTOM_RIGHT));
        circles.put(CIRCLE_NAME_BOTTOM_LEFT, createCircle(CIRCLE_NAME_BOTTOM_LEFT));

        linkCircles(circles);
        return circles;
    }

    private ExtCircle createCircle(String name) {
        ExtCircle circle = new ExtCircle(name, 0, 0, CIRCLE_RADIUS);
        circle.setFill(null);
        circle.setStroke(CIRCLE_COLOR_PASSIVE);
        circle.setStrokeWidth(CIRCLE_STROKE_WIDTH);
        circle.centerXProperty().addListener(e -> adjustPolygonBoundaries());
        circle.centerYProperty().addListener(e -> adjustPolygonBoundaries());
        return circle;
    }

    private void linkCircles(Map<String, ExtCircle> circles) {
        List<ExtCircle> list = new ArrayList<>(circles.values());
        ExtCircle firstCircle = list.get(0);
        ExtCircle lastCircle = null;
        for (int i = 0; i < list.size(); i++) {
            ExtCircle currentCircle = list.get(i);
            if (lastCircle != null) {
                currentCircle.previous = lastCircle;
                lastCircle.next = currentCircle;
            }
            lastCircle = currentCircle;
        }
        firstCircle.previous = lastCircle;
        lastCircle.next = firstCircle;
    }

    private ExtCircle findCircle(double x, double y) {
        for (ExtCircle circle : circles.values()) {
            double circleCenterX = circle.getCenterX();
            double circleCenterY = circle.getCenterY();

            double currentRange = MathUtils.calculateHypotenuse(x - circleCenterX, y - circleCenterY);
            double visibleRadius = circle.getRadius() / scale;

            if (currentRange <= visibleRadius) {
                return circle;
            }
        }
        return null;
    }

    /**
     * Adjusting scale of circles that they would have the same visual size
     */
    private void adjustCirclesScale() {
        circles.values().forEach(circle -> {
            circle.setScaleX(1 / scale);
            circle.setScaleY(1 / scale);
        });
    }

    /**
     * Adjust image size and position after scale
     *
     * @param scrollEvent
     * @param oldScale
     */
    private void adjustImageScale(double currentPosX, double currentPosY, double oldScale) {
        // Scale Image
        imagePane.setScaleX(scale);
        imagePane.setScaleY(scale);

        // Calculating new coordinates to shift the Image
        Bounds boundsInParent = imagePane.getBoundsInParent();

        double factor = scale / oldScale - 1;
        double deltaX = boundsInParent.getWidth() / 2 + boundsInParent.getMinX() - currentPosX;
        double deltaY = boundsInParent.getHeight() / 2 + boundsInParent.getMinY() - currentPosY;

        // Shifting the Image that the part of image at cursor position was the same
        imagePane.setTranslateX(imagePane.getTranslateX() + factor * deltaX);
        imagePane.setTranslateY(imagePane.getTranslateY() + factor * deltaY);
    }

    /**
     * Adjusting scale of the Rectangle
     */
    private void adjustRectangleScale() {
        rectangle.setX(0 - CIRCLE_RADIUS * 2 / scale);
        rectangle.setY(0 - CIRCLE_RADIUS * 2 / scale);
        rectangle.setWidth(image.getWidth() + CIRCLE_RADIUS * 4 / scale);
        rectangle.setHeight(image.getHeight() + CIRCLE_RADIUS * 4 / scale);
    }

    /**
     * Adjusting the Polygon's boundaries
     */
    private void adjustPolygonBoundaries() {
        polygon.getPoints().clear();

        List<Double> points = new ArrayList<>();
        circles.values().forEach(c -> {
            points.add(c.getCenterX());
            points.add(c.getCenterY());
        });

        polygon.getPoints().addAll(points);
    }

    private void setCurrentCircle() {
        double posOnImageX = getImageCoordinateX(lastPosX);
        double posOnImageY = getImageCoordinateY(lastPosY);

        ExtCircle foundCircle = findCircle(posOnImageX, posOnImageY);
        setCurrentCircle(foundCircle);
    }

    private void setCurrentCircle(ExtCircle foundCircle) {
        if (foundCircle != null) {
            changeCircle(foundCircle);
        }
    }

    private void handleImageOrCircleDrag(MouseEvent event) {
        handleImageOrCircleDrag(event.getSceneX(), event.getSceneY());
    }

    private void handleImageOrCircleDrag(TouchEvent event) {
        if (zoomInProgress) {
            return;
        }

        if (touchTrace) {
            System.out.println(event);
        }

        TouchPoint point = event.getTouchPoint();
        double currentPosX = point.getSceneX();
        double currentPosY = point.getSceneY();
        handleImageOrCircleDrag(currentPosX, currentPosY);
    }

    private void handleImageOrCircleDrag(double currentPosX, double currentPosY) {
        // If image is not initialized yet
        if (image == null) {
            return;
        }

        // Calculating position of the Circle on the Image
        double posOnImageX = getImageCoordinateX(lastPosX);
        double posOnImageY = getImageCoordinateY(lastPosY);

        // Looking for the Circle
        ExtCircle circle = findCircle(posOnImageX, posOnImageY);

        if (circle == null) {
            handleImageDrag(currentPosX, currentPosY);
        } else {
            handleCircleDrag(circle, currentPosX, currentPosY);
        }

        savePointerPosition(currentPosX, currentPosY);
    }

    private void handleImageDrag(double currentPosX, double currentPosY) {
        double deltaX = currentPosX - lastPosX;
        double deltaY = currentPosY - lastPosY;

        imagePane.setTranslateX(imagePane.getTranslateX() + deltaX);
        imagePane.setTranslateY(imagePane.getTranslateY() + deltaY);
    }

    private void handleCircleDrag(ExtCircle circle, double currentMousePosX, double currentMousePosY) {
        // Calculating new position of the Circle
        double newPositionX = getImageCoordinateX(currentMousePosX);
        double newPositionY = getImageCoordinateY(currentMousePosY);

        // Validation and adjustment of the coordinates
        newPositionX = adjustNewCirclePositionX(circle, newPositionX);
        newPositionY = adjustNewCirclePositionY(circle, newPositionY);

        // Applying coordinates
        circle.setCenterX(newPositionX);
        circle.setCenterY(newPositionY);
    }

    private void handleImageScale(ZoomEvent e) {
        if (touchTrace) {
            System.out.println(e);
        }

        double currentPosX = e.getSceneX();
        double currentPosY = e.getSceneY();
        double factor = e.getZoomFactor();

        handleImageScale(currentPosX, currentPosY, factor);
    }

    private void handleImageScale(ScrollEvent event) {
        if (sensorControl) {
            return;
        }

        if (mouseTrace) {
            System.out.println(event);
        }

        double currentPosX = event.getSceneX();
        double currentPosY = event.getSceneY();
        double delta = 1.2;
        double factor = event.getDeltaY() < 0 ? 1 / delta : delta;

        handleImageScale(currentPosX, currentPosY, factor);
    }

    private void handleImageScale(double currentPosX, double currentPosY, double factor) {
        double oldScale = scale;
        scale = oldScale * factor;

        // Adjusting shapes after scale changed
        adjustImageScale(currentPosX, currentPosY, oldScale);
        adjustCirclesScale();
        adjustRectangleScale();
    }

    private void handleMoveViaKeyboard(KeyEvent e) {
        e.consume();

        if (currentImageFilename == null || currentCircle == null) {
            return;
        }

        int step = (int) Math.round(1 / scale);
        step = MathUtils.adjustValue(step, 1, 100);
        step *= Math.pow(MarkupRoughFactor, roughMarkupMode);

        double newX, newY;
        switch (e.getCode()) {
            case RIGHT:
            case D:
                newX = currentCircle.getCenterX() + step;
                newX = adjustNewCirclePositionX(currentCircle, newX);
                currentCircle.setCenterX(newX);
                break;
            case LEFT:
            case A:
                newX = currentCircle.getCenterX() - step;
                newX = adjustNewCirclePositionX(currentCircle, newX);
                currentCircle.setCenterX(newX);
                break;
            case UP:
            case W:
                newY = currentCircle.getCenterY() - step;
                newY = adjustNewCirclePositionY(currentCircle, newY);
                currentCircle.setCenterY(newY);
                break;
            case DOWN:
            case S:
                newY = currentCircle.getCenterY() + step;
                newY = adjustNewCirclePositionY(currentCircle, newY);
                currentCircle.setCenterY(newY);
                break;
            case Q:
            case Z:
                changeCircle(currentCircle.previous);
                break;
            case E:
            case X:
                changeCircle(currentCircle.next);
                break;
            case ENTER:
            case F:
                saveImage();
                break;
            case R:
            case SHIFT:
                if (--roughMarkupMode < 0) {
                    roughMarkupMode = MarkupRoughModes - 1;
                }
            default:
                // Ignore unsupported KeyCode
        }
    }

    private void changeCircle(ExtCircle next) {
        if (currentCircle != null) {
            currentCircle.setStroke(CIRCLE_COLOR_PASSIVE);
        }
        next.setStroke(CIRCLE_COLOR_ACTIVE);
        currentCircle = next;
    }

    private double getImageCoordinateX(double screenCoordinate) {
        double sceneOffset = imagePane.getBoundsInParent().getMinX();
        double rectangleOffset = imagePane.getBoundsInLocal().getMinX();
        double result = screenCoordinate / scale - sceneOffset / scale + rectangleOffset;
        return result;
    }

    private double getImageCoordinateY(double screenCoordinate) {
        double sceneOffset = imagePane.getBoundsInParent().getMinY();
        double rectangleOffset = imagePane.getBoundsInLocal().getMinY();
        double result = screenCoordinate / scale - sceneOffset / scale + rectangleOffset;
        return result;
    }

    private double adjustNewCirclePositionX(ExtCircle circle, double newX) {
        // Checking that position X is not outside the Image width
        double imageWidth = image.getWidth();
        newX = newX >= imageWidth ? imageWidth - 1 : newX;
        newX = newX < 0 ? 0 : newX;

        switch (circle.getName()) {
            case CIRCLE_NAME_TOP_LEFT:
            case CIRCLE_NAME_BOTTOM_LEFT:
                // Checking that Left Circles don't have position X bigger than Right Circles
                double topRightX = circles.get(CIRCLE_NAME_TOP_RIGHT).getCenterX();
                double bottomRightX = circles.get(CIRCLE_NAME_BOTTOM_RIGHT).getCenterX();
                newX = newX > topRightX ? topRightX : newX;
                newX = newX > bottomRightX ? bottomRightX : newX;
                break;
            case CIRCLE_NAME_TOP_RIGHT:
            case CIRCLE_NAME_BOTTOM_RIGHT:
                // Checking that Right Circles don't have position X smaller than Left Circles
                double topLeftX = circles.get(CIRCLE_NAME_TOP_LEFT).getCenterX();
                double bottomLeftX = circles.get(CIRCLE_NAME_BOTTOM_LEFT).getCenterX();
                newX = newX < topLeftX ? topLeftX : newX;
                newX = newX < bottomLeftX ? bottomLeftX : newX;
                break;
        }

        return newX;
    }

    private double adjustNewCirclePositionY(ExtCircle circle, double newY) {
        // Checking that position Y is not outside the Image height
        double imageHeight = image.getHeight();
        newY = newY >= imageHeight ? imageHeight - 1 : newY;
        newY = newY < 0 ? 0 : newY;

        switch (circle.getName()) {
            case CIRCLE_NAME_TOP_LEFT:
            case CIRCLE_NAME_TOP_RIGHT:
                // Checking that Top Circles don't have position Y bigger than Bottom Circles
                double bottomRightY = circles.get(CIRCLE_NAME_BOTTOM_RIGHT).getCenterY();
                double bottomLeftY = circles.get(CIRCLE_NAME_BOTTOM_LEFT).getCenterY();
                newY = newY > bottomRightY ? bottomRightY : newY;
                newY = newY > bottomLeftY ? bottomLeftY : newY;
                break;
            case CIRCLE_NAME_BOTTOM_RIGHT:
            case CIRCLE_NAME_BOTTOM_LEFT:
                // Checking that Bottom Circles don't have position Y smaller than Top Circles
                double topLeftY = circles.get(CIRCLE_NAME_TOP_LEFT).getCenterY();
                double topRightY = circles.get(CIRCLE_NAME_TOP_RIGHT).getCenterY();
                newY = newY < topLeftY ? topLeftY : newY;
                newY = newY < topRightY ? topRightY : newY;
                break;
        }

        return newY;
    }

    private void adjustCirclePositions() {
        List<Point> selectionBoundaries = applicationContext.getSelectionBoundaries(currentFolder, currentImageFilename);
        if (selectionBoundaries == null) {
            circles.get(CIRCLE_NAME_TOP_LEFT).setCenter(0, 0);
            circles.get(CIRCLE_NAME_TOP_RIGHT).setCenter(image.getWidth(), 0);
            circles.get(CIRCLE_NAME_BOTTOM_RIGHT).setCenter(image.getWidth(), image.getHeight());
            circles.get(CIRCLE_NAME_BOTTOM_LEFT).setCenter(0, image.getHeight());
        } else {
            Iterator<Point> iterator = selectionBoundaries.iterator();
            circles.values().forEach(circle -> {
                Point point = iterator.next();
                circle.setCenter(point.x, point.y);
            });
        }
    }

    private void centerImage() {
        // TODO Investigate, why alignment to center of the parent component does not work
        imagePane.setTranslateX(imagePane.getTranslateX() - imagePane.getBoundsInParent().getMinX() - CIRCLE_RADIUS + CIRCLE_STROKE_WIDTH);
        imagePane.setTranslateY(imagePane.getTranslateY() - imagePane.getBoundsInParent().getMinY() - CIRCLE_RADIUS + CIRCLE_STROKE_WIDTH);
    }

    private void saveImage() {
        try {
            trySaveImage();
            // TODO Rewrite it with button disabled and enabled when needed
            // I.e. by default disabled, by select image enabled, by refresh file list disabled
            // Be aware to enable button due to exception
            applicationContext.fireEvent(ApplicationEvents.WorkFolderRefresh, null);
            applicationContext.fireEvent(ApplicationEvents.WorkFileSelectNext, null);
        } catch (IOException e) {
            applicationContext.showError("Error due to save Image", e);
        }
    }

    private void trySaveImage() throws IOException {
        if (image == null) {
            return;
        }

        // Save boundaries for the Image to the log file
        List<Point> selectionBoundaries = extractBoundaries();
        saveBoundaries(selectionBoundaries);
    }

    private List<Point> extractBoundaries() {
        List<Point> extractedPoints = circles.values().stream()
                .map(circle -> new Point(circle.getCenterX(), circle.getCenterY()))
                .collect(Collectors.toList());
        return extractedPoints;
    }

    private void saveBoundaries(List<Point> selectionBoundaries) {
        if (currentImageFilename != null) {
            applicationContext.saveSelectionBoundaries(currentFolder, currentImageFilename, selectionBoundaries);
        }
    }
}
