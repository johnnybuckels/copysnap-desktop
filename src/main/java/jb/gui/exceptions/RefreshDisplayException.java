package jb.gui.exceptions;

/**
 * Exception thrown when refreshing copy snaps directory display fails.
 */
public class RefreshDisplayException extends RuntimeException {
    public RefreshDisplayException() {
    }

    public RefreshDisplayException(String message) {
        super(message);
    }

    public RefreshDisplayException(String message, Throwable cause) {
        super(message, cause);
    }
}
