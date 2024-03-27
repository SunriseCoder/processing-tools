package process.processing.automarkup;

import java.io.IOException;

import app.utils.FileUtils;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import process.context.ApplicationContext;
import process.processing.AbstractNode;

public class AutoMarkupNode extends AbstractNode {
    private static final String DEFAULT_THRESHOLD = "30000";
    private static final String DEFAULT_AREA_SIZE = "2000";

    private ApplicationContext applicationContext;

    @FXML
    private TextField thresholdTextField;
    @FXML
    private TextField areaSizeTextField;

    @FXML
    private ProgressBar progressBar;

    protected double progress;

    public Node init(ApplicationContext applicationContext) throws IOException {
        this.applicationContext = applicationContext;
        Parent rootNode = FileUtils.loadFXML(this);
        return rootNode;
    }

    public void initialize() throws Exception {
        thresholdTextField.setText(DEFAULT_THRESHOLD);
        areaSizeTextField.setText(DEFAULT_AREA_SIZE);
    }

    @FXML
    private void startProcessing() throws Exception {
        AutoMarkupManagerTask managerTask = new AutoMarkupManagerTask("Auto Markup");
        managerTask.setApplicationContext(applicationContext);
        managerTask.setProgressBar(progressBar);

        String thresholdText = thresholdTextField.getText();
        int threshold = Integer.parseInt(thresholdText);
        managerTask.setThreshold(threshold);

        String areaSizeText = areaSizeTextField.getText();
        int areaSize = Integer.parseInt(areaSizeText);
        managerTask.setAreaSize(areaSize);

        Thread thread = new Thread(managerTask, managerTask.getName() + " Manager");
        thread.start();
    }
}
