package process.processing.split;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

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
import processing.images.split.AbstractSplitter;
import processing.images.split.Split2PagesOn1ImageHorizontally;
import utils.FileUtils;

public class SplitNode extends AbstractNode {
    private ApplicationContext applicationContext;

    @FXML
    private ComboBox<SplitMethods> splitComboBox;

    @FXML
    private ProgressBar progressBar;

    protected double progress;

    public Node init(ApplicationContext applicationContext) throws IOException {
        this.applicationContext = applicationContext;
        Parent rootNode = FileUtils.loadFXML(this);
        return rootNode;
    }

    public void initialize() throws Exception {
        initComboBox(splitComboBox, SplitMethodsListCell.class, SplitMethods.values());
    }

    @FXML
    private void startProcessing() throws Exception {
        Thread thread = new Thread(new SplitTask());
        thread.start();
    }

    private class SplitTask implements Runnable {

        @Override
        public void run() {
            try {
                // TODO Lock Start Button before start and unlock after job finished
                // TODO Implement Cancel Button (maybe same button, but change caption)
                runWithExceptions();
                Platform.runLater(() -> applicationContext.showMessage("Split Images is done"));
            } catch (Exception e) {
                Platform.runLater(() -> applicationContext.showError("Error due to Split Images", e));
            }
        }

        private void runWithExceptions() throws Exception {
            SplitMethods splitMethod = splitComboBox.getSelectionModel().getSelectedItem();

            if (splitMethod == null) {
                return;
            }

            AbstractSplitter splitter = splitMethod.cl.newInstance();

            File inputFolder = applicationContext.getWorkFolder();
            File outputFolder = new File(inputFolder, "splitted");
            outputFolder.mkdir();

            File[] files = inputFolder.listFiles(new FilenameFilterImages(false));
            int amountOfImages = files.length;

            for (int i = 0; i < amountOfImages; i++) {
                File sourceFile = files[i];
                BufferedImage image = ImageIO.read(sourceFile);

                BufferedImage[] splittedImages = splitter.split(image);

                for (int j = 0; j < splittedImages.length; j++) {
                    BufferedImage splittedImage = splittedImages[j];

                    saveImageToFile(outputFolder, sourceFile.getName(), splittedImage, j);
                }

                progress = (double) (i + 1) / amountOfImages;
                Platform.runLater(new UpdateProgressTask());
            }
        }

        public void saveImageToFile(File outputFolder, String sourceFileName, BufferedImage image, int index) throws IOException {
            // Generating Output Filename
            String outputFileName = FileUtils.getFileName(sourceFileName);
            outputFileName += "-" + (index + 1);
            outputFileName += "." + FileUtils.getFileExtension(sourceFileName);

            // Saving Image
            File outputFile = new File(outputFolder, outputFileName);
            String formatName = FileUtils.getFileExtension(outputFileName);
            ImageIO.write(image, formatName, outputFile);
        }
    }

    public enum SplitMethods {
        Method2PagesOn1ImageHorizontally("2 pages on one Image horizontally", Split2PagesOn1ImageHorizontally.class);

        private String text;
        private Class<? extends AbstractSplitter> cl;

        private SplitMethods(String text, Class<? extends AbstractSplitter> cl) {
            this.text = text;
            this.cl = cl;
        }

        public String getText() {
            return text;
        }

        public Class<? extends AbstractSplitter> getCl() {
            return cl;
        }
    }

    public static class SplitMethodsListCell extends ListCell<SplitMethods> {
        @Override
        protected void updateItem(SplitMethods item, boolean empty) {
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
