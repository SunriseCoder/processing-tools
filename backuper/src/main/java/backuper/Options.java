package backuper;

import java.util.HashMap;
import java.util.Map;

public class Options {
    private Map<Names, String> options;

    public Options() {
        options = new HashMap<>();
    }

    public boolean isSet(Names name) {
        boolean isSet = options.containsKey(name);
        return isSet;
    }

    public static enum Names {
        FOLLOW_SYMLINKS("--follow-symlinks");

        private String optionName;

        Names(String optionName) {
            this.optionName = optionName;
        }

        public String getOptionName() {
            return optionName;
        }
    }
}
