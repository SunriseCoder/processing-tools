package process.player;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.sound.sampled.AudioFormat;

import app.utils.MathUtils;
import audio.api.FrameInputStream;
import wrappers.IntArrayWrapper;

class ScaledSampleStorage {
    private Map<Integer, IntArrayWrapper> storage;
    private AudioFormat audioFormat;
    private long frameCount;

    public ScaledSampleStorage(FrameInputStream inputStream, int minimalScale) throws IOException {
        storage = new HashMap<>();
        audioFormat = inputStream.getFormat();
        frameCount = inputStream.getFramesCount();
        init(inputStream, minimalScale);
    }

    private void init(FrameInputStream inputStream, int minimalScale) throws IOException {
        long framesCount = inputStream.getFramesCount();
        int arraySize = MathUtils.ceilToInt((double) framesCount / minimalScale);
        IntArrayWrapper arrayWrapper = new IntArrayWrapper(arraySize);
        int[] array = arrayWrapper.getArray();

        int[] buffer = new int[minimalScale];
        for (int outer = 0; outer < arraySize; outer++) {
            int value = 0;
            int read = inputStream.readFrames(buffer);
            for (int inner = 0; inner < read; inner++) {
                value += Math.abs(buffer[inner]);
            }
            value = MathUtils.roundToInt(value / read);
            array[outer] = value;
        }

        storage.put(minimalScale, arrayWrapper);
    }

    public AudioFormat getAudioFormat() {
        return audioFormat;
    }

    public long getFrameCount() {
        return frameCount;
    }

    public int[] getSamples(int scale) {
        IntArrayWrapper wrapper = getWrapper(scale);
        return wrapper.getArray();
    }

    private IntArrayWrapper getWrapper(int scale) {
        IntArrayWrapper wrapper = storage.get(scale);
        if (wrapper == null) {
            createWrapper(scale);
            wrapper = storage.get(scale);
        }

        return wrapper;
    }

    private IntArrayWrapper createWrapper(int scale) {
        IntArrayWrapper sourceWrapper = storage.get(scale / 2);
        if (sourceWrapper == null) {
            sourceWrapper = createWrapper(scale / 2);
        }

        int size = MathUtils.ceilToInt((double) sourceWrapper.getLength() / 2);
        IntArrayWrapper wrapper = new IntArrayWrapper(size);
        int[] sourceArray = sourceWrapper.getArray();
        int[] array = wrapper.getArray();
        for (int i = 0; i < array.length; i++) {
            int value = sourceArray[i * 2];
            double counter = 1;
            if (sourceArray.length > i * 2 + 1) {
                value += sourceArray[i * 2 + 1];
                counter++;
            }
            array[i] = MathUtils.roundToInt((double) value / counter);
        }

        storage.put(scale, wrapper);
        return wrapper;
    }
}
