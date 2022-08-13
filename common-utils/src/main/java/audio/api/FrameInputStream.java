package audio.api;

import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.UnsupportedAudioFileException;

public interface FrameInputStream {
    void reset() throws IOException, UnsupportedAudioFileException;
    boolean available() throws IOException;
    boolean available(int channel) throws IOException;
    AudioFormat getFormat();
    int readFrame() throws IOException;
    int readFrame(int channel) throws IOException;
    int readFrames(int[] frames) throws IOException;
    int readFrames(int channel, int[] frames) throws IOException;
    long getFramesCount();
    void close() throws IOException;
}
