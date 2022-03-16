package process.forms;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import process.context.ApplicationContext;
import process.context.ApplicationEvents;
import process.context.ApplicationParameters;
import utils.FileUtils;

public class AnalyzerForm {
    private ApplicationContext applicationContext;

    @FXML
    private Spinner<Integer> fontSizeSpinner;

    public Node createUI(ApplicationContext applicationContext) throws IOException {
        this.applicationContext = applicationContext;

        Parent root = FileUtils.loadFXML(this);

        // Events from AudioPlayer
        applicationContext.addEventListener(ApplicationEvents.AudioPlayerOnPlay,
                value -> handleAudioPlayerPlayEvent(value));

        // Subtitles Text Size Spinner
        SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(10, 96, 12);
        fontSizeSpinner.setValueFactory(valueFactory);

        return root;
    }

    public void restoreComponents() throws IOException {
        // Font Size
        String fontSizeString = applicationContext.getParameterValue(ApplicationParameters.FontSize);
        if (fontSizeString != null) {
            fontSizeSpinner.getValueFactory().setValue(Integer.parseInt(fontSizeString));
        }
    }

    private void handleAudioPlayerPlayEvent(Object value) {
        long position = (long) value;
    }
}
