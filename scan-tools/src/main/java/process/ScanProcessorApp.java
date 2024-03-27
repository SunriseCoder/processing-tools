package process;

import java.io.File;
import java.util.Arrays;
import java.util.stream.Collectors;

import app.utils.FileUtils;
import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.SplitPane;
import javafx.stage.Stage;
import process.context.ApplicationContext;
import process.context.ApplicationEvents;
import process.context.ApplicationParameters;
import process.filelist.FileListNode;
import process.markup.ImageViewer;
import process.processing.ProcessingNode;

public class ScanProcessorApp extends Application {
    private static final String APPLICATION_CONTEXT_CONFIG_FILENAME = "scan-processor-config.json";

    public static void main(String[] args) {
        launch(args);
    }

    private ApplicationContext applicationContext;

    @FXML
    private SplitPane splitPane;

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Application Context
        applicationContext = new ApplicationContext(APPLICATION_CONTEXT_CONFIG_FILENAME);

        // Root UI Node
        Parent root = FileUtils.loadFXML(this);

        // Main Scene
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Scan Processor");
        primaryStage.setMaximized(true);
        primaryStage.show();

        // Image View and Markup
        ImageViewer imageViewer = new ImageViewer();
        Node imageViewerRoot = imageViewer.init(applicationContext);
        splitPane.getItems().add(imageViewerRoot);

        // Processing
        ProcessingNode processing = new ProcessingNode();
        Node processingNode = processing.init(applicationContext);
        splitPane.getItems().add(processingNode);

        // FileList
        FileListNode fileListNode = new FileListNode();
        Node fileListRoot = fileListNode.init(applicationContext);
        splitPane.getItems().add(fileListRoot);

        // Restore sizes of Visual Components (Panes, Windows, etc)
        restoreComponent();
    }

    private void restoreComponent() {
        // Restore SplitPane Dividers
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
            startFolder = FileUtils.getExistingParentIfFolderNotExists(startFolder);
            if (startFolder.exists() && startFolder.isDirectory()) {
                applicationContext.fireEvent(ApplicationEvents.WorkFolderChanged, startFolder);
            }
        }
    }
}
