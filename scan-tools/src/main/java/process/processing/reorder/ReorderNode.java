package process.processing.reorder;

import java.io.File;
import java.io.IOException;

import filters.FilenameFilterImages;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ProgressBar;
import process.context.ApplicationContext;
import process.processing.AbstractNode;
import processing.images.reordering.AbstractReorderer;
import processing.images.reordering.Reordering4Pages1PerScanFromMiddle;
import processing.images.reordering.Reordering4Pages2PerScanFromBeginning;
import utils.FileUtils;
import utils.NumberUtils;

public class ReorderNode extends AbstractNode {
    private ApplicationContext applicationContext;

    @FXML
    private ComboBox<ReorderingMethods> reorderingComboBox;

    @FXML
    private ProgressBar progressBar;

    protected double progress;

    public Node init(ApplicationContext applicationContext) throws IOException {
        this.applicationContext = applicationContext;
        Parent rootNode = FileUtils.loadFXML(this);
        return rootNode;
    }

    public void initialize() throws Exception {
        initComboBox(reorderingComboBox, ReorderingMethodsListCell.class, ReorderingMethods.values());
    }

    @FXML
    private void startProcessing() throws Exception {
        Thread thread = new Thread(new PrepareTask());
        thread.start();
    }

    private class PrepareTask implements Runnable {

        @Override
        public void run() {
            try {
                // TODO Lock Start Button before start and unlock after job finished
                // TODO Implement Cancel Button (maybe same button, but change caption)
                runWithExceptions();
                Platform.runLater(() -> applicationContext.showMessage("Prepare Images is done"));
            } catch (Exception e) {
                Platform.runLater(() -> applicationContext.showError("Error due to Prepare Images", e));
            }
        }

        private void runWithExceptions() throws Exception {
            ReorderingMethods reorderingMethod = reorderingComboBox.getSelectionModel().getSelectedItem();

            if (reorderingMethod == null) {
                return;
            }

            AbstractReorderer reorderer = reorderingMethod.getCl().newInstance();

            File inputFolder = applicationContext.getWorkFolder();
            File outputFolder = new File(inputFolder, "reordered");
            outputFolder.mkdir();

            File[] files = inputFolder.listFiles(new FilenameFilterImages(false));
            int amountOfImages = files.length;

            for (int i = 0; i < amountOfImages; i++) {
                int sourceIndex = i;

                sourceIndex = reorderer.getReorderedPageNumber(sourceIndex, amountOfImages);

                File sourceFile = files[sourceIndex];

                String outputFileName = "Page-" + NumberUtils.generateNumberByMaxNumber(i + 1, amountOfImages) +
                        "." + FileUtils.getFileExtension(sourceFile.getName());
                File outputFile = new File(outputFolder, outputFileName);
                FileUtils.copyFile(sourceFile, outputFile);

                progress = (double) (i + 1) / amountOfImages;
                Platform.runLater(new UpdateProgressTask());
            }
        }
    }

    private enum ReorderingMethods {
        Method4Pages1PerScanFromMiddle("4 pages, 1 per scan, from middle", Reordering4Pages1PerScanFromMiddle.class),
        Method4Pages2PerScanFromBeginning("4 pages, 2 per scan, from beginning", Reordering4Pages2PerScanFromBeginning.class);


        private String text;
        private Class<? extends AbstractReorderer> cl;

        private ReorderingMethods(String text, Class<? extends AbstractReorderer> cl) {
            this.text = text;
            this.cl = cl;
        }

        public String getText() {
            return text;
        }

        public Class<? extends AbstractReorderer> getCl() {
            return cl;
        }
    }

    public static class ReorderingMethodsListCell extends ListCell<ReorderingMethods> {
        @Override
        protected void updateItem(ReorderingMethods item, boolean empty) {
            super.updateItem(item, empty);
            setText(item == null ? null : item.getText());
        }
    }

    private class UpdateProgressTask implements Runnable {
        @Override
        public void run() {
            if (progressBar != null) {
                progressBar.setProgress(progress);
            }
        }
    }
}
