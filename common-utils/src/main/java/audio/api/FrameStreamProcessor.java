package audio.api;

import java.io.IOException;

import javax.sound.sampled.UnsupportedAudioFileException;

import progress.ProgressPrinter;

public interface FrameStreamProcessor {
    void setPortionSize(int chunkSize);
    void setProgressPrinter(ProgressPrinter progressPrinter);

    void prepareOperation() throws IOException, UnsupportedAudioFileException;
    void process() throws IOException, UnsupportedAudioFileException;
    void close();
}
