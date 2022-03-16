package utils;

import java.util.List;

public class PrimitiveUtils {

    public static int littleEndianByteArrayToInt(byte[] buffer, int position, int size) {
        int value = 0;
        for (int i = 0; i < size; i++) {
            int byteValue = buffer[position + i];
            if (i != size - 1) {
                byteValue &= 0xFF;
            }
            value += byteValue << i * 8;
        }
        return value;
    }

    public static byte[] intToBigEndianByteArray(int value, int sizeInBytes) {
        byte[] array = new byte[sizeInBytes];
        for (int i = 0; i < sizeInBytes; i++) {
            array[i] = (byte) ((value >> ((sizeInBytes - i) * 8)) & 0xFF);
        }
        return array;
    }

    public static byte[] intToLittleEndianByteArray(int value, int sizeInBytes) {
        byte[] array = new byte[sizeInBytes];
        for (int i = 0; i < sizeInBytes; i++) {
            array[i] = (byte) ((value >> (i * 8)) & 0xFF);
        }
        return array;
    }

    public static long bigEndianByteArrayToLong(byte[] bytes, int start, int size) {
        long value = 0;
        long bytePosition = 0;
        for (int i = start + size - 1; i >= start; i--, bytePosition++) {
            long byteValue = bytes[i];
            if (byteValue < 0) {
                byteValue += 256;
            }
            value += byteValue << bytePosition * 8;
        }
        return value;
    }

    public static int[] listToIntArray(List<Integer> list) {
        int[] array = new int[list.size()];

        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }

        return array;
    }
}
