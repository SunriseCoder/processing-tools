package config.exception;

public class ParameterNotSetException extends RuntimeException {
    private static final long serialVersionUID = 764972465503320976L;

    public ParameterNotSetException(String message) {
        super(message);
    }
}
