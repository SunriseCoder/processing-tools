package process.processing.rotateAndCrop;

import java.io.IOException;

import app.utils.FileUtils;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ProgressBar;
import process.context.ApplicationContext;
import process.handlers.SmoothFilters;
import process.processing.AbstractNode;
import processing.images.filters.AbstractImageFilter;

public class RotateAndCropNode extends AbstractNode {
    private ApplicationContext applicationContext;

    @FXML
    private ComboBox<SmoothFilters> smoothFilterComboBox;

    @FXML
    private CheckBox imageRotateCheckBox;

    @FXML
    private CheckBox imageCropCheckBox;

    @FXML
    private ProgressBar progressBar;

    protected double progress;

    public Node init(ApplicationContext applicationContext) throws IOException {
        this.applicationContext = applicationContext;
        Parent rootNode = FileUtils.loadFXML(this);
        return rootNode;
    }

    public void initialize() throws Exception {
        initComboBox(smoothFilterComboBox, SmoothFilterListCell.class, SmoothFilters.values());
    }

    @FXML
    private void startProcessing() throws Exception {
        // Reload Markup Data from the file in current folder in case of external changes
        applicationContext.reloadSelectionBoundaries(applicationContext.getWorkFolder());

        RotateAndCropManagerTask managerTask = new RotateAndCropManagerTask("Rotate and Crop");
        managerTask.setApplicationContext(applicationContext);
        managerTask.setProgressBar(progressBar);

        boolean needRotate = imageRotateCheckBox.isSelected();
        managerTask.setNeedRotate(needRotate);

        boolean needCrop = imageCropCheckBox.isSelected();
        managerTask.setNeedCrop(needCrop);

        SmoothFilters selectedSmoothFilter = smoothFilterComboBox.getSelectionModel().getSelectedItem();
        Class<? extends AbstractImageFilter> smoothFilterClass = selectedSmoothFilter.getCl();
        managerTask.setSmoothFilterClass(smoothFilterClass);

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
