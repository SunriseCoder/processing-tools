package app.utils;

import java.util.ArrayList;
import java.util.List;

import app.collection.RMap;

public class ChecksumUtils {

    public static String generateChecksumsString(List<String> checksumAlgorithms, RMap<String, String> checksums) {
        if (checksumAlgorithms == null) {
            throw new IllegalArgumentException("checksumAlgorithms cannot be null");
        }

        if (checksums == null) {
            throw new IllegalArgumentException("checksums cannot be null");
        }

        List<String> checksumStrings = new ArrayList<>();

        for (String algorithm : checksumAlgorithms) {
            if (!checksums.containsKey(algorithm)) {
                throw new IllegalStateException("There is no checksum entry for algorithm: " + algorithm);
            }

            String checksumValue = checksums.get(algorithm);
            String checksumString = algorithm + ":" + checksumValue;
            checksumStrings.add(checksumString);
        }

        String result = String.join(";", checksumStrings);
        return result;
    }

    public static String generateSizeAndChecksumsString(List<String> checksumAlgorithms, long size, RMap<String, String> checksums) {
        if (checksumAlgorithms == null) {
            throw new IllegalArgumentException("checksumAlgorithms cannot be null");
        }

        if (checksums == null) {
            throw new IllegalArgumentException("checksums cannot be null");
        }

        List<String> checksumStrings = new ArrayList<>();

        checksumStrings.add("Size:" + size);

        for (String algorithm : checksumAlgorithms) {
            if (!checksums.containsKey(algorithm)) {
                throw new IllegalStateException("There is no checksum entry for algorithm: " + algorithm);
            }

            String checksumValue = checksums.get(algorithm);
            String checksumString = algorithm + ":" + checksumValue;
            checksumStrings.add(checksumString);
        }

        String result = String.join(";", checksumStrings);
        return result;
    }
}
