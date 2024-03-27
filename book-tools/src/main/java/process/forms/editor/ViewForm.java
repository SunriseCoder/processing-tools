package process.forms.editor;

import java.io.IOException;

import app.utils.FileUtils;
import javafx.scene.Node;
import javafx.scene.Parent;
import process.context.ApplicationContext;

public class ViewForm {
    public Node createUI(ApplicationContext applicationContext) throws IOException {
        Parent root = FileUtils.loadFXML(this);

        return root;
    }
}
