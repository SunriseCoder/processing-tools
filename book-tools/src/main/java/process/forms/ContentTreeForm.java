package process.forms;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import app.utils.FileUtils;
import javafx.beans.Observable;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import process.context.ApplicationContext;
import process.context.ApplicationEvents;
import process.entities.BookElementEntity;
import process.services.BookElementService;

@Component
public class ContentTreeForm {
    private ApplicationContext applicationContext;

    @Autowired
    private BookElementService bookElementService;

    @FXML
    private TreeView<BookElementEntity> treeView;

    private BookElementEntity rootEntity;
    private Map<Long, TreeItem<BookElementEntity>> treeItems;

    public ContentTreeForm() {
        rootEntity = new BookElementEntity();
        rootEntity.setTitle("Root");

        treeItems = new HashMap<>();
    }

    public Node createUI(ApplicationContext applicationContext) throws IOException {
        this.applicationContext = applicationContext;

        applicationContext.addEventListener(ApplicationEvents.BookElementTreeChanged,
                value -> refreshBookElementTree(value));
        applicationContext.addEventListener(ApplicationEvents.DeleteSelectedBookElement,
                value -> handleDeleteSelectedBookElement());

        Parent root = FileUtils.loadFXML(this);

        treeView.setCellFactory(c -> new ContentTreeCell());
        treeView.getSelectionModel().selectedItemProperty().addListener(
                e -> treeViewSelectionChanged(e));
        treeView.setOnKeyPressed(e -> handleTreeItemKeyPressed(e));

        treeView.setShowRoot(false);
        treeView.setRoot(new TreeItem<>(rootEntity));

        refreshBookElementTree(null);

        return root;
    }

    private void treeViewSelectionChanged(Observable e) {
        TreeItem<BookElementEntity> selectedItem = treeView.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            BookElementEntity selectedBookElement = selectedItem.getValue();
            applicationContext.fireEvent(
                    ApplicationEvents.CurrentBookElementChanged, selectedBookElement);
        }
    }

    private void handleTreeItemKeyPressed(KeyEvent e) {
        switch (e.getCode()) {
        case DELETE:
            handleDeleteSelectedBookElement();
            break;
        default:
            break;
        }
    }

    @FXML
    private void handleDeleteSelectedBookElement() {
        TreeItem<BookElementEntity> selectedItem = treeView.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            return;
        }

        BookElementEntity bookElementEntity = selectedItem.getValue();
        boolean confirmed = applicationContext.showConfirmation("Delete Book Element",
                "Are You sure that You want to delete \"" + bookElementEntity.getTitle() + "\" ?");
        if (confirmed) {
            bookElementService.delete(bookElementEntity);
            refreshBookElementTree(null);
        }
    }

    @FXML
    private void handleRefreshBookElementTree() {
        refreshBookElementTree(null);
    }

    private void refreshBookElementTree(Object objectToSelect) {
        TreeItem<BookElementEntity> rootItem = treeView.getRoot();
        List<BookElementEntity> entities = bookElementService.getRootElements();
        processElementsRecursively(entities, rootItem);
        treeView.refresh();

        // Workaround due to JavaFX bug, when deleted last visible item, selection do not reset
        if (rootItem.getChildren().isEmpty() && treeView.getSelectionModel().getSelectedItem() != null) {
            treeView.getSelectionModel().clearSelection();
            applicationContext.fireEvent(ApplicationEvents.CurrentBookElementChanged, null);
        }

        if (objectToSelect != null && objectToSelect instanceof BookElementEntity) {
            BookElementEntity bookElementEntity = (BookElementEntity) objectToSelect;
            TreeItem<BookElementEntity> itemToSelect = treeItems.get(bookElementEntity.getId());
            treeView.getSelectionModel().select(itemToSelect);
        }
    }

    private void processElementsRecursively(List<BookElementEntity> entities, TreeItem<BookElementEntity> item) {
        // Creating Map to know, which Items should be removed from the Tree
        Map<Long, TreeItem<BookElementEntity>> oldItems = item.getChildren().parallelStream()
                .collect(Collectors.toMap(i -> i.getValue().getId(), i -> i));

        // Sorting Entities by Position
        entities.sort((a, b) -> compareLong(a.getPosition(), b.getPosition()));

        // Clearing
        item.getChildren().clear();

        entities.stream().forEach(entity -> {
            // Removing Item from the List to know, which Items were updated and don't need to be removed
            TreeItem<BookElementEntity> childItem = oldItems.remove(entity.getId());

            // If we have New Entity, creating New Tree Item
            if (childItem == null) {
                childItem = new TreeItem<>(entity);
                childItem.setValue(entity);

                // Put the Item to the Item Map
                treeItems.put(entity.getId(), childItem);
            }

            // Adding new Item to the TreeView
            item.getChildren().add(childItem);

            // Processing Sub-Elements Recursively
            processElementsRecursively(entity.getChildren(), childItem);
        });
    }

    private int compareLong(Long a, Long b) {
        if (a == b) {
            return 0;
        }

        if (a == null) {
            return 1;
        }

        if (b == null) {
            return -1;
        }

        return a.compareTo(b);
    }

    private class ContentTreeCell extends TreeCell<BookElementEntity> {
        @Override
        protected void updateItem(BookElementEntity item, boolean empty) {
            super.updateItem(item, empty);

            VBox node = null;
            if (!empty) {
                node = new VBox();

                // Book Element Title Label
                Label timeLabel = new Label(item.getTitle());
                timeLabel.setTextFill(Color.BLUE);
                node.getChildren().add(timeLabel);
            }
            setGraphic(node);
        }
    }
}
