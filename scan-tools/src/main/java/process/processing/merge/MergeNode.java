package process.processing.merge;

import java.io.IOException;

import app.utils.FileUtils;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.GridPane;
import process.context.ApplicationContext;
import process.processing.AbstractNode;

public class MergeNode extends AbstractNode {
    private ApplicationContext applicationContext;

    @FXML
    private GridPane gridPane;

    @FXML
    private ComboBox<ImageMergeMethods> imageMergeComboBox;

    @FXML
    private ProgressBar progressBar;

    public Node init(ApplicationContext applicationContext) throws IOException {
        this.applicationContext = applicationContext;
        Parent rootNode = FileUtils.loadFXML(this);
        return rootNode;
    }

    public void initialize() throws Exception {
        initComboBox(imageMergeComboBox, ImageMergeListCell.class, ImageMergeMethods.values());
    }

    @FXML
    private void startProcessing() {
        MergeManagerTask managerTask = new MergeManagerTask("Merge Images");
        managerTask.setApplicationContext(applicationContext);
        managerTask.setProgressBar(progressBar);

        ImageMergeMethods mergeMethod = imageMergeComboBox.getSelectionModel().getSelectedItem();
        int remainder;
        switch (mergeMethod) {
        case Method1ImageOnFirstPage:
            remainder = 0;
            break;
        case Method2ImagesOnFirstPage:
            remainder = 1;
            break;
        default:
            throw new IllegalArgumentException("Merge Method is not supported: " + mergeMethod);
        }
        managerTask.setRemainder(remainder);

        Thread thread = new Thread(managerTask, managerTask.getName() + " Manager");
        thread.start();
    }

    private enum ImageMergeMethods {
        Method1ImageOnFirstPage("On the First page should be 1 Image"),
        Method2ImagesOnFirstPage("On the First page should be 2 Images");

        private String text;

        private ImageMergeMethods(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }
    }

    public static class ImageMergeListCell extends ListCell<ImageMergeMethods> {
        @Override
        protected void updateItem(ImageMergeMethods item, boolean empty) {
            super.updateItem(item, empty);
            setText(item == null ? null : item.getText());
        }
    }
}
