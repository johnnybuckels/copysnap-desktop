package jb.engine.exceptions;

/**
 * Thrown when an {@link jb.engine.core.ObjectCreator} fails to create an object.
 */
public class ObjectCreatorException extends Exception {

    public ObjectCreatorException() {
    }

    public ObjectCreatorException(String message) {
        super(message);
    }

    public ObjectCreatorException(String message, Throwable cause) {
        super(message, cause);
    }
}
