package app.digest;

import java.security.Provider;
import java.security.Security;

/**
 * Register the algorithm by invoking the following method:
 * XorProvider.register();
 */
public class XorProvider extends Provider {
    private static final long serialVersionUID = 8796581585317590264L;

    public static void register() {
        Security.addProvider(new XorProvider());
    }

    public XorProvider() {
        super("XOR", 1.0, "XOR Security Provider v1.0");
        put("MessageDigest.XOR", XorMessageDigest.class.getName());
    }
}
