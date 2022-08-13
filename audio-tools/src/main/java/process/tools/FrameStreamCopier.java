package process.tools;

import java.io.IOException;

import javax.sound.sampled.UnsupportedAudioFileException;

import audio.api.FrameInputStream;
import audio.api.FrameOutputStream;
import audio.api.FrameStreamProcessor;
import progress.ProgressPrinter;

public class FrameStreamCopier implements FrameStreamProcessor {
    private FrameInputStream inputStream;
    private FrameOutputStream outputStream;
    private int outputChannel;

    // Staff variables
    private int[] frameBuffer;
    private ProgressPrinter progressPrinter;
    private long totalFramesRead;

    public FrameStreamCopier(FrameInputStream inputStream, FrameOutputStream outputStream, int outputChannel) {
        this.inputStream = inputStream;
        this.outputStream = outputStream;
        this.outputChannel = outputChannel;
    }

    @Override
    public void setPortionSize(int chunkSize) {
        this.frameBuffer = new int[chunkSize];
    }

    @Override
    public void setProgressPrinter(ProgressPrinter progressPrinter) {
        this.progressPrinter = progressPrinter;
    }

    @Override
    public void prepareOperation() {
        progressPrinter.reset();
        progressPrinter.setTotal(inputStream.getFramesCount());
        totalFramesRead = 0;
    }

    @Override
    public void process() throws IOException, UnsupportedAudioFileException {
        long totalFrames = inputStream.getFramesCount();
        System.out.println("StreamCopier - total frames: " + totalFrames);
        while (totalFramesRead < totalFrames) {
            int read = inputStream.readFrames(frameBuffer);
            if (read > 0) {
                outputStream.write(outputChannel, frameBuffer, 0, read);
            }

            totalFramesRead += read;
            progressPrinter.printProgress(totalFramesRead, false);
        }
        progressPrinter.printProgressFinished();
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
