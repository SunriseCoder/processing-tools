package config;

// TODO Remove this class and all Config, Parameter and Exception classes if not used anywhere
public class Config {
    private static Config INSTANCE = null;

    private BaseConfig config;

    private Config() {
        // Singleton
    }

    public static Config getInstance() {
        if (INSTANCE == null) {
            synchronized (Config.class) {
                if (INSTANCE == null) {
                    INSTANCE = new Config();
                }
            }
        }
        return INSTANCE;
    }

    public void init(BaseConfig config) {
        this.config = config;
    }

    public boolean hasValue(Parameter parameter) {
        return config.hasValue(parameter);
    }

    public String getStringValue(Parameter parameter) {
        return config.getStringValue(parameter);
    }
}
