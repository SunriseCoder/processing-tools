package audio.api;

import java.io.IOException;

import javax.sound.sampled.UnsupportedAudioFileException;

import app.progress.SimpleProgressPrinter;

public interface FrameStreamProcessor {
    void setPortionSize(int chunkSize);
    void setProgressPrinter(SimpleProgressPrinter progressPrinter);

    void prepareOperation() throws IOException, UnsupportedAudioFileException;
    void process() throws IOException, UnsupportedAudioFileException;
    void close();
}
