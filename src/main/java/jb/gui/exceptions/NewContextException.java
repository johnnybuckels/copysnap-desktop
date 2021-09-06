package jb.gui.exceptions;

public class NewContextException extends RuntimeException {

    public NewContextException() {
    }

    public NewContextException(String message) {
        super(message);
    }

    public NewContextException(String message, Throwable cause) {
        super(message, cause);
    }

}
