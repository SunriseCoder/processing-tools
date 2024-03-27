package process.forms.editor;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import app.utils.FileUtils;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import process.context.ApplicationContext;
import process.context.ApplicationEvents;
import process.entities.BookElementEntity;
import process.parser.BookElementSplitter;
import process.parser.RawBookParser;
import process.services.BookElementService;

@Component
public class ParseForm {
    private ApplicationContext applicationContext;

    @Autowired
    private BookElementService bookElementService;

    @FXML
    private SplitPane parseSplitPane;

    @FXML
    private Button transformButton;
    @FXML
    private Button saveAsCurrentButton;
    @FXML
    private Button saveAsChildButton;
    @FXML
    private Button resetButton;
    @FXML
    private Button deleteButton;

    @FXML
    private CheckBox wrapTextCheckBox;

    @FXML
    private Button splitChildrenButton;

    @FXML
    private TextArea sourceField;
    @FXML
    private TextArea transformationField;
    @FXML
    private TextArea transformedField;
    @FXML
    private TextField titleField;
    @FXML
    private TextArea contentField;

    private BookElementEntity currentBookElement;

    private RawBookParser rawBookParser;
    private BookElementSplitter bookElementSplitter;

    public ParseForm() {
        rawBookParser = new RawBookParser();
        bookElementSplitter = new BookElementSplitter();
    }

    public Node createUI(ApplicationContext applicationContext) throws IOException {
        this.applicationContext = applicationContext;
        applicationContext.addEventListener(ApplicationEvents.CurrentBookElementChanged,
                value -> currentBookElementChanged(value));

        Parent root = FileUtils.loadFXML(this);

        return root;
    }

    private void currentBookElementChanged(Object value) {
        currentBookElement = value == null ? null : (BookElementEntity) value;

        titleField.setText(currentBookElement == null ? "" : currentBookElement.getTitle());
        sourceField.setText(currentBookElement == null ? "" : currentBookElement.getSource());
        contentField.setText(currentBookElement == null ? "" : currentBookElement.getContent());
    }

    @FXML
    private void handleTransform() {
        try {
            String sourceText = sourceField.getText();
            String transformationText = transformationField.getText();

            String parsedText = rawBookParser.parse(sourceText, transformationText);

            transformedField.setText(parsedText);
        } catch (Exception e) {
            applicationContext.showError("Error due to transform text", e);
        }
    }

    @FXML
    private void handleSaveAsCurrent() {
        if (currentBookElement == null) {
            handleSaveAsChild();
        } else {
            saveBookElement(currentBookElement);
        }
    }

    @FXML
    private void handleSaveAsSibling() {
        if (currentBookElement == null) {
            handleSaveAsChild();
        } else {
            // Nullifying Id and Children for the existing entity that JPA could save it as New Entity
            currentBookElement.setId(null);
            currentBookElement.setChildren(null);
            saveBookElement(currentBookElement);
        }
    }

    @FXML
    private void handleSaveAsChild() {
        BookElementEntity child = new BookElementEntity();
        child.setParent(currentBookElement);
        saveBookElement(child);
    }

    private void saveBookElement(BookElementEntity bookElementEntity) {
        bookElementEntity.setTitle(titleField.getText());
        bookElementEntity.setSource(sourceField.getText());
        bookElementEntity.setContent(contentField.getText());

        BookElementEntity savedEntity = bookElementService.save(bookElementEntity);

        applicationContext.fireEvent(ApplicationEvents.BookElementTreeChanged, savedEntity);
    }

    @FXML
    private void handleDeleteCurrent() {
        if (currentBookElement == null) {
            return;
        }

        applicationContext.fireEvent(ApplicationEvents.DeleteSelectedBookElement, null);
    }

    @FXML
    private void handleWrapTextToggle() {
        boolean wrapText = wrapTextCheckBox.isSelected();

        sourceField.setWrapText(wrapText);
        transformationField.setWrapText(wrapText);
        transformedField.setWrapText(wrapText);
        contentField.setWrapText(wrapText);
    }

    @FXML
    private void handleSplitChildren() throws ParseException {
        String content = contentField.getText();
        List<BookElementEntity> entities = bookElementSplitter.split(content, currentBookElement);

        String message = "Are You sure to add " + entities.size() + " entity(s)?";
        boolean confirmed = applicationContext.showConfirmation("Split Children", message);

        if (confirmed) {
            bookElementService.save(entities);
            applicationContext.fireEvent(ApplicationEvents.BookElementTreeChanged, currentBookElement);
        }
    }
}
