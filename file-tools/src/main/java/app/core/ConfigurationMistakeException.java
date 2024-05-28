package app.core;

public class ConfigurationMistakeException extends RuntimeException {
    private static final long serialVersionUID = -5692731722340602484L;

    public ConfigurationMistakeException(String message) {
        super(message);
    }
}
