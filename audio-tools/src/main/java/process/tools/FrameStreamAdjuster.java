package process.tools;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.sound.sampled.UnsupportedAudioFileException;

import audio.api.FrameInputStream;
import audio.api.FrameOutputStream;
import audio.api.FrameStreamProcessor;
import progress.ProgressPrinter;

public class FrameStreamAdjuster implements FrameStreamProcessor {
    private static final int SPEECH_NORMAL_MEANING = 4000;
    private static final int MAX_FACTOR = 10;
    private static final int MIN_VALUE = -32768;
    private static final int MAX_VALUE = 32767;
    private static final int NEIGHBOUR_CHUNK_NUMBER = 3;

    private FrameInputStream inputStream;
    private FrameOutputStream outputStream;
    private int outputChannel;

    private Map<Integer, Object> chunkMap;
    private int[] frameBuffer;
    private ProgressPrinter progressPrinter;
    private int totalFramesRead;

    private int decodingPosition;
    private int processingPosition;

    // Statistics data
    private int minFactor;
    private int maxFactor;

    public FrameStreamAdjuster(FrameInputStream inputStream, FrameOutputStream outputStream, int outputChannel) {
        this.inputStream = inputStream;
        this.outputStream = outputStream;
        this.outputChannel = outputChannel;
    }

    @Override
    public void setPortionSize(int chunkSize) {
        frameBuffer = new int[chunkSize];
    }

    @Override
    public void setProgressPrinter(ProgressPrinter progressPrinter) {
        this.progressPrinter = progressPrinter;
    }

    @Override
    public void prepareOperation() throws IOException, UnsupportedAudioFileException {
        progressPrinter.reset();
        progressPrinter.setTotal(inputStream.getFramesCount());
        totalFramesRead = 0;

        decodingPosition = 0;
        processingPosition = 0;
        chunkMap = new HashMap<>();
        minFactor = Integer.MAX_VALUE;
        maxFactor = Integer.MIN_VALUE;
    }

    @Override
    public void process() throws IOException, UnsupportedAudioFileException {
        while (totalFramesRead < inputStream.getFramesCount()) {
            // If there is something to read, reading and putting to map
            boolean available = inputStream.available();
            if (available) {
                int read = inputStream.readFrames(frameBuffer);
                int[] buffer = new int[read];
                System.arraycopy(frameBuffer, 0, buffer, 0, read);
                chunkMap.put(decodingPosition++, buffer);
                totalFramesRead += read;
            }

            // If there are not enough data for smooth analysis, skipping
            //available = inputStream.available(channel);
            if (!available && decodingPosition <= processingPosition + NEIGHBOUR_CHUNK_NUMBER) {
                return;
            }

            // Calculating smooth meaning and factor
            StatsCalculator statistics = new StatsCalculator();
            for (int i = processingPosition - NEIGHBOUR_CHUNK_NUMBER; i <= processingPosition + NEIGHBOUR_CHUNK_NUMBER; i++) {
                int[] buffer = (int[]) chunkMap.get(i);
                if (buffer != null) {
                    statistics.add(buffer);
                }
            }
            int smoothMean = statistics.getMathMeaning();

            int[] buffer = (int[]) chunkMap.get(processingPosition);
            if (smoothMean != 0) {
                // Adjusting current chunk
                double factor = (double) SPEECH_NORMAL_MEANING / smoothMean;
                //System.out.println("Position: " + position + ", factor: " + factor);
                if (factor > maxFactor) {
                    maxFactor = (int) factor;
                }
                if (factor < minFactor) {
                    minFactor = (int) factor;
                }
                if (factor > MAX_FACTOR) {
                    factor = MAX_FACTOR;
                }

                // Amplifying only. If the volume level is already above normal, doing nothing
                if (factor > 1) {
                    buffer = adjustFrameBuffer(buffer, factor);
                }
            }
            outputStream.write(outputChannel, buffer);

            // Removing old chunk, which is not needed anymore
            chunkMap.remove(processingPosition - NEIGHBOUR_CHUNK_NUMBER);
            processingPosition++;

            progressPrinter.printProgress(totalFramesRead, false);
        }
        progressPrinter.printProgressFinished();
    }

    private int[] adjustFrameBuffer(int[] sourceBuffer, double factor) {
        int[] adjustedBuffer = new int[sourceBuffer.length];
        for (int i = 0; i < sourceBuffer.length; i++) {
            int value = sourceBuffer[i];
            value = adjustValue(factor, value);
            adjustedBuffer[i] = value;
        }
        return adjustedBuffer;
    }

    private int adjustValue(double factor, int value) {
        value = (int) Math.round(value * factor);
        if (value < MIN_VALUE) {
            value = MIN_VALUE;
        }
        if (value > MAX_VALUE) {
            value = MAX_VALUE;
        }
        return value;
    }

    @Override
    public void close() {
        try {
            inputStream.close();
        } catch (IOException e) {
            // Just swallowing
        }
        try {
            outputStream.close();
        } catch (IOException e) {
            // Just swallowing
        }
    }
}
