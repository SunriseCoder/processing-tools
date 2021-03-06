package process.processing.actions;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import process.ApplicationContext;
import process.ApplicationContext.Events;
import utils.FileUtils;
import utils.ThreadUtils;

public class ActionsNode {
    private ApplicationContext applicationContext;

    @FXML
    private Button saveButton;

    @FXML
    private ToggleButton sensorToggleButton;

    public Node init(ApplicationContext applicationContext) throws IOException {
        this.applicationContext = applicationContext;
        Parent rootNode = FileUtils.loadFXML(this);
        return rootNode;
    }

    @FXML
    private void handleCenterImage() {
        applicationContext.fireEvent(Events.CenterImage, null);
    }

    @FXML
    private void handleSaveImage() {
        saveButton.setDisable(true);
        applicationContext.fireEvent(Events.SaveImage, null);
        ThreadUtils.runLaterAfterSleep(3000, () -> saveButton.setDisable(false));
    }

    @FXML
    private void handleToggleSensor() {
        boolean selected = sensorToggleButton.isSelected();
        applicationContext.fireEvent(Events.SensorControl, selected);
    }
}
