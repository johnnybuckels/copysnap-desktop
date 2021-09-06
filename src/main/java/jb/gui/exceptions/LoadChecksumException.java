package jb.gui.exceptions;

public class LoadChecksumException extends RuntimeException {
    public LoadChecksumException() {
    }

    public LoadChecksumException(String message) {
        super(message);
    }

    public LoadChecksumException(String message, Throwable cause) {
        super(message, cause);
    }
}
