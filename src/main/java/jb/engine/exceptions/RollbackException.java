package jb.engine.exceptions;

public class RollbackException extends RuntimeException {
    public RollbackException() {
    }

    public RollbackException(String message) {
        super(message);
    }

    public RollbackException(String message, Throwable cause) {
        super(message, cause);
    }
}
