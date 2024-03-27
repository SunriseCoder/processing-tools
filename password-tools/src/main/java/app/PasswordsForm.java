package app;

import java.io.File;
import java.io.IOException;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;

import app.context.ApplicationContext;
import app.context.ApplicationParameters;
import app.utils.FileUtils;
import app.utils.JSONUtils;
import dto.CredentialEntry;
import javafx.beans.Observable;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeItem.TreeModificationEvent;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.cell.TextFieldTreeTableCell;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.scene.input.KeyEvent;

public class PasswordsForm {
    private ApplicationContext applicationContext;

    // General
    @FXML
    private TreeTableView<CredentialEntry> treeTableView;
    @FXML
    private TreeTableColumn<CredentialEntry, String> columnPlace;
    @FXML
    private TreeTableColumn<CredentialEntry, String> columnLogin;
    @FXML
    private TreeTableColumn<CredentialEntry, String> columnPassword;
    @FXML
    private TreeTableColumn<CredentialEntry, String> columnComment;

    private CredentialEntry rootEntry;
    private PasswordGenerator passwordGenerator;

    public PasswordsForm() {
        passwordGenerator = new PasswordGenerator();
    }

    public Parent init(ApplicationContext applicationContext) throws IOException {
        this.applicationContext = applicationContext;

        Parent rootNode = FileUtils.loadFXML(this);

        loadPasswords(applicationContext);

        TreeItem<CredentialEntry> rootItem = convertToTreeStructure(rootEntry);
        treeTableView.setRoot(rootItem);
        treeTableView.setEditable(true);
        treeTableView.setOnKeyPressed(e -> handleKeyPressed(e));

        columnPlace.setCellFactory(TextFieldTreeTableCell.forTreeTableColumn());
        columnPlace.setCellValueFactory(new TreeItemPropertyValueFactory<CredentialEntry, String>("place"));
        columnPlace.setEditable(true);
        columnPlace.setOnEditCommit(e -> e.getRowValue().getValue().setPlace(e.getNewValue()));

        columnLogin.setCellFactory(TextFieldTreeTableCell.forTreeTableColumn());
        columnLogin.setCellValueFactory(new TreeItemPropertyValueFactory<CredentialEntry, String>("login"));
        columnLogin.setEditable(true);
        columnLogin.setOnEditCommit(e -> e.getRowValue().getValue().setLogin(e.getNewValue()));

        columnPassword.setCellFactory(TextFieldTreeTableCell.forTreeTableColumn());
        columnPassword.setCellValueFactory(new TreeItemPropertyValueFactory<CredentialEntry, String>("password"));
        columnPassword.setEditable(true);
        columnPassword.setOnEditCommit(e -> e.getRowValue().getValue().setPassword(e.getNewValue()));

        columnComment.setCellFactory(TextFieldTreeTableCell.forTreeTableColumn());
        columnComment.setCellValueFactory(new TreeItemPropertyValueFactory<CredentialEntry, String>("comment"));
        columnComment.setEditable(true);
        columnComment.setOnEditCommit(e -> e.getRowValue().getValue().setComment(e.getNewValue()));

        restoreComponents();

        return rootNode;
    }

    private void loadPasswords(ApplicationContext applicationContext) {
        try {
            String filename = applicationContext.getParameterValue(ApplicationParameters.PasswordsFileName);
            File file = new File(filename);
            TypeReference<CredentialEntry> typeReference = new TypeReference<CredentialEntry>() {};
            rootEntry = JSONUtils.loadFromDisk(file , typeReference);
            setParentsRecursively(rootEntry);
        } catch (IOException e) {
            rootEntry = new CredentialEntry();
            rootEntry.setPlace("Root");
        }
    }

    private void setParentsRecursively(CredentialEntry entry) {
        entry.getChildren().forEach(childEntry -> {
            childEntry.setParent(entry);
            setParentsRecursively(childEntry);
        });
    }

    private TreeItem<CredentialEntry> convertToTreeStructure(CredentialEntry credentialEntry) {
        TreeItem<CredentialEntry> treeItem = new TreeItem<CredentialEntry>(credentialEntry);
        treeItem.setExpanded(true);

        credentialEntry.getChildren().forEach(entry -> {
            TreeItem<CredentialEntry> childTreeItem = convertToTreeStructure(entry);
            treeItem.getChildren().add(childTreeItem);
        });

        return treeItem;
    }

    private void handleKeyPressed(KeyEvent e) {
        if (e.isControlDown()) {
            switch (e.getCode()) {
            case G:
                generatePassword();
                break;
            case N:
                newEntry();
                break;
            case S:
                saveData();
                break;
            default:
                break;
            }
        } else {
            switch (e.getCode()) {
            case DELETE:
                deleteEntry();
                break;
            default:
                break;
            }
        }
    }

    private void generatePassword() {
        TreeItem<CredentialEntry> selectedItem = treeTableView.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            return;
        }

        String password = passwordGenerator.generatePassword(16);

        CredentialEntry entry = selectedItem.getValue();
        entry.setPassword(password);

        TreeModificationEvent<CredentialEntry> event = new TreeModificationEvent<>(TreeItem.valueChangedEvent(), selectedItem);
        Event.fireEvent(selectedItem, event);
    }

    private void newEntry() {
        TreeItem<CredentialEntry> selectedItem = treeTableView.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            return;
        }

        CredentialEntry entry = selectedItem.getValue();
        CredentialEntry childEntry = new CredentialEntry(entry);
        entry.addChild(childEntry);

        TreeItem<CredentialEntry> childItem = new TreeItem<CredentialEntry>(childEntry);
        childItem.setExpanded(true);
        selectedItem.getChildren().add(childItem);

        treeTableView.getSelectionModel().select(childItem);
    }

    private void saveData() {
        try {
            String json = JSONUtils.toJSON(rootEntry);
            String filename = applicationContext.getParameterValue(ApplicationParameters.PasswordsFileName);
            FileUtils.saveToFile(json, filename);
        } catch (IOException e) {
            applicationContext.showError("Could not save data to file", e);
        }
    }

    private void deleteEntry() {
        TreeItem<CredentialEntry> selectedItem = treeTableView.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            return;
        }

        CredentialEntry entry = selectedItem.getValue();
        CredentialEntry parentEntry = entry.getParent();
        if (parentEntry == null) {
            return;
        }

        parentEntry.getChildren().remove(entry);
        selectedItem.getParent().getChildren().remove(selectedItem);
    }

    private void handleColumnWidthChanged(Observable e) {
        String tableColumnWidths = treeTableView.getColumns().stream()
                .map(column -> String.valueOf(column.getWidth()))
                .collect(Collectors.joining(";"));
        applicationContext.setParameterValue(ApplicationParameters.TableColumnWidths, tableColumnWidths);
    }

    private void restoreComponents() {
        String tableColumnWidths = applicationContext.getParameterValue(ApplicationParameters.TableColumnWidths);

        if (tableColumnWidths != null) {
            String[] widthStrings = tableColumnWidths.split(";");
            for (int i = 0; i < widthStrings.length; i++) {
                TreeTableColumn<CredentialEntry, ?> column = treeTableView.getColumns().get(i);
                double width = Double.parseDouble(widthStrings[i]);
                column.setPrefWidth(width);
            }
        }

        treeTableView.getColumns().forEach(column -> {
            column.widthProperty().addListener(e -> handleColumnWidthChanged(e));
        });
    }
}
