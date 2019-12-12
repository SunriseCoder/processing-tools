package process.processing;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import process.context.ApplicationContext;
import process.context.ApplicationEvents;
import process.processing.actions.ActionsNode;
import process.processing.orientation.OrientationNode;
import process.processing.render.RenderNode;
import process.processing.reorder.ReorderNode;
import process.processing.rotateAndCrop.RotateAndCropNode;
import utils.FileUtils;
import utils.ThreadUtils;

public class ProcessingNode {
    private ApplicationContext applicationContext;

    @FXML
    private GridPane processingTabGridPane;

    @FXML
    private Button saveButton;

    public Node init(ApplicationContext applicationContext) throws IOException {
        this.applicationContext = applicationContext;

        Parent node = FileUtils.loadFXML(this);

        applicationContext.addEventListener(ApplicationEvents.SensorControl, value -> saveButton.setVisible((boolean) value));
        int rowIndex = 0;

        // Actions
        ActionsNode actions = new ActionsNode();
        Node actionsNode = actions.init(applicationContext);
        GridPane.setRowIndex(actionsNode, rowIndex++);
        processingTabGridPane.getChildren().add(actionsNode);

        // Orientation
        OrientationNode orientation = new OrientationNode();
        Node orientationNode = orientation.init(applicationContext);
        GridPane.setRowIndex(orientationNode, rowIndex++);
        processingTabGridPane.getChildren().add(orientationNode);

        // Rotate and Crop
        RotateAndCropNode rotateAndCrop = new RotateAndCropNode();
        Node rotateAndCropNode = rotateAndCrop.init(applicationContext);
        GridPane.setRowIndex(rotateAndCropNode, rowIndex++);
        processingTabGridPane.getChildren().add(rotateAndCropNode);

        // Reorder
        ReorderNode prepare = new ReorderNode();
        Node reorderNode = prepare.init(applicationContext);
        GridPane.setRowIndex(reorderNode, rowIndex++);
        processingTabGridPane.getChildren().add(reorderNode);

        // Render
        RenderNode render = new RenderNode();
        Node renderNode = render.init(applicationContext);
        GridPane.setRowIndex(renderNode, rowIndex++);
        processingTabGridPane.getChildren().add(renderNode);

        return node;
    }

    @FXML
    private void handleSave() {
        saveButton.setDisable(true);
        applicationContext.fireEvent(ApplicationEvents.SaveImage, null);
        ThreadUtils.runLaterAfterSleep(3000, () -> saveButton.setDisable(false));
    }
}
