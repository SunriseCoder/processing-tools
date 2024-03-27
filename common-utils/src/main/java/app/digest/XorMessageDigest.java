package app.digest;

import java.security.MessageDigest;

/**
 * Register the algorithm using the following code:
 * Security.addProvider(new XorProvider());
 */
public class XorMessageDigest extends MessageDigest {
    private byte[] digest;
    private int head;

    public XorMessageDigest() {
        this(32);
    }

    public XorMessageDigest(int sizeInBytes) {
        super("XOR-" + sizeInBytes);
        digest = new byte[sizeInBytes];
        reset();
    }

    @Override
    protected void engineUpdate(byte input) {
        digest[head] ^= input;
        head++;
        if (head >= digest.length) {
            head = 0;
        }
    }

    @Override
    protected void engineUpdate(byte[] input, int offset, int len) {
        for (int i = offset; i < offset + len; i++) {
            digest[head] ^= input[i];
            head++;
            if (head >= digest.length) {
                head = 0;
            }
        }
    }

    @Override
    protected byte[] engineDigest() {
        byte[] digestToReturn = digest;
        engineReset();
        return digestToReturn;
    }

    @Override
    protected void engineReset() {
        digest = new byte[digest.length];
        head = 0;
    }
}
