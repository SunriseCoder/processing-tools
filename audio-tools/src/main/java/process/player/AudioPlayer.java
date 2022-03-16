package process.player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
import javafx.scene.paint.Paint;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.util.Duration;
import process.context.ApplicationContext;
import process.context.ApplicationEvents;
import process.context.ApplicationParameters;
import process.dto.Candle;
import utils.FileUtils;
import utils.MathUtils;

public class AudioPlayer {
    private static final int MOVE_BY_ARROW_DISTANCE = 15;
    private static final int VERTICAL_SCALE = 64 * 1024;
    private static final int MAX_HORIZONTAL_SCALE = 1024 * 1024;
    private static final int MAX_SAMPLE_WIDTH = 128;
    private static final int WAVEFORM_BORDER_WIDTH = 5;

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

    // Samples
    private SampleStorage sourceSampleStorage;
    private SampleStorage diffSampleStorage;
    private SampleStorage blockSampleStorage;
    private SampleStorage normSampleStorage;

    // Logic Parameters
    private long currentSamplePosition;
    private AudioPlayerSelection selection;

    // Visual Parameters
    private int scale;
    private int sampleWidth;
    private long imageOffset;

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
                sample = selection.isEndEmpty() ? sourceSampleStorage.getSampleCount() - 1 : selection.getEnd();
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
        System.out.println("Sample position: " + samplePosition);
        if (e.getButton().equals(MouseButton.PRIMARY)) {
            setPlaybackPosition(samplePosition);
        } else if (e.getButton().equals(MouseButton.SECONDARY)) {
            setSelection(samplePosition, null);
        }

        render();
    }

    private void handleImageScroll(ScrollEvent e) {
        // Calculating Sample Position on the Image before Scale
        int mouseX = (int) e.getX();
        double samplePosition = getImageToSample(mouseX);

        // Calculating Scale
        // Checking Zoom direction (In or Out)
        if (e.getDeltaY() < 0) {
            // Zooming Out (more samples on screen)
            // Checking that the Zoom is not fully Out yet
            if (scale < MAX_HORIZONTAL_SCALE) {
                if (sampleWidth > 1) {
                    // First reducing Sample Width
                    sampleWidth /= 2;
                } else {
                    // Then increasing amount of samples per one-pixel-line
                    scale *= 2;
                }
            }
        } else {
            // Zooming In (less samples on screen)
            // Checking that the Zoom is not fully In yet
            if (sampleWidth < MAX_SAMPLE_WIDTH) {
                if (scale > 1) {
                    // First reducing amount of samples per one-pixel-line
                    scale /= 2;
                } else {
                    // Then increasing Sample Width
                    sampleWidth *= 2;
                }
            }
        }

        // Calculating Sample Position on the Image after Scale
        // TODO Review and test next formulas
        long samplePositionOnImage = MathUtils.roundToLong(samplePosition * sampleWidth / scale + imageOffset);
        // Adjusting offset by Delta
        imageOffset += mouseX - samplePositionOnImage;

        System.out.println("Scale: " + scale + ", SampleWidth: " + sampleWidth);

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
        if (sample < 0 || sample >= sourceSampleStorage.getSampleCount()) {
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
        if (sourceSampleStorage == null) {
            return;
        }

        image.clear();

        // Add sampleWidth to the next formula
        int offset = MathUtils.roundToInt((double) -imageOffset / sampleWidth);
        int amount = MathUtils.ceilToInt(image.getWidth() / sampleWidth) + 1;

        GraphicsContext graphics = image.getGraphics();
        graphics.setLineWidth(1);

        // Wave Rendering
        //renderWaveform(candles, graphics, 300, 0);
        //renderWaveform(candles, graphics, 300, 300);
        renderWaveform(sourceSampleStorage.getCandles(scale, offset, amount), graphics, 300, 0, VERTICAL_SCALE, true);
        renderWaveform(diffSampleStorage.getCandles(scale, offset, amount), graphics, 150, 300, VERTICAL_SCALE / 2, false);
        renderWaveform(blockSampleStorage.getCandles(scale, offset, amount), graphics, 150, 450, 2, false);
        renderWaveform(normSampleStorage.getCandles(scale, offset, amount), graphics, 300, 600, VERTICAL_SCALE, true);

//        // Selection rendering
//        if (selection.isStartNotEmpty()) {
//            long selectionStart = getSampleToImage(selection.getStart());
//            long selectionEnd = selection.isEndEmpty() ? selectionStart : getSampleToImage(selection.getEnd());
//            graphics.setFill(Color.GREEN);
//            graphics.setGlobalAlpha(0.3);
//            graphics.fillRect(snap(selectionStart), snap(0), snap(selectionEnd - selectionStart + 1), snap(height));
//            graphics.setGlobalAlpha(1);
//        }
//
//        // Cursor Rendering
//        long cursorPosition = getSampleToImage(currentSamplePosition);
//        if (cursorPosition >= 0 && cursorPosition < image.getWidth()) {
//            graphics.setStroke(Color.RED);
//            graphics.strokeLine(snap(cursorPosition), snap(0), snap(cursorPosition), snap(height));
//        }
    }

    private void renderWaveform(Candle[] candles, GraphicsContext graphics, int height, int verticalOffset, int scaleMaxValue, boolean isSigned) {
        // Drawing Black horizontal borders
        graphics.setStroke(Color.BLACK);
        graphics.strokeLine(snap(0), snap(verticalOffset + WAVEFORM_BORDER_WIDTH - 1), snap(image.getWidth()), snap(verticalOffset + WAVEFORM_BORDER_WIDTH));
        if (isSigned) {
            graphics.strokeLine(snap(0), snap(verticalOffset + height / 2), snap(image.getWidth()), snap(verticalOffset + height / 2));
        }
        graphics.strokeLine(snap(0), snap(verticalOffset + height - WAVEFORM_BORDER_WIDTH + 1), snap(image.getWidth()), snap(verticalOffset + height - WAVEFORM_BORDER_WIDTH));

        // Rendering Wave itself
        graphics.setStroke(Color.BLUE);
        Candle lastCandle = null;
        int lastX = 0;
        for (int i = 0; i < candles.length; i++) {
            int x = i * sampleWidth + (int) (imageOffset % sampleWidth);
            if (x >= image.getWidth()) {
                break;
            }

            // Drawing Black Vertical Grid Lines
            if (sampleWidth >= 8) {
                Paint oldStroke = graphics.getStroke();
                graphics.setStroke(Color.BLACK);
                graphics.strokeLine(snap(x), snap(verticalOffset), snap(x), snap(verticalOffset + height));
                graphics.setStroke(oldStroke);
            }

            Candle candle = candles[i];
            if (candle == null) {
                continue;
            }

            // Drawing Candle
            int minValue = calculateFrameValueToImage(height, scaleMaxValue, candle.min, isSigned);
            int maxValue = calculateFrameValueToImage(height, scaleMaxValue, candle.max, isSigned);
            //System.out.println("Candle " + i + ": " + candle + ", minValue=" + minValue + ", maxValue=" + maxValue);
            graphics.strokeLine(snap(x), snap(verticalOffset + minValue), snap(x), snap(verticalOffset + maxValue));

            // Connecting previous with current Candles
            if (lastCandle != null) {
                int lastCandleLastValue = calculateFrameValueToImage(height, scaleMaxValue, lastCandle.last, isSigned);
                int currentCandleFirstValue = calculateFrameValueToImage(height, scaleMaxValue, candle.first, isSigned);
                graphics.strokeLine(snap(lastX), snap(verticalOffset + lastCandleLastValue), snap(x), snap(verticalOffset + currentCandleFirstValue));
            }
            lastCandle = candle;
            lastX = x;
        }
    }

    private int calculateFrameValueToImage(int imageHeight, int scaleMaxValue, int value, boolean isSigned) {
        int resultValue;
        if (isSigned) {
            resultValue = MathUtils.roundToInt(imageHeight / 2 - (double) value * (imageHeight - 2 * WAVEFORM_BORDER_WIDTH) / scaleMaxValue);
        } else {
            resultValue = MathUtils.roundToInt(imageHeight - WAVEFORM_BORDER_WIDTH - (double) value * (imageHeight - 2 * WAVEFORM_BORDER_WIDTH) / scaleMaxValue);
        }
        //resultValue = Math.min(resultValue, imageHeight / 2 - WAVEFORM_BORDER_WIDTH);
        return resultValue;
    }

    private double snap(double value) {
        return value + 0.5;
    }

    private long getSampleToImage(long samplePosition) {
        // TODO Add sampleWidth to this formula
        //return samplePosition / scale + imageOffset;
        //return samplePosition * sampleWidth / scale + imageOffset * sampleWidth;
        return MathUtils.roundToLong((double) samplePosition * sampleWidth / scale + imageOffset);
    }

    private long getImageToSample(long imagePosition) {
        // TODO Add sampleWidth to this formula
        //return (imagePosition - imageOffset) * scale;
        return (imagePosition - imageOffset) * scale / sampleWidth;
    }

    private long getSampleToPlayer(long samplePosition) {
        int sampleRate = MathUtils.roundToInt(sourceSampleStorage.getAudioFormat().getSampleRate());
        int player = MathUtils.roundToInt(1000 * samplePosition / sampleRate);
        return player;
    }

    private long getPLayerToSample(long playerPosition) {
        int sampleRate = MathUtils.roundToInt(sourceSampleStorage.getAudioFormat().getSampleRate());
        int sample = MathUtils.roundToInt(playerPosition * sampleRate / 1000);
        return sample;
    }

    private void calculateScale() {
        int width = (int) image.getWidth();
        long frameCount = sourceSampleStorage.getSampleCount();
        double rate = frameCount / width;
        double power = MathUtils.ceilToInt(Math.log(rate) / Math.log(2));
        scale = MathUtils.roundToInt(Math.pow(2, power));
        // TODO Calculate also sampleWidth, i.e. if there are very few frames, it should be more than 1
        sampleWidth = 1;
    }

    @FXML
    private void selectMediaFile() throws Exception {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Audio File");
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
        sourceSampleStorage = new SampleStorage();
        sourceSampleStorage.init(inputStream);

        diffSampleStorage = new SampleStorage();
        diffSampleStorage.setAudioFormat(sourceSampleStorage.getAudioFormat());
        calculateFramesDiff(sourceSampleStorage, diffSampleStorage);

        blockSampleStorage = new SampleStorage();
        List<BlockBoundaries> blocks = findSampleBlocks(diffSampleStorage, blockSampleStorage);

        normSampleStorage = new SampleStorage();
        normSampleStorage.setAudioFormat(blockSampleStorage.getAudioFormat());
        normalizeSamples(sourceSampleStorage, diffSampleStorage, blocks, normSampleStorage);

        // Calculating Scale for just opened File and Rendering
        calculateScale();
        render();
    }

    private void calculateFramesDiff(SampleStorage sourceSampleStorage, SampleStorage diffSampleStorage) {
        int[] sourceSamples = sourceSampleStorage.getSamples();
        int[] diffSamples = new int[sourceSamples.length];
        int lastSample = 0;
        for (int i = 0; i < sourceSamples.length; i++) {
            int currentSample = sourceSamples[i];
            int diffSample = Math.abs(currentSample - lastSample);
            diffSamples[i] = diffSample;
            lastSample = currentSample;
        }

        diffSampleStorage.setSamples(diffSamples);
    }

    private List<BlockBoundaries> findSampleBlocks(SampleStorage diffSampleStorage, SampleStorage blockSampleStorage) {
        System.out.println("Looking for blocks");
        int threshold = 400;
        double maxNoiseInsideBlock = 1.0;
        double minBlockSize = 1.0;
        int sampleRate = MathUtils.roundToInt(diffSampleStorage.getAudioFormat().getSampleRate());

        int[] diffSamples = diffSampleStorage.getSamples();
        int[] blockSamples = new int[diffSamples.length];
        List<BlockBoundaries> blocks = new ArrayList<>();

        int blockStart = 0;
        int silenceStart = 0;
        boolean isInsideBlock = false;
        int usefulSamplesCounter = 0;
        int noisesInsideBlockCounter = 0;
        for (int i = 0; i < diffSamples.length; i++) {
            int currentSamlpe = diffSamples[i];
            if (currentSamlpe < threshold) {
                // Is noise
                if (isInsideBlock) {
                    if (noisesInsideBlockCounter++ == 0) {
                        silenceStart = i;
                    }
                    if ((double) noisesInsideBlockCounter / sampleRate > maxNoiseInsideBlock) {
                        // Checking that silence size is
                        if ((double) (silenceStart - blockStart) / sampleRate >= minBlockSize) {
                            // Saving useful sound block
                            BlockBoundaries block = new BlockBoundaries(blockStart, silenceStart);
                            blocks.add(block);
                        }

                        isInsideBlock = false;
                    }
                }
            } else {
                // Is useful sound
                if (!isInsideBlock) {
                    blockStart = i;
                    isInsideBlock = true;
                }

                usefulSamplesCounter++;
                if (noisesInsideBlockCounter > 0) {
                    if ((double) usefulSamplesCounter / sampleRate >= minBlockSize) {
                        // If we had a lot of useful sounds during the noise, we are resetting the noise block detecting
                        noisesInsideBlockCounter = 0;
                    }
                }
            }
        }


        for (BlockBoundaries block : blocks) {
            for (int i = block.start; i <= block.end; i++) {
                blockSamples[i] = 1;
            }
        }

        blockSampleStorage.setSamples(blockSamples);
        return blocks;
    }

    private void normalizeSamples(SampleStorage sourceSampleStorage, SampleStorage diffSampleStorage, List<BlockBoundaries> blocks, SampleStorage normSampleStorage) {
        System.out.println("Normalizing");
        int[] sourceSamples = sourceSampleStorage.getSamples();
        int[] diffSamples = diffSampleStorage.getSamples();
        int[] normSamples = new int[sourceSamples.length];

        for (BlockBoundaries block : blocks) {
            int blockDiffSum = 0;
            for (int i = block.start; i <= block.end; i++) {
                blockDiffSum += diffSamples[i];
            }

            double blockAverageDiff = (double) blockDiffSum / (block.end - block.start + 1);
            double blockFactor = 400 / blockAverageDiff;
            for (int i = block.start; i <= block.end; i++) {
                int normalizedValue = blockFactor > 1 ? MathUtils.roundToInt(sourceSamples[i] * blockFactor) : sourceSamples[i];
                normalizedValue = Math.min(normalizedValue, 32767);
                normalizedValue = Math.max(normalizedValue, -32768);
                normSamples[i] = normalizedValue;
            }
        }

        normSampleStorage.setSamples(normSamples);
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
        if (selection.isStartEmpty() || selection.isEndEmpty() || sourceSampleStorage == null) {
            return null;
        }

        long selectionStart = getSampleToPlayer(selection.getStart());
        long selectionEnd = getSampleToPlayer(selection.getEnd());
        AudioPlayerSelection selectionInMilliseconds = new AudioPlayerSelection(selectionStart, selectionEnd);
        return selectionInMilliseconds;
    }

    public void setSelectionInMilliseconds(AudioPlayerSelection selectionInMilliseconds) {
        if (sourceSampleStorage == null) {
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

    private static class BlockBoundaries {
        private int start;
        private int end;

        public BlockBoundaries(int start, int end) {
            this.start = start;
            this.end = end;
        }
    }
}
