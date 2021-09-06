package jb.gui.exceptions;

public class InvalidPathSelectionException extends Exception {
    public InvalidPathSelectionException() {
    }

    public InvalidPathSelectionException(String message) {
        super(message);
    }

    public InvalidPathSelectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
