package digest;

import static org.junit.Assert.assertArrayEquals;

import java.security.MessageDigest;

import org.junit.Test;

import app.digest.XorMessageDigest;

public class XorMessageDigestTest {

    @Test
    public void testEmptyDigest() {
        MessageDigest messageDigest = new XorMessageDigest(4);
        byte[] digest = messageDigest.digest();
        assertArrayEquals(new byte[] { 0, 0, 0, 0 }, digest);
    }

    @Test
    public void testSimpleZeroDigest() {
        MessageDigest messageDigest = new XorMessageDigest(4);
        messageDigest.update((byte) 0);
        byte[] digest = messageDigest.digest();
        assertArrayEquals(new byte[] { 0, 0, 0, 0 }, digest);
    }

    @Test
    public void testSimpleOneByteDigest() {
        MessageDigest messageDigest = new XorMessageDigest(4);
        messageDigest.update((byte) 5);
        byte[] digest = messageDigest.digest();
        assertArrayEquals(new byte[] { 5, 0, 0, 0 }, digest);
    }

    @Test
    public void testSimpleTwoBytesDigest() {
        MessageDigest messageDigest = new XorMessageDigest(4);
        messageDigest.update((byte) 5);
        messageDigest.update((byte) 15);
        byte[] digest = messageDigest.digest();
        assertArrayEquals(new byte[] { 5, 15, 0, 0 }, digest);
    }

    @Test
    public void testSimpleByteArrayDigest() {
        MessageDigest messageDigest = new XorMessageDigest(4);
        messageDigest.update(new byte[] { 5, 15 });
        byte[] digest = messageDigest.digest();
        assertArrayEquals(new byte[] { 5, 15, 0, 0 }, digest);
    }

    @Test
    public void testSimpleByteArrayDigestWithOffsetAndLength() {
        MessageDigest messageDigest = new XorMessageDigest(4);
        messageDigest.update(new byte[] { 5, 15, 25 }, 1, 1);
        byte[] digest = messageDigest.digest();
        assertArrayEquals(new byte[] { 15, 0, 0, 0 }, digest);
    }

    @Test
    public void testSimpleByteArrayDigestWithOffsetAndLengthAndOverflow() {
        MessageDigest messageDigest = new XorMessageDigest(4);
        messageDigest.update(new byte[] { 5, 15, 25, 35, 45, 55, 65 }, 1, 5);
        byte[] digest = messageDigest.digest();
        assertArrayEquals(new byte[] { 56, 25, 35, 45 }, digest);
    }

    @Test
    public void testLongerByteArrayDigest() {
        MessageDigest messageDigest = new XorMessageDigest(4);
        messageDigest.update(new byte[] { 5, 15, 25, 35, 45 });
        byte[] digest = messageDigest.digest();
        assertArrayEquals(new byte[] { 40, 15, 25, 35 }, digest);
    }

    @Test
    public void testLongerByteArrayWithNegativeValuesDigest() {
        MessageDigest messageDigest = new XorMessageDigest(4);
        messageDigest.update(new byte[] { (byte) 247, (byte) 248, 25, 35, 45, (byte) 215 });
        byte[] digest = messageDigest.digest();
        assertArrayEquals(new byte[] { (byte) 218, 47, 25, 35 }, digest);
    }

    @Test
    public void testExplicitResetByReset() {
        MessageDigest messageDigest = new XorMessageDigest(4);
        messageDigest.update(new byte[] { (byte) 247, (byte) 248, 25, 35, 45, (byte) 215 });
        messageDigest.reset();
        byte[] digest = messageDigest.digest();
        assertArrayEquals(new byte[] { 0, 0, 0, 0 }, digest);
    }

    @Test
    public void testExplicitResetByUpdateAfterReset() {
        MessageDigest messageDigest = new XorMessageDigest(4);
        messageDigest.update(new byte[] { (byte) 247, (byte) 248, 25, 35, 45, (byte) 215 });
        messageDigest.reset();

        messageDigest.update(new byte[] { 5, 15, 25, 35, 45, 55, 65 }, 1, 5);
        byte[] digest = messageDigest.digest();
        assertArrayEquals(new byte[] { 56, 25, 35, 45 }, digest);
    }

    @Test
    public void testImplicitResetByDigest() {
        MessageDigest messageDigest = new XorMessageDigest(4);
        messageDigest.update(new byte[] { (byte) 247, (byte) 248, 25, 35, 45, (byte) 215 });
        byte[] digest = messageDigest.digest(); // Implicit reset
        digest = messageDigest.digest(); // Empty digest
        assertArrayEquals(new byte[] { 0, 0, 0, 0 }, digest);
    }

    @Test
    public void testImplicitResetByUpdateAfterDigest() {
        MessageDigest messageDigest = new XorMessageDigest(4);
        messageDigest.update(new byte[] { (byte) 247, (byte) 248, 25, 35, 45, (byte) 215 });
        byte[] digest = messageDigest.digest(); // Implicit reset

        messageDigest.update(new byte[] { 5, 15, 25, 35, 45, 55, 65 }, 1, 5);
        digest = messageDigest.digest();
        assertArrayEquals(new byte[] { 56, 25, 35, 45 }, digest);
    }
}
