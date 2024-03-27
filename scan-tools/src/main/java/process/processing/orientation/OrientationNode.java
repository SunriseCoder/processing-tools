package process.processing.orientation;

import java.io.IOException;

import app.utils.FileUtils;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ProgressBar;
import process.context.ApplicationContext;
import process.processing.AbstractNode;
import processing.images.rotation.AbstractOrientationRotate;
import processing.images.rotation.RotationAll90DegreesClockWise;
import processing.images.rotation.RotationAll90DegreesCounterClockWise;
import processing.images.rotation.RotationOdd180Degrees;

public class OrientationNode extends AbstractNode {
    private ApplicationContext applicationContext;

    @FXML
    private ComboBox<RotationMethods> rotationComboBox;

    @FXML
    private ProgressBar progressBar;

    protected double progress;

    public Node init(ApplicationContext applicationContext) throws IOException {
        this.applicationContext = applicationContext;
        Parent rootNode = FileUtils.loadFXML(this);
        return rootNode;
    }

    public void initialize() throws Exception {
        initComboBox(rotationComboBox, RotationMethodsListCell.class, RotationMethods.values());
    }

    @FXML
    private void startProcessing() throws Exception {
        OrientationManagerTask managerTask = new OrientationManagerTask("Change Orientation");
        managerTask.setApplicationContext(applicationContext);
        managerTask.setProgressBar(progressBar);

        RotationMethods rotationMethod = rotationComboBox.getSelectionModel().getSelectedItem();
        Class<? extends AbstractOrientationRotate> rotationMethodClass = rotationMethod.getCl();
        managerTask.setRotationMethodClass(rotationMethodClass);

        Thread thread = new Thread(managerTask, managerTask.getName() + " Manager");
        thread.start();
    }

    private enum RotationMethods {
        All90DegreesCounterClockWise("All pages 90 Degrees Counter-Clock-Wise", RotationAll90DegreesCounterClockWise.class),
        Odd180Degrees("Odd pages 180 Degrees", RotationOdd180Degrees.class),
        All90DegreesClockWise("All pages 90 Degrees Clock-Wise", RotationAll90DegreesClockWise.class);

        private String text;
        private Class<? extends AbstractOrientationRotate> cl;

        private RotationMethods(String text, Class<? extends AbstractOrientationRotate> cl) {
            this.text = text;
            this.cl = cl;
        }

        public String getText() {
            return text;
        }

        public Class<? extends AbstractOrientationRotate> getCl() {
            return cl;
        }
    }

    public static class RotationMethodsListCell extends ListCell<RotationMethods> {
        @Override
        protected void updateItem(RotationMethods item, boolean empty) {
            super.updateItem(item, empty);
            setText(item == null ? null : item.getText());
        }
    }
}
