package jb.engine.exceptions;

/**
 * Occurs when an identical hash occurs on two different objects.
 */
public class HashDuplicateException extends RuntimeException {
    public HashDuplicateException() {
    }

    public HashDuplicateException(String message) {
        super(message);
    }

    public HashDuplicateException(String message, Object... args) {
        super(String.format(message, args));
    }

    public HashDuplicateException(String message, Throwable cause) {
        super(message, cause);
    }
}
