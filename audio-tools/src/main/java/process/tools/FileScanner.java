package process.tools;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import audio.api.FrameInputStream;
import audio.api.FrameOutputStream;
import audio.api.FrameStreamProcessor;
import audio.wav.WaveInputStream;
import audio.wav.WaveOutputStream;
import process.dto.ChannelOperation;
import process.tools.adjust.FrameStreamAdjuster2;
import process.utils.AudioFormatHelper;
import progress.ProgressPrinter;
import utils.FileUtils;

public class FileScanner {
    private File inputFile;
    private File outputFile;
    private AudioFormat inputFormat;
    private FrameOutputStream outputStream;

    public void open(String inputFileName) throws IOException, UnsupportedAudioFileException {
        File file = FileUtils.checkAndGetFile(inputFileName);
        setInputFile(file);
    }

    public void open(String foldername, String filename) throws IOException, UnsupportedAudioFileException {
        File file = FileUtils.checkAndGetFile(foldername, filename);
        setInputFile(file);
    }

    private void setInputFile(File file) throws UnsupportedAudioFileException, IOException {
        this.inputFile = file;

        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(file);
        this.inputFormat = audioInputStream.getFormat();
    }

    public void setOutput(String outputFileName, int channelsCount) throws IOException, UnsupportedAudioFileException {
        AudioFormat outputFormat = AudioFormatHelper.copyFormat(inputFormat, channelsCount);
        setOutput(outputFileName, outputFormat);
    }

    public void setOutput(String outputFileName, AudioFormat outputFormat) throws IOException, UnsupportedAudioFileException {
        this.outputFile = FileUtils.createFile(outputFileName, true);
        this.outputStream = new WaveOutputStream(outputFile, outputFormat);
        this.outputStream.writeHeader();
    }

    public void setOutput(String foldername, String outputname, int channelsCount) throws IOException, UnsupportedAudioFileException {
        AudioFormat outputFormat = AudioFormatHelper.copyFormat(inputFormat, channelsCount);
        setOutput(foldername, outputname, outputFormat);
    }

    public void setOutput(String foldername, String outputname, AudioFormat outputFormat) throws IOException, UnsupportedAudioFileException {
        this.outputFile = FileUtils.createFile(foldername, outputname, true);
        this.outputStream = new WaveOutputStream(outputFile, outputFormat);
        this.outputStream.writeHeader();
    }

    public List<List<Integer>> calculateStatistics(int chunkSizeMs, Set<String> options) throws Exception {
        int frameRate = (int) inputFormat.getFrameRate();
        int chunkSize = frameRate * chunkSizeMs / 1000;
        int[] frameBuffer = new int[chunkSize];

        int channelCount = inputFormat.getChannels();
        List<List<Integer>> fileStatistics = new ArrayList<>();

        for (int channel = 0; channel < channelCount; channel++) {
            if (options.contains("m")) {
                fileStatistics.add(new ArrayList<>());
            }
            if (options.contains("d")) {
                fileStatistics.add(new ArrayList<>());
            }
        }

        FrameInputStream inputStream = WaveInputStream.create(inputFile);
        long framesCount = inputStream.getFramesCount();

        ProgressPrinter progressPrinter = new ProgressPrinter();
        progressPrinter.setTotal(framesCount);

        long processedFramesTotal = 0;
        do {
            for (int channel = 0; channel < channelCount; channel++) {
                int read = inputStream.readFrames(channel, frameBuffer);
                if (read > 0) {
                    StatsCalculator frameStatistics = new StatsCalculator();
                    frameStatistics.addData(frameBuffer, read);

                    int optionCouner = 0;
                    if (options.contains("m")) {
                        List<Integer> channelMeanings = fileStatistics.get(channel + optionCouner++);
                        channelMeanings.add(frameStatistics.getMathMeaning());
                    }

                    if (options.contains("d")) {
                        List<Integer> channelDeltas = fileStatistics.get(channel + optionCouner++);
                        channelDeltas.add(frameStatistics.getAverageDelta());
                    }
                }

                processedFramesTotal += read;
                progressPrinter.printProgress(processedFramesTotal, false);
            }
        } while (processedFramesTotal < framesCount);
        progressPrinter.printProgressFinished();
        close(inputStream);
        return fileStatistics;
    }

    public void process(List<ChannelOperation> operations, int chunkSizeMs) throws IOException, UnsupportedAudioFileException {
        int frameRate = (int) inputFormat.getFrameRate();
        int chunkSize = frameRate * chunkSizeMs / 1000;

        fileInfoPrint(operations);

        List<FrameStreamProcessor> processors = new ArrayList<>();
        for (ChannelOperation operation : operations) {

            int inputChannel = operation.getInputChannel();
            int outputChannel = operation.getOutputChannel();

            FrameInputStream inputStream = WaveInputStream.create(inputFile, inputChannel);

            FrameStreamProcessor processor;
            if (operation.isAdjust()) { // Need to adjust channel
                processor = new FrameStreamAdjuster2(inputFile, inputChannel, outputStream, outputChannel);
            } else {
                processor = new FrameStreamCopier(inputStream, outputStream, outputChannel);
            }
            processor.setPortionSize(chunkSize);
            ProgressPrinter progressPrinter = new ProgressPrinter();
            processor.setProgressPrinter(progressPrinter);
            processor.prepareOperation();
            processors.add(processor);
        }

        for (FrameStreamProcessor processor : processors) {
            processor.process();
        }

        close(processors);
    }

    private void fileInfoPrint(List<ChannelOperation> operations) {
        System.out.println("Input file: " + inputFile.getAbsolutePath());
        System.out.println("Output file: " + outputFile.getAbsolutePath());

        for (ChannelOperation operation : operations) {
            String operationText = operation.getInputChannel() + "-> " + operation.getOutputChannel();
            operationText += " (" + (operation.isAdjust() ? "adjust" : "copy") + ")";
            System.out.println(operationText);
        }
    }

    public void close() throws IOException {
        if (outputStream != null) {
            outputStream.close();
        }
    }

    private void close(List<FrameStreamProcessor> processors) {
        for (FrameStreamProcessor processor : processors) {
            processor.close();
        }
    }

    private void close(FrameInputStream inputStream) {
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) {
                // Just swallowing
            }
        }
    }
}
