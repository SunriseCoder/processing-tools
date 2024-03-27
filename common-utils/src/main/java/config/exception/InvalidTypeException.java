package config.exception;

public class InvalidTypeException extends RuntimeException {
    private static final long serialVersionUID = 347577138848490174L;

    public InvalidTypeException(String message) {
        super(message);
    }
}
