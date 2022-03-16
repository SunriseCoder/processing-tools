package process.player;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.sound.sampled.AudioFormat;

import audio.api.FrameInputStream;
import process.dto.Candle;
import utils.MathUtils;

class SampleStorage {
    private static final int MIN_SCALE_FOR_PERMANENT_MAPS = 32;

    private AudioFormat audioFormat;
    private int[] samples;
    private Map<Integer, Candle[]> storage;

    public SampleStorage() throws IOException {
        storage = new HashMap<>();
    }

    public void init(FrameInputStream inputStream) throws IOException {
        audioFormat = inputStream.getFormat();
        long sampleCount = inputStream.getFramesCount();
        System.out.println("Sample Count: " + sampleCount);
        if (sampleCount > Integer.MAX_VALUE) {
            throw new RuntimeException("Number of the frames in the file is bigger than maximum value of int type");
        }
        samples = new int[(int) sampleCount];
        inputStream.readFrames(0, samples);
    }

    public AudioFormat getAudioFormat() {
        return audioFormat;
    }

    public void setAudioFormat(AudioFormat audioFormat) {
        this.audioFormat = audioFormat;
    }

    public long getSampleCount() {
        return samples.length;
    }

    public Candle[] getCandles(int scale, int offset, int amount) {
        //System.out.println("Offset: " + offset);
        Candle[] candles;
        if (scale >= MIN_SCALE_FOR_PERMANENT_MAPS) {
            candles = storage.get(scale);
            if (candles == null) {
                candles = createCandles(scale);
            }
            // Creating SubArray
            Candle[] subArray = new Candle[amount];
            int amountToCopy = Math.min(candles.length - offset, amount);
            amountToCopy = Math.min(amountToCopy, amountToCopy + offset);
            System.arraycopy(candles, offset < 0 ? 0 : offset, subArray, offset < 0 ? -offset : 0, amountToCopy);
            candles = subArray;
        } else {
            candles = createCandles(scale, offset, amount);
        }
        return candles;
    }

    private Candle[] createCandles(int scale) {
        Candle[] candles;
        if (scale == MIN_SCALE_FOR_PERMANENT_MAPS) {
            // Create the Lowest-Scale Candles based on Frames (Samples)
            int candleArrayLength = MathUtils.ceilToInt((double) samples.length / MIN_SCALE_FOR_PERMANENT_MAPS);
            candles = createCandlesFromSamples(MIN_SCALE_FOR_PERMANENT_MAPS, 0, candleArrayLength);
        } else {
            // Create the Candles based on Lower-Scaled Candles
            Candle[] lowerCandles = storage.get(scale / 2);
            if (lowerCandles == null) {
                lowerCandles = createCandles(scale / 2);
            }

            int candleArrayLength = MathUtils.ceilToInt((double) lowerCandles.length / 2);
            candles = new Candle[candleArrayLength];
            int candleIndex = 0;
            Candle candle = null;
            for (int lowerCandleIndex = 0; lowerCandleIndex < lowerCandles.length; lowerCandleIndex++) {
                Candle lowerCandle = lowerCandles[lowerCandleIndex];
                if (lowerCandleIndex % 2 == 0) {
                    candle = new Candle(lowerCandle.first, lowerCandle.last, lowerCandle.min, lowerCandle.max);
                    candles[candleIndex++] = candle;
                } else {
                    if (lowerCandle.min < candle.min) {
                        candle.min = lowerCandle.min;
                    }
                    if (lowerCandle.max > candle.max) {
                        candle.max = lowerCandle.max;
                    }
                    candle.last = lowerCandle.last;
                }
            }
        }

        storage.put(scale, candles);
        return candles;
    }

    private Candle[] createCandles(int scale, int offset, int amount) {
        Candle[] candles = createCandlesFromSamples(scale, offset, amount);
        return candles;
    }

    private Candle[] createCandlesFromSamples(int scale, int offset, int amount) {
        Candle[] candles;
        candles = new Candle[amount];
        int candleArrayIndex = 0;
        Candle candle = null;
        if (offset < 0) {
            candleArrayIndex = -offset;
            offset = 0;
        }
        for (int i = offset * scale; i < samples.length; i++) {
            int frame = samples[i];
            if (i % scale == 0) {
                if (candleArrayIndex >= candles.length) {
                    break;
                }
                candle = new Candle(frame, frame, frame, frame);
                candles[candleArrayIndex++] = candle;
            } else {
                if (frame < candle.min) {
                    candle.min = frame;
                }
                if (frame > candle.max) {
                    candle.max = frame;
                }
                candle.last = frame;
            }
        }
        return candles;
    }

    public int[] getSamples() {
        return samples;
    }

    public void setSamples(int[] samples) {
        this.samples = samples;
    }
}
