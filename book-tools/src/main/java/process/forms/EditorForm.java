package process.forms;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import app.utils.FileUtils;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Tab;
import process.context.ApplicationContext;
import process.forms.editor.EditForm;
import process.forms.editor.ParseForm;
import process.forms.editor.ViewForm;

@Component
public class EditorForm {
    @FXML
    private Tab viewTab;
    @FXML
    private Tab editTab;
    @FXML
    private Tab parseTab;

    @Autowired
    private ParseForm parseForm;

    public Node createUI(ApplicationContext applicationContext) throws IOException {
        Parent root = FileUtils.loadFXML(this);

        ViewForm viewForm = new ViewForm();
        Node viewFormNode = viewForm.createUI(applicationContext);
        viewTab.setContent(viewFormNode);

        EditForm editForm = new EditForm();
        Node editFormNode = editForm.createUI(applicationContext);
        editTab.setContent(editFormNode);

        //parseForm = new ParseForm();
        Node parseFormNode = parseForm.createUI(applicationContext);
        parseTab.setContent(parseFormNode);

        return root;
    }
}
