package process;

import java.io.File;
import java.io.IOException;

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
import process.player.AudioPlayer;
import utils.FileUtils;

public class AnalyzerApp extends Application {
    private static final String APPLICATION_CONTEXT_CONFIG_FILENAME = "subtitles-processor-config.json";

    public static void main(String[] args) {
        launch(args);
    }

    private ApplicationContext applicationContext;

    // General
    @FXML
    private SplitPane verticalSplitPane;

    private AudioPlayer audioPlayer;
//    private AnalyzerForm analyzerForm;

    public AnalyzerApp() {
        audioPlayer = new AudioPlayer();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Application Context
        applicationContext = new ApplicationContext(APPLICATION_CONTEXT_CONFIG_FILENAME);
        applicationContext.setStage(primaryStage);

        // Root UI Node
        Parent root = FileUtils.loadFXML(this);

        // AudioPlayer
        Node audioPlayerNode = audioPlayer.createUI(applicationContext);
        verticalSplitPane.getItems().add(0, audioPlayerNode);

        // Main Scene
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Audio Analyzer");
        primaryStage.setMaximized(true);
        primaryStage.show();

        // Restore Parameters Visual Components (Panes, Windows, etc)
        restoreComponents();
//        analyzerForm.restoreComponents();
    }

    private void restoreComponents() throws IOException {
        // AudioPlayer Components
        audioPlayer.restoreComponents();

        // Work Folders
        String workMediaFilePath = applicationContext.getParameterValue(ApplicationParameters.MediaWorkFile);
        if (workMediaFilePath != null) {
            File workMediaFile = new File(workMediaFilePath);
            if (workMediaFile.exists() && !workMediaFile.isDirectory()) {
                applicationContext.fireEvent(ApplicationEvents.WorkMediaFileChanged, workMediaFile);
            }
        }
    }
}
