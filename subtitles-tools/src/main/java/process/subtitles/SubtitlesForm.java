package process.subtitles;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import app.utils.FileUtils;
import app.utils.PrimitiveUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import process.context.ApplicationContext;
import process.context.ApplicationEvents;
import process.context.ApplicationParameters;
import process.dto.SubtitleDTO;
import process.dto.SubtitleTimeDTO;
import wrappers.IntWrapper;
import wrappers.LongWrapper;

public class SubtitlesForm {
    private ApplicationContext applicationContext;

    @FXML
    private Spinner<Integer> fontSizeSpinner;
    @FXML
    private TextField subtitlesSearchTextField;
    @FXML
    private ListView<SubtitleDTO> subtitlesListView;
    @FXML
    private TextField subtitlesWorkFileTextField;

    private File workSubtitlesFile;

    public Node createUI(ApplicationContext applicationContext) throws IOException {
        this.applicationContext = applicationContext;

        Parent root = FileUtils.loadFXML(this);

        // Events from AudioPlayer
        applicationContext.addEventListener(ApplicationEvents.AudioPlayerOnPlay,
                value -> handleAudioPlayerPlayEvent(value));

        // Subtitles Text Size Spinner
        SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(10, 96, 12);
        fontSizeSpinner.setValueFactory(valueFactory);
        fontSizeSpinner.valueProperty().addListener(e -> handleFontSizeChanged());

        // Subtitles Search
        subtitlesSearchTextField.textProperty().addListener(e -> handleSubtitlesSerchTextChanged());
        subtitlesSearchTextField.setOnKeyPressed(e -> handleSubtitlesSearchKeyPressed(e));

        // Subtitles ListView
        subtitlesListView.setOnMouseClicked(e -> handleSubtitlesMouseClicked(e));
        subtitlesListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        subtitlesListView.setCellFactory(c -> new SubtitleListCell());
        subtitlesListView.setOnKeyPressed(e -> handleSubtitlesListViewKeyPressed(e));

        return root;
    }

    public void restoreComponents() throws IOException {
        // Font Size
        String fontSizeString = applicationContext.getParameterValue(ApplicationParameters.FontSize);
        if (fontSizeString != null) {
            fontSizeSpinner.getValueFactory().setValue(Integer.parseInt(fontSizeString));
        }

        // Subtitles File
        String workSubtitlesFilePath = applicationContext.getParameterValue(ApplicationParameters.SubtitlesWorkFile);
        if (workSubtitlesFilePath != null) {
            File workSubtitlesFile = new File(workSubtitlesFilePath);
            if (workSubtitlesFile.exists() && !workSubtitlesFile.isDirectory()) {
                setWorkSubtitlesFile(workSubtitlesFile);
                loadSubtitlesFromFile();
            }
        }
    }

    private void handleAudioPlayerPlayEvent(Object value) {
        long position = (long) value;

        subtitlesListView.getSelectionModel().clearSelection();

        // Looking for the Subtitle at particular Position
        ObservableList<SubtitleDTO> items = subtitlesListView.getItems();
        List<SubtitleDTO> itemList = new ArrayList<>(items);
        SubtitleDTO subtitle = findSubtitleByPosition(itemList, position);

        // If found, Selecting and Scrolling to it
        if (subtitle != null) {
            subtitlesListView.getSelectionModel().select(subtitle);
            int selectedIndex = subtitlesListView.getSelectionModel().getSelectedIndex();
            selectedIndex = Math.max(0, selectedIndex - 3);
            subtitlesListView.scrollTo(selectedIndex);
        }
    }

    private SubtitleDTO findSubtitleByPosition(List<SubtitleDTO> items, long position) {
        // Recursive Binary Search
        // Items must be Sorted, otherwise algorithm will not work
        if (items.size() == 0) {
            return null;
        } else if (items.size() == 1) {
            SubtitleDTO item = items.get(0);
            boolean match = item.getStart().getAsMilliseconds() <= position
                    && item.getEnd().getAsMilliseconds() >= position;
            return match ? item : null;
        } else {
            int size = items.size();
            int halfSize = size / 2;

            SubtitleDTO item = items.get(halfSize);

            List<SubtitleDTO> nextList = item.getStart().getAsMilliseconds() > position ? items.subList(0, halfSize)
                    : items.subList(halfSize, size);

            SubtitleDTO result = findSubtitleByPosition(nextList, position);
            return result;
        }
    }

    private void handleFontSizeChanged() {
        Integer size = fontSizeSpinner.getValue();
        applicationContext.setParameterValue(ApplicationParameters.FontSize, size.toString());
        applicationContext.fireEvent(ApplicationEvents.FontSizeChanged, size);

        subtitlesListView.refresh();
    }

    private void handleSubtitlesSerchTextChanged() {
        // Clearing Selection
        subtitlesListView.getSelectionModel().clearSelection();

        // Checking that Search Text is not empty
        String searchText = subtitlesSearchTextField.getText().toLowerCase();
        if (searchText.isEmpty()) {
            return;
        }

        // Searching Matching Items
        List<Integer> selectedIndices = new ArrayList<>();
        ObservableList<SubtitleDTO> items = subtitlesListView.getItems();
        for (int i = 0; i < items.size(); i++) {
            SubtitleDTO subtitle = items.get(i);
            if (subtitle.getText().toLowerCase().contains(searchText)) {
                selectedIndices.add(i);
            }
        }

        // Selecting Items and Scrolling ListView to the first Selected Element
        if (selectedIndices.size() > 0) {
            int[] selectedIndicesArray = PrimitiveUtils.listToIntArray(selectedIndices);
            subtitlesListView.getSelectionModel().selectIndices(-1, selectedIndicesArray);
            int scrollTo = Math.max(0, selectedIndicesArray[0] - 1);
            subtitlesListView.scrollTo(scrollTo);
        }
    }

    private void handleSubtitlesSearchKeyPressed(KeyEvent e) {
        if (e.getCode().equals(KeyCode.ENTER)) {
            handleSubtitlesSerchTextChanged();
        }
    }

    private void handleSubtitlesMouseClicked(MouseEvent e) {
        // Double Click via Left Mouse Button
        if (e.getButton().equals(MouseButton.PRIMARY) && e.getClickCount() == 2) {
            SubtitleDTO subtitle = subtitlesListView.getSelectionModel().getSelectedItem();
            fireViewSubtitleAction(subtitle);
        }
    }

    private void handleSubtitlesListViewKeyPressed(KeyEvent e) {
        switch (e.getCode()) {
            case ENTER:
                SubtitleDTO subtitle = subtitlesListView.getSelectionModel().getSelectedItem();
                fireViewSubtitleAction(subtitle);
                break;
            case DELETE:
                handleDeleteSubtitlesPressed();
                break;
            default:
                break;
        }
    }

    private void fireViewSubtitleAction(SubtitleDTO subtitle) {
        applicationContext.fireEvent(ApplicationEvents.ViewSubtitleAction, subtitle);
    }

    private class SubtitleListCell extends ListCell<SubtitleDTO> {
        @Override
        protected void updateItem(SubtitleDTO item, boolean empty) {
            super.updateItem(item, empty);

            VBox node = null;
            if (!empty) {
                node = new VBox();

                // Font
                Integer fontSize = fontSizeSpinner.getValue();
                Font font = new Font(fontSize);

                // Subtitle Time Label
                Label timeLabel = new Label(item.getTimeAsString());
                timeLabel.setFont(font);
                timeLabel.setTextFill(Color.BLUE);
                node.getChildren().add(timeLabel);

                // Subtitle Text Label
                Label textLabel = new Label(item.getText());
                textLabel.setFont(font);
                textLabel.setTextFill(Color.BLACK);
                node.getChildren().add(textLabel);
            }
            setGraphic(node);
        }
    }

    @FXML
    private void handleAddSubtitlePressed() {
        applicationContext.fireEvent(ApplicationEvents.AddSubtitleAction, null);
    }

    @FXML
    private void handleEditSubtitlePressed() throws IOException {
        ObservableList<SubtitleDTO> selectedItems = subtitlesListView.getSelectionModel().getSelectedItems();

        // Checking that User selected only 1 Subtitle
        if (selectedItems.size() != 1) {
            applicationContext.showWarning("You have to select 1 Subtitle first", null);
            return;
        }

        // Creating SubtitleEditForm
        SubtitleDTO subtitleDTO = selectedItems.get(0);
        Stage stage = applicationContext.getStage();
        SubtitleEditForm form = new SubtitleEditForm(stage);
        form.setSubtitle(subtitleDTO);
        form.showAndWait();

        // Refreshing List and Saving to File
        subtitlesListView.refresh();
        saveSubtitlesToFile();
    }

    @FXML
    private void handleMoveSubtitlesToEditorPressed() {
        ObservableList<SubtitleDTO> selectedItems = subtitlesListView.getSelectionModel().getSelectedItems();

        // User Confirmation about going to Edit Subtitle(s)
        boolean confirmed;
        if (selectedItems.size() == 1) {
            confirmed = applicationContext.showConfirmation("Edit Subtitles", "Are You sure to Edit 1 Subtitle?");
        } else if (selectedItems.size() > 1) {
            confirmed = applicationContext.showConfirmation("Edit Subtitles",
                    "You Are going to Edit " + selectedItems.size() + " Subtitle(s).\n"
                            + "This will lead to lose all Timings of these Subtitles.\nAre You sure?");
        } else {
            return;
        }
        if (!confirmed) {
            return;
        }

        // Firing MoveSubtitleToEditor Event
        List<SubtitleDTO> selectedSubtitles = new ArrayList<>(selectedItems);
        applicationContext.fireEvent(ApplicationEvents.MoveSubtitlesToEditor, selectedSubtitles);

        // Removing Subtitles from ListView
        subtitlesListView.getItems().removeAll(selectedSubtitles);

        saveSubtitlesToFile();
    }

    @FXML
    private void handleDeleteSubtitlesPressed() {
        ObservableList<SubtitleDTO> selectedItems = subtitlesListView.getSelectionModel().getSelectedItems();
        boolean confirmed = applicationContext.showConfirmation("Delete Subtitles",
                "Are You sure that You want to delete " + selectedItems.size() + " Subtitle(s)?");
        if (confirmed) {
            subtitlesListView.getItems().removeAll(selectedItems);
            saveSubtitlesToFile();
        }
    }

    @FXML
    private void handleSortSubtitlesPressed() {
        sortSubtitles(subtitlesListView.getItems());
    }

    @FXML
    private void handleMergeSubtitlesPressed() {
        ObservableList<SubtitleDTO> selectedItems = subtitlesListView.getSelectionModel().getSelectedItems();

        // Checking that 2 or more Subtitles are Selected
        if (selectedItems.size() < 2) {
            applicationContext.showWarning("You have to Select at least 2 Subtitles to Merge", null);
            return;
        }

        // User Confirmation
        String confirmationMessage = "You are going to Merge " + selectedItems.size() + " Subtitle(s).\n"
                + "That means, that as the result will me 1 Subtitle.\n"
                + "Time Interval will be expanded to fit all Subtitles to be merged.\n"
                + "Text will be also Merged by starting new line for each Subtitle.\n"
                + "Attention! If You are merging not Neighbour Subtitles only,\n"
                + "Then as result the subtitles in between could be damaged by next Sort!\n" + "Are You sure to Merge?";
        boolean confirmed = applicationContext.showConfirmation("Merge Subtitles", confirmationMessage);
        if (!confirmed) {
            return;
        }

        // Sorting Subtitles
        List<SubtitleDTO> subtitles = new ArrayList<>(selectedItems);
        sortSubtitles(subtitles);

        // Merging Time Intervals
        SubtitleDTO subtitle = new SubtitleDTO();
        subtitle.setStart(subtitles.get(0).getStart());
        subtitle.setEnd(subtitles.get(subtitles.size() - 1).getEnd());

        // Merging Text
        String subtitleText = subtitles.stream().map(s -> s.getText()).collect(Collectors.joining("\n"));
        subtitle.setText(subtitleText);

        // Removing Source Subtitles from ListView and Adding New One
        subtitlesListView.getItems().removeAll(subtitles);
        subtitlesListView.getItems().add(subtitle);

        // Sorting Subtitles to place New Subtitle and Adjust others
        sortSubtitles(subtitlesListView.getItems());
    }

    public void addSubtitle(SubtitleDTO subtitle) {
        // Adding to the List and Sorting the List
        ObservableList<SubtitleDTO> items = subtitlesListView.getItems();
        items.add(subtitle);
        sortSubtitles(items);

        // Saving Subtitles File
        saveSubtitlesToFile();
    }

    private void sortSubtitles(List<SubtitleDTO> items) {
        items.sort((a, b) -> (int) (a.getStart().getAsMilliseconds() - b.getStart().getAsMilliseconds()));

        // Adjust that Subtitles does not Intersect with each others
        LongWrapper lastEnd = new LongWrapper(-1);
        items.forEach(subtitle -> {
            // Checking that Last End is Before Next Start
            if (subtitle.getStart().getAsMilliseconds() <= lastEnd.getValue()) {
                subtitle.setStart(new SubtitleTimeDTO(lastEnd.getValue() + 1));
            }

            // Checking that End is After Start
            if (subtitle.getEnd().getAsMilliseconds() <= subtitle.getStart().getAsMilliseconds()) {
                subtitle.setEnd(new SubtitleTimeDTO(subtitle.getStart().getAsMilliseconds() + 1));
            }

            lastEnd.setValue(subtitle.getEnd().getAsMilliseconds());
        });
    }

    @FXML
    private void handleSubtitlesOpenFile() throws IOException {
        chooseSubtitlesFileToOpen();
        loadSubtitlesFromFile();
    }

    @FXML
    private void handleSubtitlesRefreshFromFile() throws IOException {
        loadSubtitlesFromFile();
    }

    private void chooseSubtitlesFileToOpen() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Subtitles File");
        ExtensionFilter filter = new ExtensionFilter("Subtitles files (*.srt)", "*.srt");
        fileChooser.getExtensionFilters().add(filter);

        if (workSubtitlesFile != null) {
            fileChooser.setInitialDirectory(workSubtitlesFile.getParentFile());
        }

        File newFile = fileChooser.showOpenDialog(null);

        if (newFile != null && newFile.exists() && !newFile.isDirectory()) {
            setWorkSubtitlesFile(newFile);
        }
    }

    private void loadSubtitlesFromFile() throws IOException, FileNotFoundException {
        if (workSubtitlesFile == null) {
            applicationContext.showError("Select Subtitles File first", null);
            return;
        }

        subtitlesListView.getItems().clear();

        // Reading Subtitles from File
        List<SubtitleDTO> subtitles = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(workSubtitlesFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Skip Subtitle Number

                // Reading Subtitle Time Interval
                line = reader.readLine();
                SubtitleDTO subtitle = new SubtitleDTO();
                subtitle.parseTime(line);

                // Reading Subtitle Text
                String text = "";
                boolean firstLine = true;
                while (!(line = reader.readLine()).isEmpty()) {
                    if (firstLine) {
                        firstLine = false;
                    } else {
                        text += "\n";
                    }
                    text += line;
                }
                subtitle.setText(text);

                subtitles.add(subtitle);
            }
        }

        ObservableList<SubtitleDTO> items = FXCollections.observableArrayList(subtitles);
        subtitlesListView.setItems(items);
    }

    @FXML
    private void handleSubtitlesSaveToFile() throws IOException {
        saveSubtitlesToFile();
    }

    private void saveSubtitlesToFile() {
        if (workSubtitlesFile == null) {
            chooseSubtitlesFileToSave();
        }

        if (workSubtitlesFile == null) {
            return;
        }

        try {
            doSubtitlesSaveToFile();
        } catch (IOException e) {
            applicationContext.showError("Could not Save Subtitles to File: " + workSubtitlesFile.getAbsolutePath(), e);
        }
    }

    private void doSubtitlesSaveToFile() throws IOException, FileNotFoundException {
        FileUtils.createFile(workSubtitlesFile, true);
        try (PrintWriter writer = new PrintWriter(workSubtitlesFile)) {
            IntWrapper counter = new IntWrapper(1);
            ObservableList<SubtitleDTO> subtitles = subtitlesListView.getItems();
            subtitles.forEach(subtitle -> {
                writer.println(counter.postIncrement());
                writer.println(subtitle.getTimeAsString());
                writer.println(subtitle.getText());
                writer.println();
            });
            writer.flush();
        }
    }

    private void chooseSubtitlesFileToSave() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Subtitles File");
        ExtensionFilter filter = new ExtensionFilter("Subtitles files (*.srt)", "*.srt");
        fileChooser.getExtensionFilters().add(filter);

        if (workSubtitlesFile != null) {
            fileChooser.setInitialDirectory(workSubtitlesFile.getParentFile());
        }

        File newFile = fileChooser.showSaveDialog(null);

        if (newFile != null && !newFile.isDirectory()) {
            setWorkSubtitlesFile(newFile);
        }
    }

    private void setWorkSubtitlesFile(File newFile) {
        workSubtitlesFile = newFile;

        String filePath = newFile.getAbsolutePath();
        applicationContext.setParameterValue(ApplicationParameters.SubtitlesWorkFile, filePath);

        subtitlesWorkFileTextField.setText(filePath);
    }
}
