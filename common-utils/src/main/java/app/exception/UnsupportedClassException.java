package app.exception;

public class UnsupportedClassException extends RuntimeException {
    private static final long serialVersionUID = -1815347863569119484L;

    public UnsupportedClassException(String message) {
        super(message);
    }

    public UnsupportedClassException(String message, Throwable t) {
        super(message, t);
    }
}
