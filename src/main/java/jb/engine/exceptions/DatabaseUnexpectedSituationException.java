package jb.engine.exceptions;

/**
 * Thrown when unexpected things happen
 */
public class DatabaseUnexpectedSituationException extends RuntimeException {

    public DatabaseUnexpectedSituationException() {
    }

    public DatabaseUnexpectedSituationException(String message) {
        super(message);
    }

    public DatabaseUnexpectedSituationException(String message, Throwable cause) {
        super(message, cause);
    }
}
