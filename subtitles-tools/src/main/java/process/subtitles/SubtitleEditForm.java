package process.subtitles;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.utils.FileUtils;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import process.dto.SubtitleDTO;

public class SubtitleEditForm extends Stage {
    private Pattern subtitleTimePattern;

    @FXML
    private TextField timeTextField;
    @FXML
    private TextArea textTextField;

    private SubtitleDTO subtitle;

    public SubtitleEditForm(Window owner) throws IOException {
        super();

        // 00:00:00,000 --> 00:00:00,000
        subtitleTimePattern = Pattern
                .compile("^[0-9]{2}:[0-9]{2}:[0-9]{2},[0-9]{3} --> [0-9]{2}:[0-9]{2}:[0-9]{2},[0-9]{3}$");

        Parent root = FileUtils.loadFXML(this);

        // Validating TextField Value
        timeTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            Matcher matcher = subtitleTimePattern.matcher(newValue);
            String value = matcher.matches() ? newValue : oldValue;
            ((StringProperty) observable).setValue(value);
        });

        // Moving Selection Forward to Replace next symbol instead of Insert
        timeTextField.setOnKeyPressed(e -> {
            timeTextField.selectForward();
        });

        Scene scene = new Scene(root);
        setScene(scene);

        // Initializing Modality of the Form
        initOwner(owner);
        initModality(Modality.WINDOW_MODAL);
    }

    public void setSubtitle(SubtitleDTO subtitle) {
        this.subtitle = subtitle;
        timeTextField.setText(subtitle.getTimeAsString());
        textTextField.setText(subtitle.getText());
    }

    @FXML
    private void saveButtonPressed() {
        subtitle.parseTime(timeTextField.getText());
        subtitle.setText(textTextField.getText());

        close();
    }

    @FXML
    private void cancelButtonPressed() {
        close();
    }
}
