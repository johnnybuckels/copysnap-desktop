package jb.engine.exceptions;

/**
 * Thrown when validating of loaded Context info fails.
 */
public class IntegrityException extends RuntimeException {
    public IntegrityException() {
    }

    public IntegrityException(String message) {
        super(message);
    }

    public IntegrityException(String message, Throwable cause) {
        super(message, cause);
    }

    public IntegrityException(String formatString, Object... args) {
        super(String.format(formatString, args));
    }
}
