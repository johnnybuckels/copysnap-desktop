package jb.engine.exceptions;

/**
 * This exception occurs when serializing or deserializing of an object fails.
 */
public class SerDeException extends RuntimeException {
    public SerDeException() {
    }

    public SerDeException(String message) {
        super(message);
    }

    public SerDeException(String message, Throwable cause) {
        super(message, cause);
    }
}
