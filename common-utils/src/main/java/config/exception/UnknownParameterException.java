package config.exception;

public class UnknownParameterException extends RuntimeException {
    private static final long serialVersionUID = 2714434769719835379L;

    public UnknownParameterException(String message) {
        super(message);
    }
}
