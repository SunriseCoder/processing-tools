package app;

import java.io.IOException;

import app.utils.FileUtils;
import dto.Vertex;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

public class VertexEditForm extends Stage {
    @FXML
    private TextArea textTextField;
    @FXML
    private TextArea detailsTextField;

    private Vertex vertex;

    public VertexEditForm(Window owner) throws IOException {
        super();

        Parent root = FileUtils.loadFXML(this);

        // Moving Selection Forward to Replace next symbol instead of Insert
        textTextField.setOnKeyPressed(e -> {
            textTextField.selectForward();
        });

        Scene scene = new Scene(root);
        setScene(scene);

        // Initializing Modality of the Form
        initOwner(owner);
        initModality(Modality.WINDOW_MODAL);
    }

    public void setVertex(Vertex vertex) {
        this.vertex = vertex;
        textTextField.setText(vertex.getText());
        detailsTextField.setText(vertex.getDetails());
    }

    @FXML
    private void saveButtonPressed() {
        vertex.setText(textTextField.getText());
        vertex.setDetails(detailsTextField.getText());

        close();
    }

    @FXML
    private void cancelButtonPressed() {
        close();
    }
}
