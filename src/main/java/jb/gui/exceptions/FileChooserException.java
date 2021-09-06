package jb.gui.exceptions;

public class FileChooserException extends RuntimeException {

    public FileChooserException() {
    }

    public FileChooserException(String message) {
        super(message);
    }

    public FileChooserException(String message, Throwable cause) {
        super(message, cause);
    }

}
