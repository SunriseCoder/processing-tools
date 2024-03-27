package config;

import java.security.InvalidParameterException;

public class CommandLineConfig extends BaseConfig {

    public CommandLineConfig(String[] args) {
        parseArgs(args);
    }

    private void parseArgs(String[] args) {
        for (int i = 0; i < args.length; i++) {
            String pair = args[i];
            if (!pair.startsWith("--")) {
                throw new InvalidParameterException("All Keys must starts with --, argument #" + i + " is: " + pair);
            }

            pair = pair.substring(2); // Removing --
            if (pair.contains("=")) {
                int separatorIndex = pair.indexOf("=");
                String key = pair.substring(0, separatorIndex);
                String value = pair.substring(separatorIndex + 1);
                setValue(key, value);
            } else {
                setValue(pair, null);
            }
        }
    }
}
