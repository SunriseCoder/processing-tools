package digest;

import java.security.Provider;

public class XorProvider extends Provider {
    private static final long serialVersionUID = 8796581585317590264L;

    public XorProvider() {
        super("XOR", 1.0, "XOR Security Provider v1.0");
        put("MessageDigest.XOR", XorMessageDigest.class.getName());
    }
}
