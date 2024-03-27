package process;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import app.utils.FileUtils;
import app.utils.StringUtils;
import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyEvent;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import process.context.ApplicationContext;
import process.context.ApplicationEvents;
import process.context.ApplicationParameters;
import process.dto.SubtitleDTO;
import process.dto.SubtitleTimeDTO;
import process.player.AudioPlayer;
import process.player.AudioPlayerSelection;
import process.subtitles.SubtitlesForm;

public class SubtitlesProcessorApp extends Application {
    private static final String APPLICATION_CONTEXT_CONFIG_FILENAME = "subtitles-processor-config.json";

    public static void main(String[] args) {
        launch(args);
    }

    private ApplicationContext applicationContext;

    // General
    @FXML
    private SplitPane verticalSplitPane;
    @FXML
    private SplitPane horizontalSplitPane;

    // TextEditor
    @FXML
    private TextArea textEditor;
    @FXML
    private Line lineSizeLine;

    private AudioPlayer audioPlayer;
    private SubtitlesForm subtitlesForm;

    public SubtitlesProcessorApp() {
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

        // TextEditor
        textEditor.setOnKeyPressed(e -> handleTextEditorKeyPressed(e));
        applicationContext.addEventListener(ApplicationEvents.FontSizeChanged, value -> handleFontSizeChanged(value));

        // Subtitles
        subtitlesForm = new SubtitlesForm();
        Node subtitlesNode = subtitlesForm.createUI(applicationContext);
        horizontalSplitPane.getItems().add(subtitlesNode);

        // Subtitle Events
        applicationContext.addEventListener(ApplicationEvents.AddSubtitleAction, value -> addSubtitle());
        applicationContext.addEventListener(ApplicationEvents.MoveSubtitlesToEditor, value -> moveSubtitlesToTextEditor(value));
        applicationContext.addEventListener(ApplicationEvents.ViewSubtitleAction, subtitle -> setAudioPlayerSelection(subtitle));

        // Main Scene
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Subtitles Processor");
        primaryStage.setMaximized(true);
        primaryStage.show();

        // Restore Parameters Visual Components (Panes, Windows, etc)
        restoreComponents();
        subtitlesForm.restoreComponents();
    }

    private void restoreComponents() throws IOException {
        // SplitPane Dividers
        restoreSplitPane(verticalSplitPane, ApplicationParameters.VerticalSplitPaneDivider);
        restoreSplitPane(horizontalSplitPane, ApplicationParameters.HorizontalSplitPaneDivider);

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

        // TextEditor Text
        String textEditorText = applicationContext.getParameterValue(ApplicationParameters.TextEditorText);
        if (textEditorText != null) {
            textEditor.setText(textEditorText);
        }
    }

    private void restoreSplitPane(SplitPane splitPane, ApplicationParameters applicationParameter) {
        // Restore SplitPane Dividers
        String positionsString = applicationContext.getParameterValue(applicationParameter);
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
                        .map(d -> String.valueOf(d)).collect(Collectors.joining(";"));
                applicationContext.setParameterValue(applicationParameter, dividerPositionsString);
            });
        });
    }

    private void handleTextEditorKeyPressed(KeyEvent e) {
        if (!e.isControlDown()) {
            return;
        }

        switch (e.getCode()) {
            // CTRL+S -> Save
            case S:
                saveTextEditorText();
                e.consume();
                break;

            // CTRL+Up -> Switch to AudioPlayer
            case UP:
                audioPlayer.requestFocus();
                break;

            // CTRL+Right -> Add Subtitle
            case RIGHT:
                addSubtitle();
                break;
            default:
                break;
        }
    }

    private void addSubtitle() {
        AudioPlayerSelection selection = audioPlayer.getSelectionInMilliseconds();
        String selectedText = textEditor.getSelectedText();

        // Validating AudioPlayer and TextEditor Selections
        if (selection.isStartEmpty() || selection.isEndEmpty() || selectedText == null || selectedText.isEmpty()) {
            String message = "To Add new Subtitle You have to:\n"
                    + "1. Select Audio Interval on Wave Diagram via Mouse Left Click and Drag\n"
                    + "2. Type and Select Subtitle Text in Text Editor\n"
                    + "3. Press Add Button or Hotkey (CRTL+Right)";
            applicationContext.showError(message, null);
        }

        // Creating new Subtitle
        SubtitleTimeDTO start = new SubtitleTimeDTO(selection.getStart());
        SubtitleTimeDTO end = new SubtitleTimeDTO(selection.getEnd());
        String text = StringUtils.trimEndSymbols(selectedText, "\n");
        SubtitleDTO subtitle = new SubtitleDTO(start, end, text);

        // Adding Subtitle
        subtitlesForm.addSubtitle(subtitle);

        // Resetting Selections
        audioPlayer.resetSelectionInterval();
        textEditor.replaceSelection("");

        // Saving TextEditor Text
        saveTextEditorText();

        audioPlayer.requestFocus();
    }

    private void moveSubtitlesToTextEditor(Object value) {
        @SuppressWarnings("unchecked")
        List<SubtitleDTO> subtitles = (List<SubtitleDTO>) value;

        // Preparing Subtitles Text
        StringBuilder stringBuilder = new StringBuilder();
        subtitles.forEach(subtitle -> {
            stringBuilder.append(subtitle.getText());
            stringBuilder.append("\n\n");
        });
        int subtitlesTextLength = stringBuilder.length();

        // Taking Current Text
        String oldText = textEditor.getText();
        if (!oldText.isEmpty()) {
            stringBuilder.append("\n\n");
        }
        stringBuilder.append(oldText);

        // Setting New Text to Editor
        String newText = stringBuilder.toString();
        textEditor.setText(newText);
        textEditor.selectRange(0, subtitlesTextLength);
        saveTextEditorText();

        // Setting Selection to AudioPlayer
        if (subtitles.size() == 1) {
            SubtitleDTO subtitle = subtitles.get(0);
            setAudioPlayerSelection(subtitle);
        }

        saveTextEditorText();
    }

    private void handleFontSizeChanged(Object value) {
        Integer size = (Integer) value;
        Font font = new Font("Liberation Mono", size);
        textEditor.setFont(font);
    }

    private void saveTextEditorText() {
        String text = textEditor.getText();
        applicationContext.setParameterValue(ApplicationParameters.TextEditorText, text);
    }

    private void setAudioPlayerSelection(Object value) {
        SubtitleDTO subtitle = (SubtitleDTO) value;
        long start = subtitle.getStart().getAsMilliseconds();
        long end = subtitle.getEnd().getAsMilliseconds();
        AudioPlayerSelection selection = new AudioPlayerSelection(start, end);
        audioPlayer.setSelectionInMilliseconds(selection);
    }
}
