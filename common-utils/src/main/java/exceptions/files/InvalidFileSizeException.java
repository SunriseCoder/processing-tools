package exceptions.files;

import java.io.IOException;

public class InvalidFileSizeException extends IOException {
    private static final long serialVersionUID = -5713158202913994135L;

    public InvalidFileSizeException(String message) {
        super(message);
    }
}
