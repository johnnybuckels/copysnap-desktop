package jb.gui.exceptions;

/**
 * General exception thrown by the CopySnap UI
 */
public class CopySnapException extends RuntimeException {

    public CopySnapException() {
    }

    public CopySnapException(String message) {
        super(message);
    }

    public CopySnapException(String message, Throwable cause) {
        super(message, cause);
    }
}
