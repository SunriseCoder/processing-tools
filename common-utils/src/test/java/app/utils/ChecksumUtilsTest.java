package app.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import app.collection.RMap;
import app.collection.wrap.RMapWrap;

public class ChecksumUtilsTest {

    @Test
    public void testGenerateChecksumsStringFromRMap() {
        // Preparing ChecksumAlgorithms, which are the basis for the ChecksumString generating
        List<String> checksumAlgorithms = new ArrayList<>();
        checksumAlgorithms.add("SHA-1");
        checksumAlgorithms.add("MD5");

        // Checking ChecksumMap is null
        try {
            ChecksumUtils.generateChecksumsString(checksumAlgorithms, null);
            fail("Checksum Map is null, must throw an exception");
        } catch (IllegalArgumentException e) {}

        Map<String, String> checksums = new HashMap<>();
        RMap<String, String> checksumsRMap = new RMapWrap<>(checksums);

        // Checking Exception on ChecksumAlgoriths is null
        try {
            ChecksumUtils.generateChecksumsString(null, checksumsRMap);
            fail("ChecksumAlgorithms is null, must throw an exception");
        } catch (IllegalArgumentException e) {}

        // Checking both ChecksumAlgorithms and ChecksumMap are empty
        assertEquals("", ChecksumUtils.generateChecksumsString(new ArrayList<>(), checksumsRMap));

        // Checking Exception on ChecksumMap is empty
        try {
            ChecksumUtils.generateChecksumsString(checksumAlgorithms, checksumsRMap);
            fail("In the Checksum Map there are no required entries, must throw an exception");
        } catch (IllegalStateException e) {}

        // Checking Exception on not all the required entries are in the ChecksumMap
        checksums.put("MD5", "123");
        try {
            ChecksumUtils.generateChecksumsString(checksumAlgorithms, checksumsRMap);
            fail("In the Checksum Map there is no required entry, must throw an exception");
        } catch (IllegalStateException e) {}

        // Checking All required entries are in place
        checksums.put("SHA-1", "56789");
        assertEquals("SHA-1:56789;MD5:123", ChecksumUtils.generateChecksumsString(checksumAlgorithms, checksumsRMap));

        // Checking All the required entries are in place and there are also some extra entries
        checksums.put("SHA-512", "3465484");
        assertEquals("SHA-1:56789;MD5:123", ChecksumUtils.generateChecksumsString(checksumAlgorithms, checksumsRMap));

        // Checking that the ChecksumString generating order is really based on ChecksumAlgorithms' order
        checksumAlgorithms.sort(Comparator.naturalOrder());
        assertEquals("MD5:123;SHA-1:56789", ChecksumUtils.generateChecksumsString(checksumAlgorithms, checksumsRMap));

        // Checking that when ChecksumAlgorithms is empty, the empty ChecksumString will be generated
        assertEquals("", ChecksumUtils.generateChecksumsString(new ArrayList<>(), checksumsRMap));
    }

    @Test
    public void testGenerateSizeAndChecksumsStringFromRMap() {
        // Preparing ChecksumAlgorithms, which are the basis for the ChecksumString generating
        List<String> checksumAlgorithms = new ArrayList<>();
        checksumAlgorithms.add("SHA-1");
        checksumAlgorithms.add("MD5");

        long size = 347;

        // Checking ChecksumMap is null
        try {
            ChecksumUtils.generateSizeAndChecksumsString(checksumAlgorithms, size, null);
            fail("Checksum Map is null, must throw an exception");
        } catch (IllegalArgumentException e) {}

        Map<String, String> checksums = new HashMap<>();
        RMap<String, String> checksumsRMap = new RMapWrap<>(checksums);

        // Checking Exception on ChecksumAlgoriths is null
        try {
            ChecksumUtils.generateSizeAndChecksumsString(null, size, checksumsRMap);
            fail("ChecksumAlgorithms is null, must throw an exception");
        } catch (IllegalArgumentException e) {}

        // Checking both ChecksumAlgorithms and ChecksumMap are empty
        assertEquals("Size:" + size, ChecksumUtils.generateSizeAndChecksumsString(new ArrayList<>(), size, checksumsRMap));

        // Checking Exception on ChecksumMap is empty
        try {
            ChecksumUtils.generateSizeAndChecksumsString(checksumAlgorithms, size, checksumsRMap);
            fail("In the Checksum Map there are no required entries, must throw an exception");
        } catch (IllegalStateException e) {}

        // Checking Exception on not all the required entries are in the ChecksumMap
        checksums.put("MD5", "123");
        try {
            ChecksumUtils.generateSizeAndChecksumsString(checksumAlgorithms, size, checksumsRMap);
            fail("In the Checksum Map there is no required entry, must throw an exception");
        } catch (IllegalStateException e) {}

        // Checking All required entries are in place
        checksums.put("SHA-1", "56789");
        assertEquals("Size:" + size + ";SHA-1:56789;MD5:123", ChecksumUtils.generateSizeAndChecksumsString(checksumAlgorithms, size, checksumsRMap));

        // Checking All the required entries are in place and there are also some extra entries
        checksums.put("SHA-512", "3465484");
        assertEquals("Size:" + size + ";SHA-1:56789;MD5:123", ChecksumUtils.generateSizeAndChecksumsString(checksumAlgorithms, size, checksumsRMap));

        // Checking that the ChecksumString generating order is really based on ChecksumAlgorithms' order
        checksumAlgorithms.sort(Comparator.naturalOrder());
        assertEquals("Size:" + size + ";MD5:123;SHA-1:56789", ChecksumUtils.generateSizeAndChecksumsString(checksumAlgorithms, size, checksumsRMap));

        // Checking that when ChecksumAlgorithms is empty, the empty ChecksumString will be generated
        assertEquals("Size:" + size, ChecksumUtils.generateSizeAndChecksumsString(new ArrayList<>(), size, checksumsRMap));
    }
}
