package jb.engine.exceptions;

/**
 * Exception occurring when creating a snapshot fails.
 */
public class SnapshotException extends RuntimeException{
    public SnapshotException() {
    }

    public SnapshotException(String message, Throwable cause) {
        super(message, cause);
    }

    public SnapshotException(String message) {
        super(message);
    }
}
