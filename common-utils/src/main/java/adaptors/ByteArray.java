package adaptors;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

public class ByteArray {
    private byte[] storage;
    private int realSize;

    public ByteArray() {
        this(4096);
    }

    public ByteArray(int arraySize) {
        storage = new byte[arraySize];
        realSize = 0;
    }

    public void clear() {
        realSize = 0;
    }

    public byte[] getStorage() {
        return storage;
    }

    public int size() {
        return realSize;
    }

    public synchronized ByteArray append(byte[] array) {
        append(array, 0, array.length);

        return this;
    }

    public synchronized ByteArray append(byte[] array, int offset, int size) {
        while (realSize + size > storage.length) {
            increaseArray();
        }

        System.arraycopy(array, offset, storage, realSize, size);
        realSize += size;

        return this;
    }

    private void increaseArray() {
        byte[] oldArray = storage;
        storage = new byte[oldArray.length * 2];
        System.arraycopy(oldArray, 0, storage, 0, realSize);
    }

    public synchronized boolean endsWith(byte[] array) {
        if (array.length > realSize) {
            return false;
        }

        int storageOffset = realSize - array.length;
        for (int i = 0; i < array.length; i++) {
            if (storage[storageOffset + i] != array[i]) {
                return false;
            }
        }

        return true;
    }

    public void writeToOutputStream(OutputStream outputStream) throws IOException {
        outputStream.write(storage, 0, realSize);
    }

    public byte[] createBytes() {
        byte[] bytes = new byte[realSize];
        System.arraycopy(storage, 0, bytes, 0, realSize);
        return bytes;
    }

    public String createString(String charsetName) throws UnsupportedEncodingException {
        String string = new String(storage, 0, realSize, charsetName);
        return string;
    }

    @Override
    public String toString() {
        byte[] buffer = new byte[realSize];
        System.arraycopy(storage, 0, buffer, 0, realSize);
        return buffer.toString();
    }
}
