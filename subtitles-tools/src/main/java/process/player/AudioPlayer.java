package process.player;

import java.io.File;
import java.io.IOException;

import app.utils.FileUtils;
import app.utils.MathUtils;
import audio.wav.WaveInputStream;
import components.containers.CanvasPane;
import javafx.beans.Observable;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaPlayer.Status;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.util.Duration;
import process.context.ApplicationContext;
import process.context.ApplicationEvents;
import process.context.ApplicationParameters;

public class AudioPlayer {
    private static final int MOVE_BY_ARROW_DISTANCE = 15;
    private static final int VERTICAL_SCALE = 16 * 1024;
    private static final int MIN_HORIZONTAL_SCALE = 32;
    private static final int MAX_HORIZONTAL_SCALE = 1024 * 1024;

    private ApplicationContext applicationContext;

    private Parent root;
    @FXML
    private Slider volumeSlider;
    @FXML
    private GridPane gridPane;
    @FXML
    private TextField openMediaFileTextField;
    private CanvasPane image;

    // Components
    private File currentMediaFile;
    private MediaPlayer mediaPlayer;
    private ScaledSampleStorage sampleStorage;

    // Logic Parameters
    private long currentSamplePosition;
    private AudioPlayerSelection selection;

    // Visual Parameters
    private int scale;
    private int imageOffset;

    // Temporary variables
    private int mouseImagePosition;

    public AudioPlayer() {
        selection = new AudioPlayerSelection();
    }

    public Node createUI(ApplicationContext applicationContext) throws IOException {
        this.applicationContext = applicationContext;
        applicationContext.addEventListener(ApplicationEvents.WorkMediaFileChanged,
                value -> handleStartMediaFileChanged(value));

        root = FileUtils.loadFXML(this);

        root.setFocusTraversable(true);
        root.setOnMousePressed(e -> requestFocus());
        root.setOnKeyPressed(e -> handleKeyPressed(e));

        image = new CanvasPane();
        image.setMinHeight(200);
        GridPane.setColumnSpan(image, 12);
        GridPane.setVgrow(image, Priority.ALWAYS);
        gridPane.getChildren().add(image);

        image.widthProperty().addListener(e -> render());
        image.heightProperty().addListener(e -> render());

        image.setOnMousePressed(e -> handleImageMousePressed(e));
        image.setOnScroll(e -> handleImageScroll(e));
        image.setOnMouseDragged(e -> handleImageMouseDragged(e));

        return root;
    }

    public void restoreComponents() {
        String volumeString = applicationContext.getParameterValue(ApplicationParameters.AudioPlayerVolume);
        if (volumeString != null) {
            double volume = Double.parseDouble(volumeString);
            volumeSlider.setValue(volume);
        }

        volumeSlider.valueProperty().addListener(e -> {
            String volume = String.valueOf(volumeSlider.getValue());
            applicationContext.setParameterValue(ApplicationParameters.AudioPlayerVolume, volume);
        });
    }

    private void handleStartMediaFileChanged(Object value) {
        try {
            handleChangeMediaFile((File) value);
        } catch (Exception e) {
            applicationContext.showError("Could not open file", e);
        }
    }

    private void handleKeyPressed(KeyEvent e) {
        switch (e.getCode()) {
            // Toggle Play/Pause
            case SPACE:
                if (mediaPlayer != null) {
                    if (mediaPlayer.getStatus().equals(Status.PLAYING)) {
                        handlePause();
                    } else {
                        // TODO MediaPlayer glitch - don't fire Play Events after first press of Play on UI
                        mediaPlayer.play();
                    }
                }
                break;

            // Set Select Start/End
            case INSERT:
                handleSetSelectionStart();
                break;
            case PAGE_UP:
                handleSetSelectionEnd();
                break;

            // Reset Selection
            case DELETE:
                selection.reset();
                render();
                break;

            // Move to Start/End of Selection or Whole File
            case HOME:
                long sample = selection.isStartEmpty() ? 0 : selection.getStart();
                setPlaybackPosition(sample);
                break;
            case END:
                sample = selection.isEndEmpty() ? sampleStorage.getFrameCount() - 1 : selection.getEnd();
                setPlaybackPosition(sample);
                break;

            // Short Cursor Move Forward/Backward
            case LEFT:
                sample = currentSamplePosition - MOVE_BY_ARROW_DISTANCE * scale;
                setPlaybackPosition(sample);
                break;
            case RIGHT:
                sample = currentSamplePosition + MOVE_BY_ARROW_DISTANCE * scale;
                setPlaybackPosition(sample);
                break;
            default:
                break;
        }
    }

    private void handleImageMousePressed(MouseEvent e) {
        mouseImagePosition = (int) e.getX();

        long samplePosition = getImageToSample((int) e.getX());
        if (e.getButton().equals(MouseButton.PRIMARY)) {
            setPlaybackPosition(samplePosition);
        } else if (e.getButton().equals(MouseButton.SECONDARY)) {
            setSelection(samplePosition, null);
        }

        render();
    }

    private void handleImageScroll(ScrollEvent e) {
        // Calculating Sample Position on the Image before Scale
        double sample = (e.getX() - imageOffset) * scale;

        // Calculating Scale
        scale = e.getDeltaY() < 0 ? scale * 2 : scale / 2;
        // Adjust Scale between the Boundaries
        scale = Math.max(scale, MIN_HORIZONTAL_SCALE);
        scale = Math.min(scale, MAX_HORIZONTAL_SCALE);

        // Calculating Sample Position on the Image after Scale
        int samplePositionOnImage = MathUtils.roundToInt(sample / scale + imageOffset);
        // Adjusting offset by Delta
        imageOffset += e.getX() - samplePositionOnImage;

        render();
    }

    private void handleImageMouseDragged(MouseEvent e) {
        if (e.getButton().equals(MouseButton.PRIMARY)) {
            int deltaX = (int) (e.getX() - mouseImagePosition);
            mouseImagePosition = (int) e.getX();
            imageOffset += deltaX;
        } else if (e.getButton().equals(MouseButton.SECONDARY)) {
            long start = getImageToSample(mouseImagePosition);
            long end = getImageToSample((int) e.getX());
            setSelection(start, end);
        }

        render();
    }

    private void setPlaybackPosition(long sample) {
        if (sample < 0 || sample >= sampleStorage.getFrameCount()) {
            return;
        }

        setMediaPlayerPlaybackPosition(sample);
        setSamplePosition(sample);
    }

    private void setMediaPlayerPlaybackPosition(long sample) {
        long milliseconds = getSampleToPlayer(sample);
        mediaPlayer.seek(new Duration(milliseconds));
    }

    private void render() {
        if (sampleStorage == null) {
            return;
        }

        image.clear();

        int[] samples = sampleStorage.getSamples(scale);

        int width = Math.min((int) image.getWidth(), samples.length + imageOffset);
        int height = (int) image.getHeight();

        // Wave Rendering
        GraphicsContext graphics = image.getGraphics();
        graphics.setStroke(Color.BLUE);
        for (int x = 0; x < width; x++) {
            int index = x - imageOffset;
            if (index < 0 || index >= samples.length) {
                continue;
            }

            int value = MathUtils.roundToInt((double) samples[index] * height / VERTICAL_SCALE);
            graphics.strokeLine(x, (height - value) / 2, x, (height + value) / 2);
        }

        // Selection rendering
        if (selection.isStartNotEmpty()) {
            long selectionStart = getSampleToImage(selection.getStart());
            long selectionEnd = selection.isEndEmpty() ? selectionStart : getSampleToImage(selection.getEnd());
            graphics.setFill(Color.GREEN);
            graphics.setGlobalAlpha(0.3);
            graphics.fillRect(selectionStart, 0, selectionEnd - selectionStart + 1, height);
            graphics.setGlobalAlpha(1);
        }

        // Cursor Rendering
        long cursorPosition = getSampleToImage(currentSamplePosition);
        if (cursorPosition >= 0 && cursorPosition < image.getWidth()) {
            graphics.setStroke(Color.RED);
            graphics.strokeLine(cursorPosition, 0, cursorPosition, height);
        }
    }

    private long getSampleToImage(long samplePosition) {
        return samplePosition / scale + imageOffset;
    }

    private long getImageToSample(long imagePosition) {
        return (imagePosition - imageOffset) * scale;
    }

    private long getSampleToPlayer(long samplePosition) {
        int sampleRate = MathUtils.roundToInt(sampleStorage.getAudioFormat().getSampleRate());
        int player = MathUtils.roundToInt(1000 * samplePosition / sampleRate);
        return player;
    }

    private long getPLayerToSample(long playerPosition) {
        int sampleRate = MathUtils.roundToInt(sampleStorage.getAudioFormat().getSampleRate());
        int sample = MathUtils.roundToInt(playerPosition * sampleRate / 1000);
        return sample;
    }

    private void calculateScale() {
        int width = (int) image.getWidth();
        long frameCount = sampleStorage.getFrameCount();
        double rate = frameCount / width;
        double power = MathUtils.ceilToInt(Math.log(rate) / Math.log(2));
        scale = MathUtils.roundToInt(Math.pow(2, power));
    }

    @FXML
    private void selectMediaFile() throws Exception {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Folder with Images");
        ExtensionFilter filter = new ExtensionFilter("Wave files (*.wav)", "*.wav");
        fileChooser.getExtensionFilters().add(filter);

        if (currentMediaFile != null) {
            fileChooser.setInitialDirectory(currentMediaFile.getParentFile());
        }

        File newFile = fileChooser.showOpenDialog(null);

        if (newFile != null && newFile.exists() && !newFile.isDirectory()) {
            handleChangeMediaFile(newFile);
        }
    }

    private void handleChangeMediaFile(File file) throws Exception {
        currentMediaFile = file;
        openMediaFileTextField.setText(file.getAbsolutePath());

        // Saving Work File to System Configuration
        applicationContext.setParameterValue(ApplicationParameters.MediaWorkFile, file.getAbsolutePath());

        // Creating new Media and set it to new MediaPlayer
        Media media = new Media(file.toURI().toString());
        mediaPlayer = new MediaPlayer(media);
        mediaPlayer.currentTimeProperty().addListener((e) -> handleMediaPlayerPlaybackPositionChanged(e));
        mediaPlayer.volumeProperty().bind(volumeSlider.valueProperty());

        // Creating Sample Storage
        WaveInputStream inputStream = WaveInputStream.create(file, 0);
        sampleStorage = new ScaledSampleStorage(inputStream, MIN_HORIZONTAL_SCALE);

        // Calculating Scale for just opened File and Rendering
        calculateScale();
        render();
    }

    private void handleMediaPlayerPlaybackPositionChanged(Observable e) {
        if (mediaPlayer.getStatus().equals(Status.PAUSED)) {
            return;
        }

        // Calculating Sample Position
        long playerPosition = MathUtils.roundToLong(mediaPlayer.getCurrentTime().toMillis());
        long samplePosition = getPLayerToSample(playerPosition);

        // Adjusting that Sample Position don't come outside Selected Interval
        if (selection.isEndNotEmpty() && samplePosition > selection.getEnd()) {
            samplePosition = selection.getEnd();
            mediaPlayer.pause();
        }

        if (!mediaPlayer.getStatus().equals(Status.PLAYING)) {
            return;
        }

        // Firing Play Position Change Event
        playerPosition = getSampleToPlayer(samplePosition);
        applicationContext.fireEvent(ApplicationEvents.AudioPlayerOnPlay, playerPosition);

        // Setting Sample Position
        setSamplePosition(samplePosition);
    }

    @FXML
    public void handlePlay() {
        if (mediaPlayer != null) {
            if (selection.isStartNotEmpty() && selection.isEndNotEmpty()) {
                setPlaybackPosition(selection.getStart());
            }
            mediaPlayer.play();
        }
    }

    @FXML
    public void handlePause() {
        if (mediaPlayer != null) {
            long playerPosition = MathUtils.roundToLong(mediaPlayer.getCurrentTime().toMillis());
            mediaPlayer.pause();
            long samplePosition = getPLayerToSample(playerPosition);
            setSamplePosition(samplePosition);
        }
    }

    @FXML
    private void handleToSelectionStart() {
        if (selection.isStartNotEmpty()) {
            setPlaybackPosition(selection.getStart());
        }
    }

    @FXML
    private void handleToSelectionEnd() {
        if (selection.isEndNotEmpty()) {
            setPlaybackPosition(selection.getEnd());
        }
    }

    @FXML
    private void handleSetSelectionStart() {
        setSelection(currentSamplePosition, null);
        render();
    }

    @FXML
    private void handleSetSelectionEnd() {
        if (selection.isEndNotEmpty() && selection.getEnd().equals(currentSamplePosition)) {
            selection.setEnd(null);
        } else {
            selection.setEnd(currentSamplePosition);
        }

        render();
    }

    public AudioPlayerSelection getSelectionInMilliseconds() {
        if (selection.isStartEmpty() || selection.isEndEmpty() || sampleStorage == null) {
            return null;
        }

        long selectionStart = getSampleToPlayer(selection.getStart());
        long selectionEnd = getSampleToPlayer(selection.getEnd());
        AudioPlayerSelection selectionInMilliseconds = new AudioPlayerSelection(selectionStart, selectionEnd);
        return selectionInMilliseconds;
    }

    public void setSelectionInMilliseconds(AudioPlayerSelection selectionInMilliseconds) {
        if (sampleStorage == null) {
            return;
        }

        long selectionStart = getPLayerToSample(selectionInMilliseconds.getStart());
        long selectionEnd = getPLayerToSample(selectionInMilliseconds.getEnd());

        setSelection(selectionStart, selectionEnd);
    }

    private void setSamplePosition(long position) {
        currentSamplePosition = position;
        adjustImageOffset();
        render();
    }

    private void adjustImageOffset() {
        // Calculating offset, that the Cursor don't go outside the Window
        long imagePosition = getSampleToImage(currentSamplePosition);
        int imageWidth = (int) image.getWidth();
        if (imagePosition > 0.9 * imageWidth) {
            imageOffset -= imagePosition - 50;
        } else if (imagePosition < 50) {
            imageOffset += 50 - imagePosition;
        }
    }

    private void setSelection(Long start, Long end) {
        // TODO Add checks that both boundaries of the selection are inside the Sample Area
        selection.setStart(start);
        selection.setEnd(end);
        render();
    }

    public void resetSelectionInterval() {
        selection.reset();
        render();
    }

    public void requestFocus() {
        root.requestFocus();
    }
}
