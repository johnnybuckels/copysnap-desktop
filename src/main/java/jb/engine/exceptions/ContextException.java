package jb.engine.exceptions;

/**
 * Exception occurring when actions fail involving a context.
 */
public class ContextException extends RuntimeException{
    public ContextException() {
    }

    public ContextException(String message, Throwable cause) {
        super(message, cause);
    }

    public ContextException(String message) {
        super(message);
    }
}
