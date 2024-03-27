package process.processing.render;

import java.io.IOException;

import app.utils.FileUtils;
import dto.TaskParameters;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import process.context.ApplicationContext;
import process.handlers.SmoothFilters;
import process.processing.AbstractNode;

public class RenderNode extends AbstractNode {
    private static final String DEFAULT_RESIZE_SOURCE_DPI = "600";
    private static final String DEFAULT_RESIZE_TARGET_DPI = "400";

    private static final String DEFAULT_BINARIZATION_THRESHOLD = "100000";
    private static final String DEFAULT_BINARIZATION_WEIGHT_RED = "3";
    private static final String DEFAULT_BINARIZATION_WEIGHT_GREEN = "1";
    private static final String DEFAULT_BINARIZATION_WEIGHT_BLUE = "1";

    private ApplicationContext applicationContext;

    @FXML
    private GridPane gridPane;

    @FXML
    private ComboBox<SmoothFilters> smoothFilterComboBox;

    @FXML
    private CheckBox imageResizeCheckBox;
    @FXML
    private TextField sourceDPITextField;
    @FXML
    private TextField targetDPITextField;

    @FXML
    private CheckBox imageBinarizationCheckBox;
    @FXML
    private TextField thresholdTextField;
    @FXML
    private TextField weightRedTextField;
    @FXML
    private TextField weightGreenTextField;
    @FXML
    private TextField weightBlueTextField;

    @FXML
    private ProgressBar progressBar;

    public Node init(ApplicationContext applicationContext) throws IOException {
        this.applicationContext = applicationContext;
        Parent rootNode = FileUtils.loadFXML(this);
        return rootNode;
    }

    public void initialize() throws Exception {
        initComboBox(smoothFilterComboBox, SmoothFilterListCell.class, SmoothFilters.values());

        sourceDPITextField.setText(DEFAULT_RESIZE_SOURCE_DPI);
        targetDPITextField.setText(DEFAULT_RESIZE_TARGET_DPI);

        thresholdTextField.setText(DEFAULT_BINARIZATION_THRESHOLD);
        weightRedTextField.setText(DEFAULT_BINARIZATION_WEIGHT_RED);
        weightGreenTextField.setText(DEFAULT_BINARIZATION_WEIGHT_GREEN);
        weightBlueTextField.setText(DEFAULT_BINARIZATION_WEIGHT_BLUE);
    }

    @FXML
    private void startProcessing() {
        RenderManagerTask managerTask = new RenderManagerTask("Render Images");
        managerTask.setApplicationContext(applicationContext);
        managerTask.setProgressBar(progressBar);

        TaskParameters parameters = new TaskParameters();

        SmoothFilters selectedSmoothFilter = smoothFilterComboBox.getSelectionModel().getSelectedItem();
        parameters.setClass(RenderTask.SMOOTH_FILTER_CLASS, selectedSmoothFilter.getCl());

        boolean needResize = imageResizeCheckBox.isSelected();
        parameters.setBoolean(RenderTask.NEED_RESIZE, needResize);

        int sourceDPI = Integer.parseInt(sourceDPITextField.getText());
        parameters.setInt(RenderTask.SOURCE_DPI, sourceDPI);

        int targetDPI = Integer.parseInt(targetDPITextField.getText());
        parameters.setInt(RenderTask.TARGET_DPI, targetDPI);

        boolean needBinarization = imageBinarizationCheckBox.isSelected();
        parameters.setBoolean(RenderTask.NEED_BINARIZATION, needBinarization);

        double weightRed = Double.parseDouble(weightRedTextField.getText());
        parameters.setDouble(RenderTask.WEIGHT_RED, weightRed);

        double weightGreen = Double.parseDouble(weightGreenTextField.getText());
        parameters.setDouble(RenderTask.WEIGHT_GREEN, weightGreen);

        double weightBlue = Double.parseDouble(weightBlueTextField.getText());
        parameters.setDouble(RenderTask.WEIGHT_BLUE, weightBlue);

        double colorThreshold = Double.parseDouble(thresholdTextField.getText());
        parameters.setDouble(RenderTask.COLOR_THRESHOLD, colorThreshold);

        managerTask.setTaskParameters(parameters);

        Thread thread = new Thread(managerTask, managerTask.getName() + " Manager");
        thread.start();
    }

    public static class SmoothFilterListCell extends ListCell<SmoothFilters> {
        @Override
        protected void updateItem(SmoothFilters item, boolean empty) {
            super.updateItem(item, empty);
            setText(item == null ? null : item.getText());
        }
    }
}
