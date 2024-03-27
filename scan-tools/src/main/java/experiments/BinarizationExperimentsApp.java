package experiments;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import java.util.stream.Collectors;

import app.utils.FileUtils;
import dto.FileListEntry;
import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import process.context.ApplicationContext;
import process.context.ApplicationEvents;
import process.context.ApplicationParameters;
import process.filelist.FileListNode;
import processing.images.filters.BinarizationFilter;

public class BinarizationExperimentsApp extends Application {
    private static final String APPLICATION_CONTEXT_CONFIG_FILENAME = "binarization-experiments-config.json";

    public static void main(String[] args) {
        launch(args);
    }

    private ApplicationContext applicationContext;

    @FXML
    private TextField thresholdField;
    @FXML
    private TextField redField;
    @FXML
    private TextField greenField;
    @FXML
    private TextField blueField;
    @FXML
    private Button renderButton;

    @FXML
    private SplitPane splitPane;
    @FXML
    private Pane imagePane;
    @FXML
    private ImageView imageView1;
    @FXML
    private ImageView imageView2;

    private File currentFolder;
    private BufferedImage sourceImage;

    private double lastMousePosX;
    private double lastMousePosY;
    private double scale = 0.2;


    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FileUtils.loadFXML(this);

        FileListNode fileList = new FileListNode();
        applicationContext = new ApplicationContext(APPLICATION_CONTEXT_CONFIG_FILENAME);
        Node fileListNode = fileList.init(applicationContext);
        splitPane.getItems().add(fileListNode);

        applicationContext.addEventListener(ApplicationEvents.WorkFolderChanged, value -> handleWorkFolderChanged(value));
        applicationContext.addEventListener(ApplicationEvents.WorkFileSelected, value -> handleSelectWorkFile(value));

        imagePane.setScaleX(scale);
        imagePane.setScaleY(scale);

        imagePane.setOnMousePressed(e -> {
            saveMouseClickPosition(e);
        });

        // Handling dragging event when mouse moved after the button is pressed
        imagePane.setOnMouseDragged(e -> {
            handleImageDrag(e);
            saveMouseClickPosition(e);
        });

        // Handle scaling event
        imagePane.setOnScroll(e -> {
            handleImageScale(e);
        });

        Scene scene = new Scene(root);
        primaryStage.setMaximized(true);
        primaryStage.setScene(scene);
        primaryStage.show();

        restoreComponent();
        centerImage();
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
        String currentImageFilename = fileListEntry.getFilename();
        String uri = new File(currentFolder, currentImageFilename).toURI().toString();
        Image image = new Image(uri);
        imageView1.setImage(image);
        imageView2.setImage(image);

        sourceImage = new BufferedImage((int) image.getWidth(), (int) image.getHeight(), BufferedImage.TYPE_INT_RGB);
        SwingFXUtils.fromFXImage(image, sourceImage);

        // Scale image to fit into the window
        double parentWidth = imagePane.getLayoutBounds().getWidth();
        double parentHeight = imagePane.getLayoutBounds().getHeight();
        double horizontalRatio = parentWidth / image.getWidth();
        double verticalRatio = parentHeight / image.getHeight();
        scale = Math.min(horizontalRatio, verticalRatio);
        imagePane.setScaleX(scale);
        imagePane.setScaleY(scale);

        centerImage();

    }

    private void restoreComponent() {
        String positionsString = applicationContext.getParameterValue(ApplicationParameters.SplitPaneDivider);
        if (positionsString != null) {
            double[] positions = Arrays.stream(positionsString.split(";"))
                    .mapToDouble(s -> Double.parseDouble(s)).toArray();
            splitPane.setDividerPositions(positions);
        }

        // Listener to Save SplitPane Dividers on SplitPane Dividers change
        splitPane.getDividers().forEach(div -> {
            div.positionProperty().addListener(e -> {
                double[] dividerPositions = splitPane.getDividerPositions();
                String dividerPositionsString = Arrays.stream(dividerPositions).boxed()
                        .map(d -> String.valueOf(d))
                        .collect(Collectors.joining(";"));
                applicationContext.setParameterValue(ApplicationParameters.SplitPaneDivider, dividerPositionsString);
            });
        });

        // Restore Working Folder
        String startFolderPath = applicationContext.getParameterValue(ApplicationParameters.StartFolder);
        if (startFolderPath != null) {
            File startFolder = new File(startFolderPath);
            if (startFolder.exists() && startFolder.isDirectory()) {
                applicationContext.fireEvent(ApplicationEvents.WorkFolderChanged, startFolder);
            }
        }
    }

    @FXML
    private void renderImage() {
        renderButton.setDisable(true);
        BufferedImage newImage = new BufferedImage(sourceImage.getWidth(), sourceImage.getHeight(), BufferedImage.TYPE_INT_RGB);

        BinarizationFilter filter = new BinarizationFilter();
        filter.setImage(sourceImage);
        double threshold = Double.parseDouble(thresholdField.getText());
        double rw = Double.parseDouble(redField.getText());
        double gw = Double.parseDouble(greenField.getText());
        double bw = Double.parseDouble(blueField.getText());
        threshold *= rw + gw + bw;
        filter.setWeightRed(rw);
        filter.setWeightGreen(gw);
        filter.setWeightBlue(bw);
        filter.setColorThreshold(threshold);

        for (int y = 0; y < newImage.getHeight(); y++) {
            for (int x = 0; x < newImage.getWidth(); x++) {
                int resultColor = filter.getRGB(x, y);
                newImage.setRGB(x, y, resultColor );
            }
        }

        Image newImageFX = SwingFXUtils.toFXImage(newImage, null);
        imageView2.setImage(newImageFX);

        renderButton.setDisable(false);
    }

    private void handleImageScale(ScrollEvent scrollEvent) {
        double delta = 1.2;
        double oldScale = scale;
        scale = scrollEvent.getDeltaY() < 0 ? oldScale / delta : oldScale * delta;

        // Adjusting shapes after scale changed
        adjustImageScale(scrollEvent, oldScale);
    }

    private void adjustImageScale(ScrollEvent scrollEvent, double oldScale) {
        // Scale Image
        imagePane.setScaleX(scale);
        imagePane.setScaleY(scale);

        // Calculating new coordinates to shift the Image
        Bounds boundsInParent = imagePane.getBoundsInParent();
        double currentMousePosX = scrollEvent.getSceneX();
        double currentMousePosY = scrollEvent.getSceneY();

        double factor = scale / oldScale - 1;
        double deltaX = boundsInParent.getWidth() / 2 + boundsInParent.getMinX() - currentMousePosX;
        double deltaY = boundsInParent.getHeight() / 2 + boundsInParent.getMinY() - currentMousePosY;

        // Shifting the Image that the part of image at cursor position was the same
        imagePane.setTranslateX(imagePane.getTranslateX() + factor * deltaX);
        imagePane.setTranslateY(imagePane.getTranslateY() + factor * deltaY);
    }

    private void handleImageDrag(MouseEvent mouseEvent) {
        double currentMousePosX = mouseEvent.getSceneX();
        double currentMousePosY = mouseEvent.getSceneY();
        double mouseDeltaX = currentMousePosX - lastMousePosX;
        double mouseDeltaY = currentMousePosY - lastMousePosY;

        imagePane.setTranslateX(imagePane.getTranslateX() + mouseDeltaX);
        imagePane.setTranslateY(imagePane.getTranslateY() + mouseDeltaY);
    }

    private void saveMouseClickPosition(MouseEvent mouseEvent) {
        lastMousePosX = mouseEvent.getSceneX();
        lastMousePosY = mouseEvent.getSceneY();
    }

    @FXML
    private void centerImage() {
        // TODO Investigate, why alignment to center of the parent component does not work
        // There is an assumption that whether rectangle or circles sizes are affecting
        imagePane.setTranslateX(imagePane.getTranslateX() - imagePane.getBoundsInParent().getMinX());
        imagePane.setTranslateY(imagePane.getTranslateY() - imagePane.getBoundsInParent().getMinY());
    }
}
