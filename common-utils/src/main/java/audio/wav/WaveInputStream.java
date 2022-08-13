package audio.wav;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import adaptors.FrameBuffer;
import audio.api.FrameInputStream;
import utils.PrimitiveUtils;

public class WaveInputStream implements FrameInputStream, AutoCloseable {
    private static final int PARSE_DATA_CHUNK_SIZE = 4096;

    private File inputFile;
    private AudioFormat format;
    private AudioInputStream inputStream;
    private long framesCount;
    private int channel;

    private int frameSize;
    private int sampleSize;

    private FrameBuffer[] frameBuffers;

    // Service variables
    private byte[] parseDataBuffer = new byte[PARSE_DATA_CHUNK_SIZE];

    private WaveInputStream(AudioInputStream audioInputStream, File inputFile) throws UnsupportedAudioFileException {
        this(audioInputStream, -1, inputFile);
    }

    private WaveInputStream(AudioInputStream audioInputStream, int channel, File inputFile) throws UnsupportedAudioFileException {
        this.inputFile = inputFile;
        this.format = audioInputStream.getFormat();

        if (!format.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED)) {
            throw new UnsupportedAudioFileException("Only PCM-Signed Wav-files are supported");
        }
        if (format.isBigEndian()) {
            throw new UnsupportedAudioFileException("Big-Endian Wav-files are not supported");
        }

        this.frameSize = format.getFrameSize();
        this.sampleSize = format.getSampleSizeInBits() / 8;

        if (sampleSize > 4) {
            throw new UnsupportedAudioFileException("Only 8 to 32-bit Wav-files are supported");
        }

        this.inputStream = audioInputStream;
        this.framesCount = audioInputStream.getFrameLength();
        int channelCount = format.getChannels();
        this.frameBuffers = new FrameBuffer[channelCount];
        for (int i = 0; i < channelCount; i++) {
            this.frameBuffers[i] = new FrameBuffer();
        }

        this.channel = channel;
    }

    @Override
    public void reset() throws IOException, UnsupportedAudioFileException {
        if (channel == -1) {
            throw new IllegalStateException("Default channel was not set");
        }
        // Recreating input stream because JVM implementation does not support inputStream.reset();
        inputStream = AudioSystem.getAudioInputStream(inputFile);
        frameBuffers[channel].reset();
    }

    @Override
    public boolean available() throws IOException {
        if (channel == -1) {
            throw new IllegalStateException("Default channel was not set");
        }
        boolean available = available(channel);
        return available;
    }

    @Override
    public boolean available(int channel) throws IOException {
        FrameBuffer frameBuffer = frameBuffers[channel];
        boolean available = frameBuffer.available() > 0;
        available |= inputStream.available() > 0;
        return available;
    }

    @Override
    public AudioFormat getFormat() {
        return format;
    }

    @Override
    public int readFrame() throws IOException {
        if (channel == -1) {
            throw new IllegalStateException("Default channel was not set");
        }
        int value = readFrame(channel);
        return value;
    }

    @Override
    public int readFrame(int channel) throws IOException {
        FrameBuffer frameBuffer = frameBuffers[channel];

        if (frameBuffer.available() < 1 && inputStream.available() > 0) {
            parseFrameChunk(channel);
        }

        int value = frameBuffer.read();
        return value;
    }

    @Override
    public int readFrames(int[] frames) throws IOException {
        if (channel == -1) {
            throw new IllegalStateException("Default channel was not set");
        }
        int read = readFrames(channel, frames);
        return read;
    }

    @Override
    public int readFrames(int channel, int[] frames) throws IOException {
        FrameBuffer frameBuffer = frameBuffers[channel];
        int requiredFrames = frames.length;

        while (frameBuffer.available() < requiredFrames && inputStream.available() > 0) {
            parseFrameChunk(channel);
        }

        int read = frameBuffer.read(frames);
        return read;
    }

    private void parseFrameChunk(int channel) throws IOException {
        int read = inputStream.read(parseDataBuffer);

        if (read % frameSize != 0) {
            throw new IllegalStateException("Incomplete data in source stream");
        }

        int readPosition = 0;
        while (readPosition < read) {
            parseFrame(readPosition, channel);
            readPosition += frameSize;
        }
    }

    private void parseFrame(int readPosition, int channel) {
        int channelCount = format.getChannels();
        for (int ch = 0; ch < channelCount ; ch++) {
            // Don't parse data if the channel was excluded from processing
            if (this.channel != -1 && ch != channel) {
                continue;
            }
            int position = readPosition + ch * sampleSize;
            int value = PrimitiveUtils.littleEndianByteArrayToInt(parseDataBuffer, position, sampleSize);
            FrameBuffer frameBuffer = frameBuffers[ch];
            frameBuffer.push(value);
        }
    }

    @Override
    public long getFramesCount() {
        return framesCount;
    }

    @Override
    public void close() throws IOException {
        if (inputStream != null) {
            inputStream.close();
        }
    }

    public static WaveInputStream create(File inputFile) throws UnsupportedAudioFileException, IOException {
        WaveInputStream waveInputStream = create(inputFile, -1);
        return waveInputStream;
    }

    public static WaveInputStream create(File inputFile, int channel) throws UnsupportedAudioFileException, IOException {
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(inputFile);
        WaveInputStream waveInputStream = new WaveInputStream(audioInputStream, channel, inputFile);
        return waveInputStream;
    }
}
