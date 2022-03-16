package utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PrimitiveUtilsTest {

    @Test
    public void testBigEndianByteArrayToLong() {
        assertEquals(5, PrimitiveUtils.bigEndianByteArrayToLong(new byte[] {0, 0, 0, 5}, 0, 4));
        assertEquals(793, PrimitiveUtils.bigEndianByteArrayToLong(new byte[] {0, 0, 3, 25}, 0, 4));
    }
}
